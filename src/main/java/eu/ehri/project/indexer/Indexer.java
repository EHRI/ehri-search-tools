package eu.ehri.project.indexer;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.indexer.converter.Converter;
import eu.ehri.project.indexer.converter.impl.JsonConverter;
import eu.ehri.project.indexer.converter.impl.NoopConverter;
import eu.ehri.project.indexer.index.Index;
import eu.ehri.project.indexer.index.impl.SolrIndex;
import eu.ehri.project.indexer.sink.Sink;
import eu.ehri.project.indexer.sink.impl.*;
import eu.ehri.project.indexer.source.Source;
import eu.ehri.project.indexer.source.impl.*;
import eu.ehri.project.indexer.utils.Stats;
import org.apache.commons.cli.*;
import org.codehaus.jackson.JsonNode;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Pull data from the EHRI REST API and index it in Solr.
 *         <p/>
 *         Majorly overengineered for the fun of it, and to allow very
 *         flexible input/output options without incurring excessive
 *         complexity in the main logic. Orchestrates a source, a
 *         converter, and one or more sink objects to get some JSON
 *         data, convert it to another format, and put it somewhere.
 */
public class Indexer {

    /**
     * Default service end points.
     * <p/>
     * TODO: Store these in a properties file?
     */
    private static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr/portal";
    private static final String DEFAULT_EHRI_URL = "http://localhost:7474/ehri";

    private final Source<JsonNode> source;
    private final Sink<JsonNode> writer;
    private final Converter<JsonNode> converter;

    /**
     * Builder for an Indexer. More options to come.
     */
    public static class Builder {
        private final List<Source<JsonNode>> sources = Lists.newArrayList();
        private final List<Sink<JsonNode>> writers = Lists.newArrayList();

        private Converter<JsonNode> converter;

        public Builder addSink(Sink<JsonNode> writer) {
            writers.add(writer);
            return this;
        }

        private Sink<JsonNode> getWriter() {
            if (writers.size() > 1) {
                return new MultiSink<JsonNode, Sink<JsonNode>>(writers);
            } else if (writers.size() == 1) {
                return writers.get(0);
            } else {
                return new NoopSink<JsonNode>();
            }
        }

        public Source<JsonNode> getSources() {
            if (sources.size() > 1) {
                return new MultiSource<JsonNode>(sources);
            } else if (sources.size() == 1) {
                return sources.get(0);
            } else {
                return new NoopSource<JsonNode>();
            }
        }

        public Builder addSource(Source<JsonNode> source) {
            this.sources.add(source);
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
            if (sources.isEmpty()) {
                throw new IllegalStateException("Source has not been given");
            }
            if (converter == null) {
                throw new IllegalStateException("Converter has not been given");
            }
            return new Indexer(this);
        }
    }

    private Indexer(Builder builder) {
        this.writer = builder.getWriter();
        this.source = builder.getSources();
        this.converter = builder.getConverter();
    }

    /**
     * Perform the actual actions.
     */
    public void runIndex() {
        try {
            for (JsonNode node : source) {
                for (JsonNode out : converter.convert(node)) {
                    writer.write(out);
                }
            }
        } finally {
            source.finish();
            writer.close();
        }
    }

    /**
     * Turn a list of specs into a set of EHRI REST URLs to download
     * JSON lists from.
     * <p/>
     * This is gross and subject to change.
     *
     * @param serviceUrl The base REST URL
     * @param specs      A list of specs
     * @return A list of URLs
     */
    public static List<URI> urlsFromSpecs(String serviceUrl, String... specs) {
        List<URI> urls = Lists.newArrayList();
        List<String> ids = Lists.newArrayList();
        for (String spec : specs) {
            // Item type and id - denotes fetching child items (?)
            if (spec.contains("|")) {
                Iterable<String> split = Splitter.on("|").limit(2).split(spec);
                String type = Iterables.get(split, 0);
                String id = Iterables.get(split, 1);
                URI url = UriBuilder.fromPath(serviceUrl)
                        .segment(type).segment(id).segment("list")
                        .queryParam("limit", -1)
                        .queryParam("all", true).build();
                urls.add(url);
            } else if (spec.startsWith("@")) {
                ids.add(spec.substring(1));
            } else {
                URI url = UriBuilder.fromPath(serviceUrl)
                        .segment(spec).segment("list")
                        .queryParam("limit", -1).build();
                urls.add(url);
            }
        }

        // Unlike types or children, multiple ids are done in one request.
        UriBuilder idBuilder = UriBuilder.fromPath(serviceUrl).segment("entities");
        for (String id : ids) {
            idBuilder = idBuilder.queryParam("id", id);
        }
        urls.add(idBuilder.queryParam("limit", -1).build());
        return urls;
    }


