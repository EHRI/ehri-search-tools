package eu.ehri.project.indexer.source.impl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import eu.ehri.project.indexer.source.Source;
import org.codehaus.jackson.JsonNode;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Iterator;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Fetch JSON from a web resource. It must accept and
 *         return MediaType application/json.
 */
public class WebSource implements Source<JsonNode> {
    private final Client client;
    private final URI url;
    private ClientResponse response = null;
    private InputStreamSource ios = null;

    public WebSource(URI url) {
        this(Client.create(), url);
    }

    public WebSource(Client client, URI url) {
        this.client = client;
        this.url = url;
    }

    public void finish() {
        if (ios != null) {
            ios.finish();
        }
        if (response != null) {
            response.close();
        }
    }

    private ClientResponse getResponse() {
        return client.resource(url)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    }

    @Override
    public Iterator<JsonNode> iterator() {
        response = getResponse();
        checkResponse(response);
        ios = new InputStreamSource(response.getEntityInputStream());
        return ios.iterator();
    }

    /**
     * Check a REST API response is good.
     *
     * @param response The response object to check
     */
    private void checkResponse(ClientResponse response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new SourceException(
                    "Unexpected response from EHRI REST: " + response.getStatus());
        }
    }
}
