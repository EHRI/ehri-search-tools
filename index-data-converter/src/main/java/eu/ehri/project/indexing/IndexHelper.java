package eu.ehri.project.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;
import eu.ehri.project.indexing.converter.impl.JsonConverter;
import eu.ehri.project.indexing.converter.impl.MultiConverter;
import eu.ehri.project.indexing.converter.impl.NoopConverter;
import eu.ehri.project.indexing.index.Index;
import eu.ehri.project.indexing.index.impl.SolrIndex;
import eu.ehri.project.indexing.sink.Sink;
import eu.ehri.project.indexing.sink.impl.*;
import eu.ehri.project.indexing.source.Source;
import eu.ehri.project.indexing.source.impl.*;
import eu.ehri.project.indexing.utils.Stats;
import org.apache.commons.cli.*;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;

/**
 * Pull data from the EHRI REST API and index it in Solr.
 * <p/>
 * Designed allow very flexible input/output options without
 * incurring excessive complexity in the main logic. Orchestrates
 * a source, a converter, and one or more sink objects to get some JSON
 * data, convert it to another format, and put it somewhere.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IndexHelper<T> {

    public static final String PROGRAM_NAME = "index-helper";
    public static final String VERSION_NUMBER = "1.0.1";

    enum ErrCodes {
        BAD_SOURCE_ERR(3),
        BAD_SINK_ERR(4),
        BAD_CONVERSION_ERR(5),
        BAD_STATE_ERR(6),
        INDEX_ERR(7);

        private int code;

        ErrCodes(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * Default service end points.
     * <p/>
     * TODO: Store these in a properties file?
     */
    private static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr/portal";
    private static final String DEFAULT_EHRI_URL = "http://localhost:7474/ehri";

    private final Source<T> source;
    private final Sink<T> writer;
    private final Converter<T> converter;

    /**
     * Builder for an Indexer. More options to come.
     */
    public static class Builder<T> {
        private final List<Source<T>> sources = Lists.newArrayList();
        private final List<Sink<T>> writers = Lists.newArrayList();
        private List<Converter<T>> converters = Lists.newArrayList();

        public Builder<T> addSink(Sink<T> writer) {
            writers.add(writer);
            return this;
        }

        private Sink<T> getSink() {
            if (writers.size() > 1) {
                return new MultiSink<>(writers);
            } else if (writers.size() == 1) {
                return writers.get(0);
            } else {
                return new NoopSink<>();
            }
        }

        public Source<T> getSource() {
            if (sources.size() > 1) {
                return new MultiSource<>(sources);
            } else if (sources.size() == 1) {
                return sources.get(0);
            } else {
                return new NoopSource<>();
            }
        }

        public Builder<T> addSource(Source<T> source) {
            this.sources.add(source);
            return this;
        }

        public Converter<T> getConverter() {
            if (converters.size() > 1) {
                return new MultiConverter<>(converters);
            } else if (converters.size() == 1) {
                return converters.get(0);
            } else {
                return new NoopConverter<>();
            }
        }

        public Builder<T> addConverter(Converter<T> converter) {
            this.converters.add(converter);
            return this;
        }

        public IndexHelper<T> build() {
            return new IndexHelper<>(this);
        }
    }

    private IndexHelper(Builder<T> builder) {
        this.writer = builder.getSink();
        this.source = builder.getSource();
        this.converter = builder.getConverter();
    }

    /**
     * Perform the actual actions.
     */
    public void iterate() throws Source.SourceException, Sink.SinkException, Converter.ConverterException {
        try {
            for (T item : source.getIterable()) {
                for (T out : converter.convert(item)) {
                    writer.write(out);
                }
            }
        } finally {
            source.finish();
            writer.finish();
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
        if (!ids.isEmpty()) {
            UriBuilder idBuilder = UriBuilder.fromPath(serviceUrl).segment("entities");
            for (String id : ids) {
                idBuilder = idBuilder.queryParam("id", id);
            }
            urls.add(idBuilder.queryParam("limit", -1).build());
        }
        return urls;
    }


    public static void main(String[] args) throws IOException, ParseException {

        // Long opts
        final String PRINT = "print";
        final String PRETTY = "pretty";
        final String CLEAR_ALL = "clear-all";
        final String CLEAR_KEY_VALUE = "clear-key-value";
        final String CLEAR_ID = "clear-id";
        final String CLEAR_TYPE = "clear-type";
        final String FILE = "file";
        final String REST_URL = "rest";
        final String HEADERS = "H";
        final String SOLR_URL = "solr";
        final String INDEX = "index";
        final String NO_CONVERT = "noconvert";
        final String VERBOSE = "verbose";
        final String VERSION = "version";
        final String STATS = "stats";
        final String HELP = "help";

        Options options = new Options();
        options.addOption("p", "print", false,
                "Print converted JSON to stdout. The default action in the omission of --index.");
        options.addOption("D", CLEAR_ALL, false,
                "Clear entire index first (use with caution.)");
        options.addOption(OptionBuilder.withLongOpt(CLEAR_KEY_VALUE)
                .withArgName("key=value")
                .hasArgs(2).withValueSeparator()
                .withDescription("Clear items with a given key=value pair. Can be used multiple times.")
                .create("K"));
        options.addOption("c", CLEAR_ID, true,
                "Clear an individual id. Can be used multiple times.");
        options.addOption("C", CLEAR_TYPE, true,
                "Clear an item type. Can be used multiple times.");
        options.addOption("P", PRETTY, false,
                "Pretty print out JSON given by --print (implies --print).");
        options.addOption("s", SOLR_URL, true,
                "Base URL for Solr service (minus the action segment.)");
        options.addOption("f", FILE, true,
                "Read input from a file instead of the REST service. Use '-' for stdin.");
        options.addOption("r", REST_URL, true,
                "Base URL for EHRI REST service.");
        options.addOption(OptionBuilder.withArgName("header=value")
                .hasArgs(2).withValueSeparator()
                .withDescription("Set a header for the REST service.")
                .create(HEADERS));
        options.addOption("i", INDEX, false,
                "Index the data. This is NOT the default for safety reasons.");
        options.addOption("n", NO_CONVERT, false,
                "Don't convert data to index format.");
        options.addOption("v", VERBOSE, false,
                "Print individual item ids to show progress.");
        options.addOption(OptionBuilder.withLongOpt(VERSION)
                .withDescription("Print the version number and exit.")
                .create());
        options.addOption("S", STATS, false, "Print indexing stats.");
        options.addOption("h", HELP, false, "Print this message.");

        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(VERSION)) {
            System.out.println(PROGRAM_NAME + " " + VERSION_NUMBER);
            System.exit(0);
        }

        String usage = PROGRAM_NAME + " [OPTIONS] <spec> ... <specN>";
        String help = "\n" +
                "Each <spec> should consist of:\n" +
                "   * an item type (all items of that type)\n" +
                "   * an item id prefixed with '@' (individual items)\n" +
                "   * a type|id (bar separated - all children of an item)\n\n\n" +
                "The default URIs for Solr and the REST service are:\n" +
                " * " + DEFAULT_EHRI_URL + "\n" +
                " * " + DEFAULT_SOLR_URL + "\n\n";

        if (cmd.hasOption(HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, null, options, help);
            System.exit(1);
        }

        String ehriUrl = cmd.getOptionValue(REST_URL, DEFAULT_EHRI_URL);
        String solrUrl = cmd.getOptionValue(SOLR_URL, DEFAULT_SOLR_URL);
        Properties restHeaders = cmd.getOptionProperties(HEADERS);

        IndexHelper.Builder<JsonNode> builder = new IndexHelper.Builder<>();

        // Initialize the index...
        Index index = new SolrIndex(solrUrl);

        // Determine if we're printing the data...
        if (!cmd.hasOption(INDEX) || cmd.hasOption(PRINT) || cmd.hasOption(PRETTY)) {
            builder.addSink(new OutputStreamJsonSink(System.out, cmd.hasOption(PRETTY)));
        }

        // Determine if we need to actually index the data...
        if (cmd.hasOption(INDEX)) {
            builder.addSink(new IndexJsonSink(index));
        }

        // Determine if we want to convert the data or print the incoming
        // JSON as-is...
        if (!cmd.hasOption(NO_CONVERT)) {
            builder.addConverter(new JsonConverter());
        }

        // See if we want to print stats... if so create a callback sink
        // to count the individual items and optionally print them...
        if (cmd.hasOption(VERBOSE) || cmd.hasOption(STATS)) {
            final Stats stats = new Stats();
            final boolean printStats = cmd.hasOption(STATS);
            final boolean printItems = cmd.hasOption(VERBOSE);
            CallbackSink.Callback<JsonNode> cb = new CallbackSink.Callback<JsonNode>() {
                @Override
                public void call(JsonNode jsonNode) {
                    stats.incrementCount();
                    if (printItems) {
                        System.err.println(jsonNode.path("type").asText()
                                + " -> " + jsonNode.path("id").asText());
                    }
                }

                @Override
                public void finish() {
                    if (printStats) {
                        stats.printReport(System.err);
                    }
                }
            };

            builder.addSink(new CallbackSink<>(cb));
        }

        // Determine the source, either stdin, a file, or the rest service.
        if (cmd.hasOption(FILE)) {
            for (String fileName : cmd.getOptionValues(FILE)) {
                if (fileName.trim().equals("-")) {
                    builder.addSource(new InputStreamJsonSource(System.in));
                } else {
                    builder.addSource(new FileJsonSource(fileName));
                }
            }
        }

        // Parse the command line specs...
        for (URI uri : urlsFromSpecs(ehriUrl, cmd.getArgs())) {
            builder.addSource(new WebJsonSource(uri, restHeaders));
        }

        try {
            // Check if we need to clear anything in index... do this if we're NOT indexing.
            boolean commitOnDelete = !cmd.hasOption(INDEX);
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
                if (cmd.hasOption(CLEAR_KEY_VALUE)) {
                    Properties kvs = cmd.getOptionProperties(CLEAR_KEY_VALUE);
                    for (String key : kvs.stringPropertyNames()) {
                        index.deleteByFieldValue(key, kvs.getProperty(key), commitOnDelete);
                    }
                }
            }

            // Now do the main indexing tasks
            builder.build().iterate();
        } catch (Source.SourceException e) {
            System.err.println(e.getMessage());
            System.exit(ErrCodes.BAD_SOURCE_ERR.getCode());
        } catch (Converter.ConverterException e) {
            System.err.println(e.getMessage());
            System.exit(ErrCodes.BAD_CONVERSION_ERR.getCode());
        } catch (Sink.SinkException e) {
            System.err.println(e.getMessage());
            System.exit(ErrCodes.BAD_SINK_ERR.getCode());
        } catch (Index.IndexException e) {
            System.err.println(e.getMessage());
            System.exit(ErrCodes.INDEX_ERR.getCode());
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(ErrCodes.BAD_STATE_ERR.getCode());
        }
    }
}
