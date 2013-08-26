package eu.ehri.project.indexer;

import com.google.common.collect.Lists;
import eu.ehri.project.indexer.converter.Converter;
import eu.ehri.project.indexer.converter.impl.JsonConverter;
import eu.ehri.project.indexer.converter.impl.NoopConverter;
import eu.ehri.project.indexer.index.impl.SolrIndex;
import eu.ehri.project.indexer.sink.Sink;
import eu.ehri.project.indexer.sink.impl.*;
import eu.ehri.project.indexer.source.Source;
import eu.ehri.project.indexer.source.impl.FileSource;
import eu.ehri.project.indexer.source.impl.InputStreamSource;
import eu.ehri.project.indexer.source.impl.RestServiceSource;
import org.apache.commons.cli.*;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
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
        private Source<JsonNode> source = null;
        private final List<Sink<JsonNode>> writers = Lists.newArrayList();

        private Converter<JsonNode> converter;

        public Builder addWriter(Sink<JsonNode> writer) {
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

        public Source<JsonNode> getSource() {
            return source;
        }

        public Builder setSource(Source<JsonNode> source) {
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
        this.writer = builder.getWriter();
        this.source = builder.getSource();
        this.converter = builder.getConverter();
    }

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
                "   - a type|id (bar separated - all children of an item)\n\n";

        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, null, options, help);
            System.exit(1);
        }

        String ehriUrl = cmd.getOptionValue("ehri", DEFAULT_EHRI_URL);
        String solrUrl = cmd.getOptionValue("solr", DEFAULT_SOLR_URL);

        Indexer.Builder builder = new Indexer.Builder();

        // Determine if we're printing the data...
        if (cmd.hasOption("noindex") || cmd.hasOption("print") || cmd.hasOption("pretty")) {
            builder.addWriter(new OutputStreamSink(System.out, cmd.hasOption("pretty")));
        }

        // Determine if we need to actually index the data...
        if (!(cmd.hasOption("noconvert") || cmd.hasOption("noindex"))) {
            builder.addWriter(new IndexingSink(new SolrIndex(solrUrl)));
        }

        // Determine if we want to convert the data or print the incoming
        // JSON as-is...
        if (cmd.hasOption("noconvert")) {
            builder.setConverter(new NoopConverter<JsonNode>());
        } else {
            builder.setConverter(new JsonConverter());
        }

        // See if we want to print stats...
        if (cmd.hasOption("verbose") || cmd.hasOption("veryVerbose")) {
            builder.addWriter(new StatsSink(System.err, cmd.hasOption("veryVerbose")));
        }

        // Determine the source, either stdin, a file, or the rest service.
        if (cmd.hasOption("file")) {
            String fileName = cmd.getOptionValue("file");
            if (fileName.trim().equals("-")) {
                builder.setSource(new InputStreamSource(System.in));
            } else {
                builder.setSource(new FileSource(fileName));
            }
        } else {
            builder.setSource(new RestServiceSource(ehriUrl, cmd.getArgs()));
        }

        Indexer indexer = builder.build();
        indexer.runIndex();
    }
}
