package eu.ehri.project.indexer.impl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.project.indexer.CloseableIterable;
import org.codehaus.jackson.JsonNode;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

/**
* @author Mike Bryant (http://github.com/mikesname)
*/
class ChildItemSource extends ServiceSource implements CloseableIterable<JsonNode> {
    private final Client client;
    private final String type;
    private final String id;

    public ChildItemSource(Client client, String type, String id) {
        this.client = client;
        this.type = type;
        this.id = id;
    }
    
    @Override
    ClientResponse getResponse() {
        System.err.println("Initializing reader... " + this);
        WebResource resource = client.resource(
                UriBuilder.fromPath(RestServiceSource.URL).segment(type).segment(id).segment("list").build());
        return resource
                .queryParam("limit", "100000") // Ugly, but there's a default limit
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    }
}
