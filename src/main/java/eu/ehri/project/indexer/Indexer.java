package eu.ehri.project.indexer;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Indexer {

   private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {

        String type = "documentaryUnit";
        if (args.length > 0)
            type = args[0];

        long startTime = System.nanoTime();

        Client client = Client.create();

        //WebResource resource = client.resource("http://localhost:7474/ehri/" + type + "/list?limit=100000");
        WebResource resource = client.resource("http://localhost:7474/ehri/" + type + "/list?limit=100000");
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntityInputStream()));

        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(br);
        // advance stream to START_ARRAY first:
        jp.nextToken();
        int items = 0;
        // and then each time, advance to opening START_OBJECT
        while (jp.nextToken() == JsonToken.START_OBJECT) {
            SolrDocument doc = mapper.readValue(jp, SolrDocument.class);
            System.out.println(doc.getType() + " -> " + doc.getId());
            items++;
        }
        jp.close();
        //response.close();

        long endTime = System.nanoTime();
        double duration = ((double)(endTime - startTime)) / 1000000000.0;

        System.out.println("Indexing completed in " + duration);
        System.out.println("Items indexed: " + items);
        System.out.println("Items per second: " + (items / duration));
    }
}
