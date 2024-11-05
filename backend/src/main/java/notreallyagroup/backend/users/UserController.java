package notreallyagroup.backend.users;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    public final String NAME_COLLECTION_USER = "user_collection";
    public final String NAME_COLLECTION_SESSION = "session_collection";
    public final String NAME_USERNAME = "username";
    public final String NAME_PASSWORD = "password";
    public final String NAME_SESSION_ID = "session_id";
    public final String NAME_TOKEN = "token";
    public final String NAME_MERCHANDISE = "merchandise";
    public final String NAME_ATTRIBUTE = "attribute";
    public final String NAME_MERCHANDISES = "merchandises";

    CategoryManager categoryManager;
    ElasticsearchRepository elasticsearchRepository;

    public UserController(CategoryManager categoryManager, ElasticsearchRepository elasticsearchRepository) {
        this.categoryManager = categoryManager;
        this.elasticsearchRepository = elasticsearchRepository;


        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            boolean alreadyCreated = database.listCollectionNames().into(new ArrayList<>()).contains(NAME_COLLECTION_USER);
            MongoCollection<Document> collection = database.getCollection(NAME_COLLECTION_USER);
            if (!alreadyCreated) {
                collection.createIndex(Indexes.ascending(NAME_USERNAME), new IndexOptions().unique(true));
            }
        }
    }

    private record RequestBodySignUp(String username, String password) {
    }

    @PostMapping("signup")
    protected ResponseEntity<String> signup(@RequestBody RequestBodySignUp request) {

        try (MongoClient mongoClient = MongoClients.create(Constants.MONGO_DB_CONNECTION)) {
            MongoDatabase database = mongoClient.getDatabase(Constants.MONGO_DB_DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(NAME_COLLECTION_USER);


            // Create a document
            Document document = new Document()
                    .append(NAME_USERNAME, request.username)
                    .append(NAME_PASSWORD, PasswordHasher.hashPassword(request.password))
                    .append(NAME_MERCHANDISES, new Document());

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
            MongoCollection<Document> users = database.getCollection(NAME_COLLECTION_USER);
            MongoCollection<Document> sessions = database.getCollection(NAME_COLLECTION_SESSION);
            Document findUser = users.find(new Document(NAME_USERNAME, request.username)).first();
            if (findUser == null || !PasswordHasher.checkPassword(request.password, findUser.getString(NAME_PASSWORD))) {
                return ResponseEntity.status(400).body("Username or password is incorrect.");
            }

            String sessionId = request.username + ZonedDateTime.now(ZoneOffset.UTC);

            Document document = new Document().append(NAME_SESSION_ID, sessionId).append(NAME_USERNAME, request.username);

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
            MongoCollection<Document> users = database.getCollection(NAME_COLLECTION_USER);
            MongoCollection<Document> sessions = database.getCollection(NAME_COLLECTION_SESSION);

            Document findUser = users.find(new Document(NAME_USERNAME, request.username)).first();
            var findSession = sessions.find(new Document(NAME_SESSION_ID, request.sessionId)).first();
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
            MongoCollection<Document> users = database.getCollection(NAME_COLLECTION_USER);

            JsonParser jsonParser = new JsonParser();
            JsonObject jsonRequest = jsonParser.parse(requestText).getAsJsonObject();

            var claims = TokenManager.validateToken(jsonRequest.get(NAME_TOKEN).getAsString());
            if (claims == null) {
                return ResponseEntity.status(400).body("Token invalid or expired.");
            }
            String username = (String) claims.get(NAME_USERNAME);
            var jsonMerchandise = jsonRequest.getAsJsonObject(NAME_MERCHANDISE);
            String newMerchandiseId = Utilities.stringToHexString(ZonedDateTime.now(ZoneOffset.UTC).toString());
            var updateResult = users.updateOne(
                    new Document(NAME_USERNAME, username),
                    Updates.set(NAME_MERCHANDISES + "." + newMerchandiseId,
                            Document.parse(jsonMerchandise.toString())));

            String esrbody = jsonMerchandise.toString();
            elasticsearchRepository.requestBlocking("POST","myindex/_doc/",esrbody);



            return ResponseEntity.ok("Added merchandise.");
        }
//        catch(Exception ex){
//            System.out.println(ex.getMessage());
//            return ResponseEntity.status(500).body("Failed to add merchandise.");
//        }
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
            MongoCollection<Document> users = database.getCollection(NAME_COLLECTION_USER);
            var user = users.find(new Document(NAME_USERNAME, username)).first().toBsonDocument();

            return ResponseEntity.ok(user.getDocument(NAME_MERCHANDISES).toJson());
        }
    }


}
