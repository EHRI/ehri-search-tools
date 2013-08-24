package eu.ehri.project.indexer;

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.cli.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Indexer {

    // TODO: Move to external configuration
    public static final String SOLR_URL = "http://localhost:8983/solr/portal";
    public static final String EHRI_URL = "http://localhost:7474/ehri";

    // JSON mapper and writer
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    // Reusable Jersey client
    private static final Client client = Client.create();

    /**
     * A class for holding interesting stats.
     */
    private static class Stats {
        public int itemCount = 0;
    }

    /**
     * Commit the Solr updates.
     */
    public static void commit() {
        WebResource commitResource = client.resource(SOLR_URL + "/update?commit=true&optimize=true");
        ClientResponse response = commitResource
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class);
        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
            throw new RuntimeException("Error with Solr commit: " + response.getEntity(String.class));
        }
    }

    /**
     * Index some JSON data.
     *
     * @param ios      The input stream containing update JSON
     * @param doCommit Whether or not to commit the update
     */
    public static void doIndex(InputStream ios, boolean doCommit) {
        WebResource resource = client.resource(SOLR_URL + "/update?commit=" + doCommit);
        ClientResponse response = resource
                .type(MediaType.APPLICATION_JSON)
                .entity(ios)
                .post(ClientResponse.class);
        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
            throw new RuntimeException("Error with Solr upload: " + response.getEntity(String.class));
        }
    }

    /**
     * Write converted JSON data to an output stream.
     *
     * @param type  The type of item to reindex
     * @param out   The output stream for converted JSON
     * @param stats A Stats object for storing metrics
     * @throws IOException
     */
    public static void convertType(String type, OutputStream out, Stats stats) throws IOException {

        WebResource fetchResource = client.resource(EHRI_URL + "/" + type + "/list?limit=100000");

        ClientResponse response = fetchResource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntityInputStream()));

        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(br);

        try {
            jp.nextToken();

            JsonGenerator generator = f.createJsonGenerator(out);
            try {
                generator.writeStartArray();
                generator.writeRaw('\n');
                while (jp.nextToken() == JsonToken.START_OBJECT) {
                    JsonNode node = mapper.readValue(jp, JsonNode.class);
                    convertItem(node, generator);
                    stats.itemCount++;
                }
                generator.writeEndArray();
                generator.writeRaw('\n');
            } finally {
                generator.flush();
                generator.close();
            }
        } finally {
            jp.close();
        }
        response.close();
    }

    /**
     * Convert a individual item from the stream and write the results.
     *
     * @param node      A JSON node representing a single item
     * @param generator The JSON generator with which to write
     *                  the converted data
     * @throws IOException
     */
    private static void convertItem(JsonNode node, JsonGenerator generator) throws IOException {
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


    /**
     * Index a specific entity type.
     *
     * @param type The type of item to index
     * @throws IOException
     */
    public static void indexType(String type) throws IOException {

        System.out.println("Indexing: " + type);
        long startTime = System.nanoTime();
        Stats stats = new Stats();

        File tempFile = File.createTempFile(type, "json");
        try {
            // Write the converted JSON to the tempFile file...
            OutputStream out = new FileOutputStream(tempFile);
            try {
                convertType(type, out, stats);
            } finally {
                out.close();
            }

            // Load the tempFile file as an input stream and index it...
            InputStream ios = new FileInputStream(tempFile);
            try {
                doIndex(ios, false);
            } finally {
                ios.close();
            }
        } finally {
            tempFile.delete();
        }


        long endTime = System.nanoTime();
        double duration = ((double) (endTime - startTime)) / 1000000000.0;

        System.out.println("Indexing completed in " + duration);
        System.out.println("Items indexed: " + stats.itemCount);
        System.out.println("Items per second: " + (stats.itemCount / duration));
    }

    /**
     * Print converted data to StdOut.
     *
     * @param types An array of type strings
     * @throws IOException
     */
    public static void print(String[] types) throws IOException {
        OutputStream pw = new PrintStream(System.out);
        try {
            for (String type : types) {
                convertType(type, pw, new Stats());
            }
        } finally {
            pw.close();
        }
    }

    /**
     * Index a set of content types.
     *
     * @param types An array of type strings
     * @throws IOException
     */
    public static void index(String[] types) throws IOException {
        for (String type : types) {
            indexType(type);
        }
        commit();
    }

    public static void main(String[] args) throws IOException, ParseException {

        Options options = new Options();
        options.addOption("p", "print", false,
                "Print converted JSON instead of indexing");
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse( options, args);

        if (cmd.hasOption("print")) {
            print(cmd.getArgs());
        } else {
            index(cmd.getArgs());
        }
    }
}
