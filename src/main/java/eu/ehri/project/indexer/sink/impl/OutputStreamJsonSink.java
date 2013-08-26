package eu.ehri.project.indexer.sink.impl;

import eu.ehri.project.indexer.sink.Sink;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class OutputStreamJsonSink implements Sink<JsonNode> {

    private static final JsonFactory factory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final static ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    private final OutputStream out;
    private JsonGenerator generator;
    private PrintWriter pw;
    private final boolean pretty;

    public OutputStreamJsonSink(OutputStream out) {
        this(out, false);
    }

    public OutputStreamJsonSink(OutputStream out, boolean pretty) {
        this.out = out;
        this.pretty = pretty;
    }

    public void write(JsonNode node) {
        try {
            if (generator == null) {
                pw = new PrintWriter(out);
                generator = factory.createJsonGenerator(pw);
                if (pretty) {
                    generator.useDefaultPrettyPrinter();
                }
                generator.writeStartArray();
            }
            writer.writeValue(generator, node);
        } catch (IOException e) {
            throw new RuntimeException("Error writing json data: ", e);
        }

    }

    public void close() {
        if (generator != null) {
            try {
                generator.writeEndArray();
                generator.flush();
                pw.flush();
            } catch (IOException e) {
                throw new RuntimeException("Error closing JSON writer: ", e);
            }
        }
    }
}
