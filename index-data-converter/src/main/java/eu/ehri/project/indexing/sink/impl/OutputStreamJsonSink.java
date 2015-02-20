package eu.ehri.project.indexing.sink.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.ehri.project.indexing.sink.Sink;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class OutputStreamJsonSink implements Sink<JsonNode> {

    private static final JsonFactory factory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OutputStream out;
    private JsonGenerator generator;
    private PrintWriter pw;
    private final boolean pretty;
    private final ObjectWriter writer;

    public OutputStreamJsonSink(OutputStream out) {
        this(out, false);
    }

    public OutputStreamJsonSink(OutputStream out, boolean pretty) {
        this.out = out;
        this.pretty = pretty;
        this.writer = pretty
                ? mapper.writerWithDefaultPrettyPrinter()
                : mapper.writer();
    }

    public void write(JsonNode node) throws SinkException {
        try {
            if (generator == null) {
                pw = new PrintWriter(out);
                generator = factory.createGenerator(pw);
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
