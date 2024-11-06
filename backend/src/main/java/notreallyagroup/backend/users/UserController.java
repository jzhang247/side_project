package notreallyagroup.backend.users;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    public UserController(CategoryManager categoryManager, ElasticsearchRepository elasticsearchRepository) {
        this.categoryManager = categoryManager;
        this.elasticsearchRepository = elasticsearchRepository;


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


    @GetMapping("list_merchandise_categories")
    protected ResponseEntity<String> listMerchandiseCategories() {
        var ret = new JsonObject();
        for (var kv : categoryManager.categories.entrySet()) {
            var cat = kv.getValue();
            var jsonCat = new JsonObject();
            for (var i : cat.attributes.entrySet()) {
                jsonCat.addProperty(i.getKey(), i.getValue().type.toString());
            }
            ret.add(kv.getKey(), jsonCat);
        }
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
    protected ResponseEntity<String> searchMerchandise(@RequestParam Map<String, String> filters) {
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

}
