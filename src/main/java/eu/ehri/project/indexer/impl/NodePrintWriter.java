package eu.ehri.project.indexer.impl;

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
public class NodePrintWriter {

    private static final JsonFactory factory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final static ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    private final OutputStream out;
    private JsonGenerator generator;
    private PrintWriter pw;


    public NodePrintWriter(OutputStream out) {
        this.out = out;
    }

    public void write(JsonNode node) {
        try {
            if (generator == null) {
                pw = new PrintWriter(out);
                generator = factory.createJsonGenerator(pw);
                generator.writeStartArray();
            }
            generator.writeRaw('\n');
            writer.writeValue(generator, node);
        } catch (IOException e) {
            throw new RuntimeException("Error writing json data: ", e);
        }

    }

    public void close() {
        if (generator != null) {
            try {
                generator.writeEndArray();
                generator.writeRaw('\n');
                generator.flush();
                generator.close();
                pw.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing JSON writer: ", e);
            }
        }
    }
}
