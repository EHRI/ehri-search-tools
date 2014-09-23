package eu.ehri.project.indexing.sink.impl;

import eu.ehri.project.indexing.sink.Sink;
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

    public void write(JsonNode node) throws SinkException {
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
            throw new SinkException("Error writing json data: ", e);
        }

    }

    public void finish() throws SinkException {
        if (generator != null) {
            try {
                generator.writeEndArray();
                generator.flush();
                pw.flush();
            } catch (IOException e) {
                throw new SinkException("Error closing JSON writer: ", e);
            }
        }
    }
}
