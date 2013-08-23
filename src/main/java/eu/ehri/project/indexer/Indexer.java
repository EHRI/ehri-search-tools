package eu.ehri.project.indexer;

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Indexer {

    private static ObjectMapper mapper = new ObjectMapper();
    private static ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    private static Client client = Client.create();

    public static final int BATCH_SIZE = 2500;

    public static void commit() {
        WebResource commitResource = client.resource
                ("http://localhost:8983/solr/portal/update?commit=true&optimize=true");
        ClientResponse response = commitResource
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class);
        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
            throw new RuntimeException("Error with Solr commit: " + response.getEntity(String.class));
        }
    }

    public static void doBatch(WebResource resource, List<Map<String,Object>> data) {
        try {
            ClientResponse response = resource
                    .type(MediaType.APPLICATION_JSON)
                    .entity(writer.writeValueAsString(data))
                    .post(ClientResponse.class);
            if (Response.Status.OK.getStatusCode() != response.getStatus()) {
                throw new RuntimeException("Error with Solr upload: " + response.getEntity(String.class));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void printType(String type) throws IOException{

        PrintWriter pw = new PrintWriter(System.out, true);

        WebResource fetchResource = client.resource("http://localhost:7474/ehri/" + type + "/list?limit=100000");

        ClientResponse response = fetchResource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntityInputStream()));

        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(br);

        jp.nextToken();
        int items = 0;

        JsonGenerator generator = f.createJsonGenerator(pw);
        try {
            generator.writeStartArray();
            generator.writeRaw('\n');

            while (jp.nextToken() == JsonToken.START_OBJECT) {

                JsonNode node = mapper.readValue(jp, JsonNode.class);

                Iterator<JsonNode> elements = node.path("relationships").path("describes").getElements();
                List<JsonNode> descriptions = Lists.newArrayList(elements);

                if (descriptions.size() > 0) {
                    for (JsonNode description : descriptions) {
                        writer.writeValue(generator, JsonConverter.getDescribedData(description, node));
                        generator.writeRaw('\n');
                    }
                } else {
                    writer.writeValue(generator, JsonConverter.getData(node));
                    generator.writeRaw('\n');
                }
            }
            generator.writeEndArray();
        } finally {
            generator.flush();
            generator.close();
        }
        pw.flush();
        //System.out.println("DONE");
        pw.close();
        jp.close();
        response.close();
    }



    public static void indexType(String type) throws IOException{

        System.out.println("Indexing: " + type);

        long startTime = System.nanoTime();

        WebResource fetchResource = client.resource("http://localhost:7474/ehri/" + type + "/list?limit=100000");
        WebResource postResource = client.resource("http://localhost:8983/solr/portal/update?commit=false&wt=json");

        ClientResponse response = fetchResource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntityInputStream()));

        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(br);
        // advance stream to START_ARRAY first:
        jp.nextToken();
        int items = 0;

        List<Map<String,Object>> buffer = Lists.newArrayList();

        // and then each time, advance to opening START_OBJECT
        while (jp.nextToken() == JsonToken.START_OBJECT) {
            //RestItem doc = mapper.readValue(jp, RestItem.class);
            JsonNode node = mapper.readValue(jp, JsonNode.class);

            Iterator<JsonNode> elements = node.path("relationships").path("describes").getElements();
            List<JsonNode> descriptions = Lists.newArrayList(elements);

            if (descriptions.size() > 0) {
                for (JsonNode description : descriptions) {
                    buffer.add(JsonConverter.getDescribedData(description, node));
                }
            } else {
                buffer.add(JsonConverter.getData(node));
            }

            items++;

            if (buffer.size() >= BATCH_SIZE) {
                System.out.println("Writing batch...");
                doBatch(postResource, buffer);
                buffer.clear();
            }
        }
        jp.close();
        response.close();

        if (buffer.size() > 0) {
            System.out.println("Writing final batch...");
            doBatch(postResource, buffer);
        }

        System.out.println("Committing");
        commit();


        long endTime = System.nanoTime();
        double duration = ((double)(endTime - startTime)) / 1000000000.0;

        System.out.println("Indexing completed in " + duration);
        System.out.println("Items indexed: " + items);
        System.out.println("Items per second: " + (items / duration));
    }

    public static void main(String[] args) throws IOException {

        for (String type :  args) {
            indexType(type);
        }
    }
}
