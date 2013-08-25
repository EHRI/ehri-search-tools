package eu.ehri.project.indexer;

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import eu.ehri.project.indexer.impl.*;
import eu.ehri.project.indexer.impl.OutputStreamWriter;
import org.apache.commons.cli.*;
import org.codehaus.jackson.JsonNode;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Pull data from the EHRI REST API and index it in Solr.
 *         <p/>
 *         Majorly overengineered for the fun of it.
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
    private final SolrIndexer solrIndexer;
    private final CloseableIterable<JsonNode> source;
    private final Writer<JsonNode> writer;
    private final Converter<JsonNode> converter;

    /**
     * Builder for an Indexer. More options to come.
     */
    public static class Builder {
        private String solrUrl = DEFAULT_SOLR_URL;
        private String ehriUrl = DEFAULT_EHRI_URL;

        private CloseableIterable<JsonNode> source = null;
        private List<Writer<JsonNode>> writers = Lists.newArrayList();

        private Converter<JsonNode> converter;

        public Builder addWriter(Writer<JsonNode> writer) {
            writers.add(writer);
            return this;
        }

        private Writer<JsonNode> getWriter() {
            if (writers.size() > 1) {
                return new MultiWriter<JsonNode, Writer<JsonNode>>(
                    writers.toArray(new Writer[writers.size()]));
            } else if (writers.size() == 1) {
                return writers.get(0);
            } else {
                return new NoopWriter<JsonNode>();
            }
        }

        private String getSolrUrl() {
            return solrUrl;
        }

        public void setSolrUrl(String solrUrl) {
            this.solrUrl = solrUrl;
        }

        private String getEhriUrl() {
            return ehriUrl;
        }

        public void setEhriUrl(String ehriUrl) {
            this.ehriUrl = ehriUrl;
        }

        public CloseableIterable<JsonNode> getSource() {
            return source;
        }

        public Builder setSource(CloseableIterable<JsonNode> source) {
            this.source = source;
            return this;
        }

        public Converter<JsonNode> getConverter() {
            return converter;
        }

        public Builder setConverter(Converter<JsonNode> converter) {
            this.converter = converter;
            return this;
        }

        public Indexer build() {
            if (source == null) {
                throw new IllegalStateException("Source has not been given");
            }
            if (converter == null) {
                throw new IllegalStateException("Converter has not been given");
            }
            return new Indexer(this);
        }
    }

    private Indexer(Builder builder) {
        this.solrIndexer = new SolrIndexer(builder.getSolrUrl());
        this.ehriUrl = builder.getEhriUrl();
        this.writer = builder.getWriter();
        this.source = builder.getSource();
        this.converter = builder.getConverter();
    }

    public void doIt() {
        try {
            for (JsonNode node : source) {
                for (JsonNode out : converter.convert(node)) {
                    writer.write(out);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error converting items: ", e);
        } finally {
            source.close();
            writer.close();
        }
    }

    public static void main(String[] args) throws IOException, ParseException {

        Options options = new Options();
        options.addOption("p", "print", false,
                "Print converted JSON to stdout. Also implied by --noindex.");
        options.addOption("P", "pretty", false,
                "Pretty print out JSON given by --print.");
        options.addOption("s", "solr", true,
                "Base URL for Solr service (minus the action segment).");
        options.addOption("f", "file", true,
                "Read input from a file instead of the REST service. Use '-' for stdin.");
        options.addOption("e", "ehri", true,
                "Base URL for EHRI REST service.");
        options.addOption("n", "noindex", false,
                "Don't perform actual indexing.");
        options.addOption("n", "noconvert", false,
                "Don't convert data to index format. Implies --noindex.");
        options.addOption("v", "verbose", false,
                "Print index stats.");
        options.addOption("V", "veryverbose", false,
                "Print individual item ids");
        options.addOption("h", "help", false,
                "Print this message.");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        String usage = "indexer  [OPTIONS] <spec> ... <specN>";
        String help = "\n" +
                "Each <spec> should consist of:\n" +
                "   - an item type (all items of that type)\n" +
                "   - an item id prefixed with '@' (individual items)\n" +
                "   - a type|id (bar separated - all children of an item)\n";


        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, null, options, help);
            System.exit(1);
        }

        Indexer.Builder builder = new Indexer.Builder();
        if (cmd.hasOption("solr")) {
            builder.setSolrUrl(cmd.getOptionValue("solr"));
        }
        if (cmd.hasOption("ehri")) {
            builder.setEhriUrl(cmd.getOptionValue("ehri"));
        }
        if (cmd.hasOption("noindex") || cmd.hasOption("print") || cmd.hasOption("pretty")) {
            builder.addWriter(new OutputStreamWriter(System.out, cmd.hasOption("pretty")));
        }
        if (!(cmd.hasOption("noconvert") || cmd.hasOption("noindex"))) {
            builder.addWriter(new IndexWriter(builder.solrUrl));
        }
        if (cmd.hasOption("noconvert")) {
            builder.setConverter(new NoopConverter<JsonNode>());
        } else {
            builder.setConverter(new JsonConverter());
        }
        if (cmd.hasOption("verbose") || cmd.hasOption("veryVerbose")) {
            builder.addWriter(new StatsWriter(System.err, cmd.hasOption("veryVerbose")));
        }
        if (cmd.hasOption("file")) {
            String fileName = cmd.getOptionValue("file");
            if (fileName.trim().equals("-")) {
                builder.setSource(new InputStreamSource(System.in));
            } else {
                // Mmmn, who should close our file? The InputStreamSource
                // or us, at some point???
                throw new NotImplementedException();
            }
        } else {
            builder.setSource(new RestServiceSource(cmd.getArgs()));
        }

        Indexer indexer = builder.build();
        indexer.doIt();
    }
}