    public static void main(String[] args) throws IOException, ParseException {

        // Long opts
        final String PRINT = "print";
        final String PRETTY = "pretty";
        final String CLEAR_ALL = "clear-all";
        final String CLEAR_ID = "clear-id";
        final String CLEAR_TYPE = "clear-type";
        final String FILE = "file";
        final String REST_URL = "rest";
        final String SOLR_URL = "solr";
        final String NO_INDEX = "noindex";
        final String NO_CONVERT = "noconvert";
        final String VERBOSE = "verbose";
        final String VERY_VERBOSE = "veryverbose";
        final String HELP = "help";


        Options options = new Options();
        options.addOption("p", "print", false,
                "Print converted JSON to stdout. Also implied by --noindex.");
        options.addOption("D", CLEAR_ALL, false,
                "Clear entire index first (use with caution.)");
        options.addOption("c", CLEAR_ID, true,
                "Clear an individual id. Can be used multiple times.");
        options.addOption("C", CLEAR_TYPE, true,
                "Clear an item type. Can be used multiple times.");
        options.addOption("P", PRETTY, false,
                "Pretty print out JSON given by --print.");
        options.addOption("s", SOLR_URL, true,
                "Base URL for Solr service (minus the action segment).");
        options.addOption("f", FILE, true,
                "Read input from a file instead of the REST service. Use '-' for stdin.");
        options.addOption("r", REST_URL, true,
                "Base URL for EHRI REST service.");
        options.addOption("n", NO_INDEX, false,
                "Don't perform actual indexing.");
        options.addOption("n", NO_CONVERT, false,
                "Don't convert data to index format. Implies --noindex.");
        options.addOption("v", VERBOSE, false,
                "Print index stats.");
        options.addOption("V", VERY_VERBOSE, false,
                "Print individual item ids");
        options.addOption("h", HELP, false,
                "Print this message.");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        String usage = "indexer  [OPTIONS] <spec> ... <specN>";
        String help = "\n" +
                "Each <spec> should consist of:\n" +
                "   - an item type (all items of that type)\n" +
                "   - an item id prefixed with '@' (individual items)\n" +
                "   - a type|id (bar separated - all children of an item)\n\n";

        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, null, options, help);
            System.exit(1);
        }

        String ehriUrl = cmd.getOptionValue(REST_URL, DEFAULT_EHRI_URL);
        String solrUrl = cmd.getOptionValue(SOLR_URL, DEFAULT_SOLR_URL);

        Indexer.Builder builder = new Indexer.Builder();

        // Initialize the index...
        Index index = new SolrIndex(solrUrl);

        // Check if we need to clear the index...
        boolean commitOnDelete = cmd.hasOption(NO_CONVERT) || cmd.hasOption(NO_INDEX);
        if (cmd.hasOption(CLEAR_ALL)) {
            index.deleteAll(commitOnDelete);
        } else {
            if (cmd.hasOption(CLEAR_ID)) {
                String[] ids = cmd.getOptionValues(CLEAR_ID);
                index.deleteItems(Lists.newArrayList(ids), commitOnDelete);
            }
            if (cmd.hasOption(CLEAR_TYPE)) {
                String[] types = cmd.getOptionValues(CLEAR_TYPE);
                index.deleteTypes(Lists.newArrayList(types), commitOnDelete);
            }
        }

        // Determine if we're printing the data...
        if (cmd.hasOption(NO_INDEX) || cmd.hasOption(PRINT) || cmd.hasOption(PRETTY)) {
            builder.addSink(new OutputStreamJsonSink(System.out, cmd.hasOption(PRETTY)));
        }

        // Determine if we need to actually index the data...
        if (!(cmd.hasOption(NO_CONVERT) || cmd.hasOption(NO_INDEX))) {
            builder.addSink(new IndexJsonSink(index));
        }

        // Determine if we want to convert the data or print the incoming
        // JSON as-is...
        if (cmd.hasOption(NO_CONVERT)) {
            builder.setConverter(new NoopConverter<JsonNode>());
        } else {
            builder.setConverter(new JsonConverter());
        }

        // See if we want to print stats... if so create a callback sink
        // to count the individual items and optionally print them...
        if (cmd.hasOption(VERBOSE) || cmd.hasOption(VERY_VERBOSE)) {
            final Stats stats = new Stats();
            final boolean vv = cmd.hasOption(VERY_VERBOSE);
            CallbackSink.Callback<JsonNode> cb = new CallbackSink.Callback<JsonNode>() {
                @Override
                public void call(JsonNode jsonNode) {
                    stats.incrementCount();
                    if (vv) {
                        System.err.println(jsonNode.path("type").asText()
                                + " -> " + jsonNode.path("id").asText());
                    }
                }

                @Override
                public void finish() {
                    stats.printReport(System.err);
                }
            };
            //noinspection unchecked
            builder.addSink(new CallbackSink<JsonNode>(cb));
        }

        // Determine the source, either stdin, a file, or the rest service.
        if (cmd.hasOption(FILE)) {
            String fileName = cmd.getOptionValue(FILE);
            if (fileName.trim().equals("-")) {
                builder.addSource(new InputStreamJsonSource(System.in));
            } else {
                builder.addSource(new FileJsonSource(fileName));
            }
        }

        // Parse the command line specs...
        for (URI uri : urlsFromSpecs(ehriUrl, cmd.getArgs())) {
            builder.addSource(new WebJsonSource(uri));
        }

        builder.build().runIndex();
    }
}
