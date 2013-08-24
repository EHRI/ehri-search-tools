package eu.ehri.project.indexer;

import com.google.common.base.Joiner;
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
import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Indexer {

    /**
     * Default service end points.
     * <p/>
     * TODO: Store these in a properties file?
     */
    public static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr/portal";
    public static final String DEFAULT_EHRI_URL = "http://localhost:7474/ehri";

    // JSON mapper and writer
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    // Reusable Jersey client
    private static final Client client = Client.create();

    /**
     * Fields.
     */
    private final String solrUrl;
    private final String ehriUrl;

    /**
     * Builder for an Indexer. More options to come.
     */
    public static class Builder {
        private String solrUrl = DEFAULT_SOLR_URL;
        private String ehriUrl = DEFAULT_EHRI_URL;

        public String getSolrUrl() {
            return solrUrl;
        }

        public void setSolrUrl(String solrUrl) {
            this.solrUrl = solrUrl;
        }

        public String getEhriUrl() {
            return ehriUrl;
        }

        public void setEhriUrl(String ehriUrl) {
            this.ehriUrl = ehriUrl;
        }

        public Indexer build() {
            return new Indexer(this);
        }
    }

    private Indexer(Builder builder) {
        this.solrUrl = builder.getSolrUrl();
        this.ehriUrl = builder.getEhriUrl();
    }

    /**
     * A class for holding interesting stats.
     */
    private static class Stats {
        private long startTime = System.nanoTime();

        public int itemCount = 0;

        public void printReport() {
            long endTime = System.nanoTime();
            double duration = ((double) (endTime - startTime)) / 1000000000.0;

            System.out.println("Indexing completed in " + duration);
            System.out.println("Items indexed: " + itemCount);
            System.out.println("Items per second: " + (itemCount / duration));
        }
    }

    /**
     * Commit the Solr updates.
     */
    public void commit() {
        WebResource commitResource = client.resource(
                UriBuilder.fromPath(solrUrl).segment("update").build());
        ClientResponse response = commitResource
                .queryParam("commit", "true")
                .queryParam("optimize", "true")
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
    private void doIndex(InputStream ios, boolean doCommit) {
        WebResource resource = client.resource(
                UriBuilder.fromPath(solrUrl).segment("update").build());
        ClientResponse response = resource
                .queryParam("commit", String.valueOf(doCommit))
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
     * @param in    The type of item to reindex
     * @param out   The output stream for converted JSON
     * @param stats A Stats object for storing metrics
     * @throws IOException
     */
    private void convertStream(InputStream in, OutputStream out, Stats stats) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(in));

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
            br.close();
        }
    }

    /**
     * Convert a individual item from the stream and write the results.
     *
     * @param node      A JSON node representing a single item
     * @param generator The JSON generator with which to write
     *                  the converted data
     * @throws IOException
     */
    private void convertItem(JsonNode node, JsonGenerator generator) throws IOException {
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
     * Index a specific entity jobId.
     *
     * @param stream An input stream consisting of a JSON list
     * @param jobId  The jobId of item to index
     * @param stats  A statistics object
     * @throws IOException
     */
    private void indexStream(InputStream stream, String jobId, Stats stats) throws IOException {

        File tempFile = File.createTempFile(jobId, "json");
        try {
            // Write the converted JSON to the tempFile file...
            OutputStream out = new FileOutputStream(tempFile);

            try {
                convertStream(stream, out, stats);
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
    }

    /**
     * Print converted data to StdOut.
     *
     * @param ids An array of item id strings
     * @throws IOException
     */
    public void printIds(String[] ids) throws IOException {
        OutputStream pw = new PrintStream(System.out);
        Stats stats = new Stats();
        try {
            ClientResponse response = getJsonResponseForIds(ids);
            try {
                checkResponse(response);
                convertStream(response.getEntityInputStream(), pw, stats);
            } finally {
                response.close();
            }
        } finally {
            pw.close();
        }
    }

    /**
     * Print converted data to StdOut.
     *
     * @param types An array of type strings
     * @throws IOException
     */
    public void printTypes(String[] types) throws IOException {
        OutputStream pw = new PrintStream(System.out);
        Stats stats = new Stats();
        try {
            for (String type : types) {
                ClientResponse response = getJsonResponseForType(type);
                try {
                    checkResponse(response);
                    convertStream(response.getEntityInputStream(), pw, stats);
                } finally {
                    response.close();
                }
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
    public void indexTypes(String... types) throws IOException {
        System.out.println("Indexing: " + Joiner.on(", ").join(types));
        Stats stats = new Stats();

        for (String type : types) {
            ClientResponse response = getJsonResponseForType(type);
            try {
                checkResponse(response);
                indexStream(response.getEntityInputStream(), type, stats);
            } finally {
                response.close();
            }
        }
        commit();
        stats.printReport();
    }

    /**
     * Index a set of content types.
     *
     * @param ids An array of item id strings
     * @throws IOException
     */
    public void indexIds(String... ids) throws IOException {
        String jobId = Joiner.on("-").join(ids);
        System.out.println("Indexing: " + Joiner.on(", ").join(ids));
        Stats stats = new Stats();

        ClientResponse response = getJsonResponseForIds(ids);
        try {
            checkResponse(response);
            indexStream(response.getEntityInputStream(), jobId, stats);
        } finally {
            response.close();
        }

        commit();
        stats.printReport();
    }

    /**
     * Check a REST API response is good.
     *
     * @param response  The response object to check
     */
    private void checkResponse(ClientResponse response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new RuntimeException("Unexpected response from EHRI REST: " + response.getStatus());
        }
    }

    /**
     * Get API response for a given entity type.
     *
     * @param type  A REST type string
     * @return      The response object
     */
    private ClientResponse getJsonResponseForType(String type) {
        WebResource resource = client.resource(
                UriBuilder.fromPath(ehriUrl).segment(type).segment("list").build());

        return resource
                .queryParam("limit", "100000") // Ugly, but there's a default limit
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    }

    /**
     * Get type-agnostic API response for a set of item ids.
     *
     * @param ids   A set of item ids
     * @return      The response object
     */
    private ClientResponse getJsonResponseForIds(String[] ids) {
        WebResource resource = client.resource(
                UriBuilder.fromPath(ehriUrl).segment("entities").build());
        for (String id : ids) {
            resource = resource.queryParam("id", id);
        }

        return resource
                .queryParam("limit", "100000") // Ugly, but there's a default limit
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    }

    public static void main(String[] args) throws IOException, ParseException {

        Options options = new Options();
        options.addOption("p", "print", false,
                "Print converted JSON instead of indexing");
        options.addOption("i", "items", false,
                "Index items with the given ids, instead of types");
        options.addOption("s", "solr", true,
                "Base URL for Solr service (minus the action segment)");
        options.addOption("e", "ehri", true,
                "Base URL for EHRI REST service");
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        Indexer.Builder builder = new Indexer.Builder();
        if (cmd.hasOption("solr")) {
            builder.setSolrUrl(cmd.getOptionValue("solr"));
        }
        if (cmd.hasOption("ehri")) {
            builder.setEhriUrl(cmd.getOptionValue("ehri"));
        }
        Indexer indexer = builder.build();

        if (cmd.hasOption("print")) {
            if (cmd.hasOption("items")) {
                indexer.printIds(cmd.getArgs());
            } else {
                indexer.printTypes(cmd.getArgs());
            }
        } else {
            if (cmd.hasOption("items")) {
                indexer.indexIds(cmd.getArgs());
            } else {
                indexer.indexTypes(cmd.getArgs());
            }
        }
    }
}
