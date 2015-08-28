package eu.ehri.project.indexing.source.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import eu.ehri.project.indexing.source.Source;

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

    public void close() throws SourceException {
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
    public Iterable<JsonNode> iterable() throws SourceException {
        try {
            jsonParser = jsonFactory.createParser(ios);
            JsonToken firstToken = jsonParser.nextValue();
            if (!jsonParser.isExpectedStartArrayToken()) {
                throw new SourceException("Excepted a JSON array, instead first token was: " + firstToken);
            }
            // NB: Since the iterator is run lazily, a parse error here will
            // not be caught and instead throw a RuntimeException "Unexpected character..."
            // I'm not sure how we can fix that.
            if (jsonParser.nextValue() == JsonToken.END_ARRAY) {
                return Lists.newArrayListWithExpectedSize(0);
            } else {
                final Iterator<JsonNode> iterator = jsonParser.readValuesAs(JsonNode.class);
                return new Iterable<JsonNode>() {
                    @Override
                    public Iterator<JsonNode> iterator() {
                        return iterator;
                    }
                };
            }
        } catch (IOException e) {
            throw new SourceException("Error reading JSON stream: ", e);
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
