package eu.ehri.project.indexing.source.impl;

import eu.ehri.project.indexing.source.Source;
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
public class InputStreamJsonSource implements Source<JsonNode> {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonFactory jsonFactory = new JsonFactory().setCodec(mapper);
    private final InputStream ios;
    private JsonParser jsonParser;
    private boolean finished = false;

    public InputStreamJsonSource(InputStream ios) {
        this.ios = ios;
    }

    public void finish() throws SourceException {
        finished = true;
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
            JsonToken firstToken = jsonParser.nextValue();
            if (firstToken != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Excepted a JSON array, instead first token was: " + firstToken);
            }
            JsonToken nextToken = jsonParser.nextValue();
            if (nextToken != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Excepted a JSON object after inital array, " +
                        "instead next token was: " + nextToken);
            }
            final Iterator<JsonNode> iterator = jsonParser.readValuesAs(JsonNode.class);
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

    @Override
    public boolean isFinished() {
        return finished;
    }
}
