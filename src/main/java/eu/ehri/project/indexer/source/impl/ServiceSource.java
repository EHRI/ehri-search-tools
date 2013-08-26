package eu.ehri.project.indexer.source.impl;

import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.indexer.source.Source;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
abstract class ServiceSource implements Source<JsonNode> {
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper mapper = new ObjectMapper();
    private ClientResponse response = null;
    private JsonParser jsonParser = null;

    abstract ClientResponse getResponse();

    public void finish() {
        if (response != null) {
            response.close();
        }
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
        response = getResponse();
        checkResponse(response);
        try {
            jsonParser = jsonFactory.createJsonParser(
                    response.getEntityInputStream());
            JsonToken firstToken = jsonParser.nextToken();
            if (firstToken != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Excepted a JSON array, instead first token was: " + firstToken);
            }

            // Instead of just returning the mapping iterator, we override it
            // to finish the web resource once hasNext returns false...
            final MappingIterator<JsonNode> mappingIterator = mapper.readValues(jsonParser, JsonNode.class);
            return new Iterator<JsonNode>() {
                @Override
                public boolean hasNext() {
                    boolean next = mappingIterator.hasNext();
                    if (!next) {
                        finish();
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

    /**
     * Check a REST API response is good.
     *
     * @param response The response object to check
     */
    private void checkResponse(ClientResponse response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new RuntimeException("Unexpected response from EHRI REST: " + response.getStatus());
        }
    }
}
