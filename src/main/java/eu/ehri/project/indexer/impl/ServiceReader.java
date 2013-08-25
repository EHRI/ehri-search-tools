package eu.ehri.project.indexer.impl;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.indexer.CloseableIterable;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;

/**
* @author Mike Bryant (http://github.com/mikesname)
*/
abstract class ServiceReader implements CloseableIterable<JsonNode> {
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper mapper = new ObjectMapper();
    private ClientResponse response = null;

    abstract ClientResponse getResponse();

    public void close() {
        System.err.println("Closing reader: " + this);
        if (response != null)
            response.close();
    }

    @Override
    public Iterator<JsonNode> iterator() {
        response = getResponse();
        try {
            JsonParser jsonParser = jsonFactory.createJsonParser(
                    response.getEntityInputStream());
            JsonToken firstToken = jsonParser.nextToken();
            if (firstToken != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Excepted a JSON array, instead first token was: " + firstToken);
            }

            // Instead of just returning the mapping iterator, we override it
            // to close the web resource once hasNext returns false...
            final MappingIterator<JsonNode> mappingIterator = mapper.readValues(jsonParser, JsonNode.class);
            return new Iterator<JsonNode>() {
                @Override
                public boolean hasNext() {
                    boolean next = mappingIterator.hasNext();
                    if (!next) {
                        close();
                    }
                    return next;
                }

                @Override
                public JsonNode next() {
                    return mappingIterator.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("Error reading JSON stream: ", e);
        }
    }
}
