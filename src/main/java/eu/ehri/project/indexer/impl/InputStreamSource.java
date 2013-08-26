package eu.ehri.project.indexer.impl;

import eu.ehri.project.indexer.CloseableIterable;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class InputStreamSource implements CloseableIterable<JsonNode> {
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonParser jsonParser;
    private final InputStream ios;

    public InputStreamSource(InputStream ios) {
        this.ios = ios;
    }

    public void close() {
        if (jsonParser != null) {
            try {
                jsonParser.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing JSON parser", e);
            }
        }
    }

    @Override
    public Iterator<JsonNode> iterator() {
        try {
            jsonParser = jsonFactory.createJsonParser(ios);
            JsonToken firstToken = jsonParser.nextToken();
            if (firstToken != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Excepted a JSON array, instead first token was: " + firstToken);
            }
            return mapper.readValues(jsonParser, JsonNode.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading JSON stream: ", e);
        }
    }

    public static void main(String[] args) {
        InputStreamSource ios = new InputStreamSource(System.in);
        OutputStreamWriter npw = new OutputStreamWriter(System.out);
        try {
            for (JsonNode n : ios) {
                npw.write(n);
            }
        } finally {
            ios.close();
            npw.close();
        }
    }
}
