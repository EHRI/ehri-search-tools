package eu.ehri.project.indexer.source.impl;

import eu.ehri.project.indexer.source.Source;
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
public class InputStreamSource implements Source<JsonNode> {
    private static final JsonFactory jsonFactory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final InputStream ios;
    private JsonParser jsonParser;

    public InputStreamSource(InputStream ios) {
        this.ios = ios;
    }

    public void finish() {
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
}
