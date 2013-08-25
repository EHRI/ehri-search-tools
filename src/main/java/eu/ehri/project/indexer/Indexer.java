package eu.ehri.project.indexer;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.cli.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Pull data from the EHRI REST API and index it in Solr.
 *         <p/>
 *         NB: Eventually this class should look more like
 *         a producer -> transformer -> consumer thing.
 */
public class Indexer {

    /**
     * Default service end points.
     * <p/>
     * TODO: Store these in a properties file?
     */
    public static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr/portal";
    public static final String DEFAULT_EHRI_URL = "http://localhost:7474/ehri";

    // Reusable Jersey client
    private static final Client client = Client.create();

    private final String ehriUrl;
    private final JsonConverter converter = new JsonConverter();
    private final SolrIndexer solrIndexer;
    private final WriteOperation writeOperation;

    /**
     * Process a stream of data representing a list of EHRI items.
     */
    private interface WriteOperation {
        /**
         * Do something with a stream of input data.
         *
         * @param stream
         * @param stats
         * @param tag
         * @throws IOException
         */
        public void processStream(InputStream stream, Stats stats, String tag) throws IOException;

        /**
         * Actions to run after the stream or streams have been fully
         * processed.
         *
         * @param stats
         */
        public void finish(Stats stats);
    }

    /**
     * Operation which converts the item stream, saves the results to a
     * temporary file
     */
    public class IndexOperation implements WriteOperation {

        @Override
        public void processStream(InputStream stream, Stats stats, String tag) throws IOException {
            File tempFile = File.createTempFile(tag, "json");
            try {
                // Write the converted JSON to the tempFile file...
                OutputStream out = new FileOutputStream(tempFile);

                try {
                    converter.convertStream(stream, out, stats);
                } finally {
                    out.close();
                }

                // Load the tempFile file as an input stream and index it...
                InputStream ios = new FileInputStream(tempFile);
                try {
                    solrIndexer.update(ios, false);
                } finally {
                    ios.close();
                }
            } finally {
                tempFile.delete();
            }
        }

        @Override
        public void finish(Stats stats) {
            solrIndexer.commit();
            stats.printReport();
        }
    }

    /**
     * Operation which just prints the converted stream to StdOut.
     */
    public class PrintOperation implements WriteOperation {

        @Override
        public void processStream(InputStream stream, Stats stats, String tag) throws IOException {
            OutputStream pw = new PrintStream(System.out);
            try {
                converter.convertStream(stream, pw, stats);
            } finally {
                pw.close();
            }
        }

        @Override
        public void finish(Stats stats) {
            // No-op
        }
    }

    /**
     * Builder for an Indexer. More options to come.
     */
    public static class Builder {
        private String solrUrl = DEFAULT_SOLR_URL;
        private String ehriUrl = DEFAULT_EHRI_URL;
        private boolean print = false;

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

        public boolean printOnly() {
            return print;
        }

        public void setPrintOnly() {
            print = true;
        }

        public Indexer build() {
            return new Indexer(this);
        }
    }

    private Indexer(Builder builder) {
        this.solrIndexer = new SolrIndexer(builder.getSolrUrl());
        this.ehriUrl = builder.getEhriUrl();
        this.writeOperation = builder.printOnly()
                ? new PrintOperation()
                : new IndexOperation();
    }

    /**
     * A class for holding interesting stats.
     */
    public static class Stats {
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
     * Index a set of content types.
     *
     * @param types An array of type strings
     * @throws IOException
     */
    public void processTypes(String... types) throws IOException {
        System.out.println("Indexing: " + Joiner.on(", ").join(types));
        Stats stats = new Stats();

        for (String type : types) {
            ClientResponse response = getJsonResponseForType(type);
            try {
                checkResponse(response);
                writeOperation.processStream(response.getEntityInputStream(), stats, type);
            } finally {
                response.close();
            }
        }
        writeOperation.finish(stats);
    }

    /**
     * Index a set of content types.
     *
     * @param ids An array of item id strings
     * @throws IOException
     */
    public void processIds(String... ids) throws IOException {
        String jobId = Joiner.on("-").join(ids);
        System.out.println("Indexing: " + Joiner.on(", ").join(ids));
        Stats stats = new Stats();

        ClientResponse response = getJsonResponseForIds(ids);
        try {
            checkResponse(response);
            writeOperation.processStream(response.getEntityInputStream(), stats, jobId);
        } finally {
            response.close();
        }

        writeOperation.finish(stats);
    }

    /**
     * Check a REST API response is good.
     *
     * @param response The response object to check
     */
    private void checkResponse(ClientResponse response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new RuntimeException("Unexpected response from EHRI REST: " + response.getStatus());
        }
    }

    /**
     * Get API response for a given entity type.
     *
     * @param type A REST type string
     * @return The response object
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
     * @param ids A set of item ids
     * @return The response object
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
        if (cmd.hasOption("print")) {
            builder.setPrintOnly();
        }
        Indexer indexer = builder.build();

        if (cmd.hasOption("items")) {
            indexer.processIds(cmd.getArgs());
        } else {
            indexer.processTypes(cmd.getArgs());
        }
    }
}
