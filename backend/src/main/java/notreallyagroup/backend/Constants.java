package notreallyagroup.backend;

public class Constants {
    public static final String MONGO_DB_CONNECTION = "mongodb://localhost:27017";
    public static final String MONGO_DB_DATABASE_NAME = "used_item_resale";
    //    public final String MongoDBConnection= "mongodb+srv://admin:zqpmxw@cluster0.jvrp7.mongodb.net/";

    public static final String[] CURS_ALLOWED_FRONTEND = {"http://localhost:3000"};

    public static class ElasticSearch {
        public static final String URL = "http://192.168.68.133:9200";
        public static final String USERNAME = "myesuser";
        public static final String PASSWORD = "zqpmxw";

    }

    public static final int TOKEN_LIFE_SPAN_IN_MILLISECONDS = 999 * 24 * 60 * 60 * 1000;
}
