package eu.ehri.project.search.source.impl;

import eu.ehri.project.search.source.Source;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class InputStreamJsonSource implements Source<JsonNode> {
    private static final JsonFactory jsonFactory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final InputStream ios;
    private JsonParser jsonParser;

    public InputStreamJsonSource(InputStream ios) {
        this.ios = ios;
    }

    public void finish() throws SourceException {
        if (jsonParser != null) {
            try {
                jsonParser.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing JSON parser", e);
            }
        }
    }

    @Override
    public Iterable<JsonNode> getIterable() throws SourceException {
        try {
            jsonParser = jsonFactory.createJsonParser(ios);
            JsonToken firstToken = jsonParser.nextToken();
            if (firstToken != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Excepted a JSON array, instead first token was: " + firstToken);
            }
            final MappingIterator<JsonNode> iterator = mapper.readValues(jsonParser, JsonNode.class);
            return new Iterable<JsonNode>() {
                @Override
                public Iterator<JsonNode> iterator() {
                    return iterator;
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("Error reading JSON stream: ", e);
        }
    }
}
