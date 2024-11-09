package notreallyagroup.backend.users;


import notreallyagroup.backend.Constants;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

@Component
public class ElasticsearchRepository {


    public ElasticsearchRepository() throws IOException {
        try {
            requestBlocking("GET", "_cat/indices?v", "");
        } catch (Exception e) {
            System.err.println("Error constructing ElasticsearchRepository: " + e);
            throw e;
        }
    }

    public String requestBlocking(String method, String url) throws IOException {
        return requestBlocking(method, url, "");
    }

    public String requestBlocking(String method, String url, String body) throws IOException {
        final String URL_SPLITTER = "/";

        var connection = (HttpURLConnection) (new URL(Constants.ElasticSearch.URL + URL_SPLITTER + url).openConnection());
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        String auth = "Basic " + java.util.Base64.getEncoder().encodeToString((Constants.ElasticSearch.USERNAME + ":" + Constants.ElasticSearch.PASSWORD).getBytes());
        connection.setRequestProperty("Authorization", auth);
        if (body != null && !body.isEmpty() && method.equals("POST")) {
            connection.setDoOutput(true);
            connection.getOutputStream().write(body.getBytes());
        }

        return getResponse(connection);
    }

    private static String getResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            for (String line; (line = br.readLine()) != null; ) {
                response.append(line);
                response.append("\n");
            }
            if (!(200 <= responseCode && responseCode < 300)) {
                throw new IOException("Failed : HTTP error code : " + responseCode + " : " + response);
            }
        }
        return response.toString();
    }


}

