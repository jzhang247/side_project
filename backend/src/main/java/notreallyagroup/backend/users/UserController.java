package notreallyagroup.backend.users;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import notreallyagroup.backend.Constants;
import notreallyagroup.backend.mrchd.CategoryManager;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import util.RunnableException;
import util.Utilities;


@RestController
public class UserController {
    public final String KW_COLLECTION_USER = "user_collection";
    public final String KW_COLLECTION_SESSION = "session_collection";
    public final String KW_USERNAME = "username";
    public final String KW_PASSWORD = "password";
    public final String KW_SESSION_ID = "session_id";
    public final String KW_PACK_TOKEN = "token";
    public final String KW_PACK_MERCHANDISE = "merchandise";
    public final String KW_MDB_MERCHANDISES = "merchandises";
    public final String KW_ES_MERCHANDISE_OWNER = KW_USERNAME;
    public final String KW_ES_MERCHANDISE_ID = "merchandise_id";

    CategoryManager categoryManager;
    ElasticsearchRepository elasticsearchRepository;
    UserFileStorageManager userFileStorageManager;

    public UserController(CategoryManager categoryManager, ElasticsearchRepository elasticsearchRepository, UserFileStorageManager userFileStorageManager) {
        this.categoryManager = categoryManager;
        this.elasticsearchRepository = elasticsearchRepository;
        this.userFileStorageManager = userFileStorageManager;

        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            boolean alreadyCreated = database.listCollectionNames().into(new ArrayList<>()).contains(KW_COLLECTION_USER);
            MongoCollection<Document> collection = database.getCollection(KW_COLLECTION_USER);
            if (!alreadyCreated) {
                collection.createIndex(Indexes.ascending(KW_USERNAME), new IndexOptions().unique(true));
            }
        }
    }

    private record RequestBodySignUp(String username, String password) {
    }

    @PostMapping("signup")
    protected ResponseEntity<String> signup(@RequestBody RequestBodySignUp request) {

        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(KW_COLLECTION_USER);


            // Create a document
            Document document = new Document()
                    .append(KW_USERNAME, request.username)
                    .append(KW_PASSWORD, PasswordHasher.hashPassword(request.password))
                    .append(KW_MDB_MERCHANDISES, new Document());

            // Insert the document into the collection
            try {
                var rslt = collection.insertOne(document);
            } catch (MongoWriteException e) {
                return ResponseEntity.status(400).body("User already exists.");
            } catch (Exception e) {
                return ResponseEntity.status(500).body(e.getMessage());
            }

            return ResponseEntity.ok("Thank you for signing up!");
        }
    }


    @PostMapping("login")
    protected ResponseEntity<String> login(@RequestBody RequestBodySignUp request) {
        // TODO: currently the client login to get a session then get a token
        // change to get a high access level token plus a session id which would only give low level token
        // to make high level access requires fresh login.

        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            MongoCollection<Document> users = database.getCollection(KW_COLLECTION_USER);
            MongoCollection<Document> sessions = database.getCollection(KW_COLLECTION_SESSION);
            Document findUser = users.find(new Document(KW_USERNAME, request.username)).first();
            if (findUser == null || !PasswordHasher.checkPassword(request.password, findUser.getString(KW_PASSWORD))) {
                return ResponseEntity.status(400).body("Username or password is incorrect.");
            }

            String sessionId = request.username + ZonedDateTime.now(ZoneOffset.UTC);

            Document document = new Document().append(KW_SESSION_ID, sessionId).append(KW_USERNAME, request.username);

            try {
                var rslt = sessions.insertOne(document);
            } catch (Exception e) {
                return ResponseEntity.status(400).body("Failed to login.");
            }

            return ResponseEntity.ok(sessionId);
        }
    }

    private record RequestBodyGetToken(String username, String sessionId) {
    }

    @PostMapping("get_token")
    protected ResponseEntity<String> getToken(@RequestBody RequestBodyGetToken request) {
        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            MongoCollection<Document> users = database.getCollection(KW_COLLECTION_USER);
            MongoCollection<Document> sessions = database.getCollection(KW_COLLECTION_SESSION);

            Document findUser = users.find(new Document(KW_USERNAME, request.username)).first();
            var findSession = sessions.find(new Document(KW_SESSION_ID, request.sessionId)).first();
            if (findUser == null || findSession == null) {
                return ResponseEntity.status(400).body("Invalid session. Please login.");
            }

            var claims = new HashMap<String, Object>();
            claims.put("level", "low");
            claims.put("username", request.username);
            String token = TokenManager.makeToken(claims);
            return ResponseEntity.ok(token);
        }
    }

    @GetMapping("test_token")
    protected ResponseEntity<String> testToken(@RequestBody String token) {
        var claims = TokenManager.validateToken(token);
        return ResponseEntity.ok(claims != null ? "good" : "bad");
    }

    @PostMapping("refresh_token")
    protected ResponseEntity<String> refreshToken(@RequestBody String token) {
        var claims = TokenManager.validateToken(token);
        if (claims == null) {
            return ResponseEntity.status(400).body("Token invalid or expired.");
        }
        return ResponseEntity.ok(TokenManager.makeToken(claims));
    }

    @PostMapping("create_merchandise")
    protected ResponseEntity<String> createMerchandise(@RequestBody String requestText) {
        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            MongoCollection<Document> users = database.getCollection(KW_COLLECTION_USER);


            return ResponseEntity.status(200).body("");
        }
    }

    @PostMapping("add_merchandise")
    protected ResponseEntity<String> addMerchandise(@RequestBody String requestText) throws IOException {
        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            MongoCollection<Document> users = database.getCollection(KW_COLLECTION_USER);

            JsonParser jsonParser = new JsonParser();
            JsonObject jsonRequest = jsonParser.parse(requestText).getAsJsonObject();

            var claims = TokenManager.validateToken(jsonRequest.get(KW_PACK_TOKEN).getAsString());
            if (claims == null) {
                return ResponseEntity.status(400).body("Token invalid or expired.");
            }
            String username = (String) claims.get(KW_USERNAME);
            var jsonMerchandise = jsonRequest.getAsJsonObject(KW_PACK_MERCHANDISE);
            String newMerchandiseId = ZonedDateTime.now(ZoneOffset.UTC).toString();
            var updateResult = users.updateOne(
                    new Document(KW_USERNAME, username),
                    Updates.set(KW_MDB_MERCHANDISES + "." + newMerchandiseId,
                            Document.parse(jsonMerchandise.toString())));

            jsonMerchandise.addProperty(KW_ES_MERCHANDISE_OWNER, username);
            jsonMerchandise.addProperty(KW_ES_MERCHANDISE_ID, newMerchandiseId);

            elasticsearchRepository.requestBlocking("POST", "myindex/_doc/", jsonMerchandise.toString());


            return ResponseEntity.ok("Added merchandise.");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return ResponseEntity.status(500).body("Failed to add merchandise.");
        }
    }


    @PostMapping("add_merchandise_2")
    protected ResponseEntity<String> addMerchandise2(MultipartHttpServletRequest multipartRequest) throws IOException {
        var requestText = multipartRequest.getParameterMap().get("main")[0];
        var files = multipartRequest.getFileMap();


        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            MongoCollection<Document> users = database.getCollection(KW_COLLECTION_USER);

            JsonParser jsonParser = new JsonParser();
            JsonObject jsonRequest = jsonParser.parse(requestText).getAsJsonObject();

            var claims = TokenManager.validateToken(jsonRequest.get(KW_PACK_TOKEN).getAsString());
            if (claims == null) {
                return ResponseEntity.status(400).body("Token invalid or expired.");
            }
            String username = (String) claims.get(KW_USERNAME);
            var jsonMerchandise = jsonRequest.getAsJsonObject(KW_PACK_MERCHANDISE);
//            String newMerchandiseId = ZonedDateTime.now(ZoneOffset.UTC).toString();
            var currentTime = ZonedDateTime.now(ZoneOffset.UTC);
//            String newMerchandiseId = String.format("%04d%02d%02d%02%02%02",currentTime.)
            String newMerchandiseId = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            var updateResult = users.updateOne(
                    new Document(KW_USERNAME, username),
                    Updates.set(KW_MDB_MERCHANDISES + "." + newMerchandiseId,
                            Document.parse(jsonMerchandise.toString())));

            jsonMerchandise.addProperty(KW_ES_MERCHANDISE_OWNER, username);
            jsonMerchandise.addProperty(KW_ES_MERCHANDISE_ID, newMerchandiseId);

            int iterFilesEntry = 0;
            for (var kv : files.entrySet()) {
//                System.out.println(kv.getKey());
                String storagePath = username + "/" + newMerchandiseId + "-" + iterFilesEntry;
                String extension = Utilities.tryGetFileExtension(kv.getValue().getOriginalFilename());
                if (extension != null) {
                    storagePath = storagePath + "." + extension;
                }
                userFileStorageManager.add(storagePath, kv.getValue().getBytes());
                iterFilesEntry++;
            }

            elasticsearchRepository.requestBlocking("POST", "myindex/_doc/", jsonMerchandise.toString());


            return ResponseEntity.ok("Added merchandise.");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return ResponseEntity.status(500).body("Failed to add merchandise.");
        }
    }


    @GetMapping("list_merchandise_categories")
    protected ResponseEntity<String> listMerchandiseCategories() {
        var gson = new Gson();
        var ret = new JsonObject();
        var retCategories = new JsonArray();


        for (var kv : categoryManager.categories.entrySet()) {
            var cat = kv.getValue();
            var jsCat = new JsonObject();
            jsCat.addProperty("name", cat.name);

            var jsCatAttr = new JsonArray();
            for (var i : cat.attributes.entrySet()) {
                jsCatAttr.add(gson.toJsonTree(i.getValue()));
            }
            jsCat.add("attributes", jsCatAttr);

            retCategories.add(jsCat);
        }
        ret.add("categories", retCategories);
        return ResponseEntity.ok(ret.toString());
    }

    @GetMapping("list_users_merchandise")
    protected ResponseEntity<String> listUsersMerchandise(@RequestParam("username") String username) {

        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            MongoCollection<Document> users = database.getCollection(KW_COLLECTION_USER);
            var user = users.find(new Document(KW_USERNAME, username)).first().toBsonDocument();

            return ResponseEntity.ok(user.getDocument(KW_MDB_MERCHANDISES).toJson());
        }
    }

    @GetMapping("search_merchandise")
    protected ResponseEntity<String> searchMerchandise(@RequestBody String requestBody) {
        try {
            JsonObject jsQuery = new JsonObject();
            jsQuery.add("match_all", new JsonObject());
            JsonObject jsBody = new JsonObject();
            jsBody.add("query", jsQuery);
            elasticsearchRepository.requestBlocking("GET", "myindex", jsBody.toString());
            return ResponseEntity.ok("");
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return ResponseEntity.status(500).body("Failed to search merchandise.");
        }
    }


    @GetMapping("echo")
    protected ResponseEntity<String> echoGet(@RequestParam Map<String, String> params, @RequestBody String body) {
        return ResponseEntity.ok("GET params: " + params + "; body: " + body);
    }

    @PostMapping("echo")
    protected ResponseEntity<String> echoPost(@RequestParam Map<String, String> params, @RequestBody String body) {
        return ResponseEntity.ok("Post params: " + params + "; body: " + body);
    }

    @DeleteMapping("wipe_server")
    protected ResponseEntity<String> wipeServer(@RequestBody String requestText) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonRequest = jsonParser.parse(requestText).getAsJsonObject();
        var token = jsonRequest.get("token").getAsString();
        var claims = TokenManager.validateToken(jsonRequest.get(KW_PACK_TOKEN).getAsString());
        if (claims == null) {
            return ResponseEntity.status(400).body("Token invalid or expired.");
        }
        String username = (String) claims.get(KW_USERNAME);
        if (username.equalsIgnoreCase("alex") || username.toLowerCase().startsWith("admin")) {
            Consumer<RunnableException> runIgnoreException = runnable -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            try {
                runIgnoreException.accept(() -> elasticsearchRepository.requestBlocking("DELETE", "myindex"));
                userFileStorageManager.deleteAll();
                try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
                    MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
                    runIgnoreException.accept(() -> database.getCollection(KW_COLLECTION_USER).drop());
                    runIgnoreException.accept(() -> database.getCollection(KW_COLLECTION_SESSION).drop());
                }
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Failed to delete data.");
            }

            return ResponseEntity.ok("done");
        } else {
            return ResponseEntity.status(400).body("Access denied.");
        }

    }

}
