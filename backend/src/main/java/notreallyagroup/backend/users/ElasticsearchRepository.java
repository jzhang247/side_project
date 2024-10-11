package notreallyagroup.backend.users;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class ElasticsearchRepository {
    public ElasticsearchClient client;

    public ElasticsearchRepository() {

        String serverUrl = "http://192.168.68.135:9200";
        String username = "myesuser";
        String password = "zqpmxw";
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        RestClient restClient = RestClient.builder(new HttpHost(serverUrl))
                .setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", basicAuth),})
                .build();

        // Create the transport with Jackson Mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // Create the API client
        client = new ElasticsearchClient(transport);
    }


}
