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
class TypeSource extends ServiceSource implements CloseableIterable<JsonNode> {
    private final Client client;
    private final String serviceUrl;
    private final String type;

    public TypeSource(Client client, String serviceUrl, String type) {
        this.client = client;
        this.serviceUrl = serviceUrl;
        this.type = type;
    }

    @Override
    ClientResponse getResponse() {
        WebResource resource = client.resource(
                UriBuilder.fromPath(serviceUrl).segment(type).segment("list").build());
        return resource
                .queryParam("limit", "-1") // Ugly, but there's a default limit
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    }
}
