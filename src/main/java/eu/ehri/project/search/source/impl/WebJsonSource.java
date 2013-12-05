package eu.ehri.project.search.source.impl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.project.search.source.Source;
import org.codehaus.jackson.JsonNode;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Properties;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Fetch JSON from a web resource. It must accept and
 *         return MediaType application/json.
 */
public class WebJsonSource implements Source<JsonNode> {
    private final Client client;
    private final URI url;
    private ClientResponse response = null;
    private InputStreamJsonSource ios = null;
    private Properties headers;

    public WebJsonSource(URI url) {
        this(Client.create(), url, new Properties());
    }

    public WebJsonSource(URI url, Properties headers) {
        this(Client.create(), url, headers);
    }

    public WebJsonSource(Client client, URI url, Properties headers) {
        this.client = client;
        this.url = url;
        this.headers = headers;
    }

    public void finish() throws SourceException {
        if (ios != null) {
            ios.finish();
        }
        if (response != null) {
            response.close();
        }
    }

    @Override
    public Iterable<JsonNode> getIterable() throws SourceException {
        response = getResponse();
        checkResponse(response);
        ios = new InputStreamJsonSource(response.getEntityInputStream());
        return ios.getIterable();
    }

    private ClientResponse getResponse() throws SourceException {
        try {
            WebResource.Builder rsc = client.resource(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON);
            for (String header : headers.stringPropertyNames()) {
                rsc = rsc.header(header, headers.getProperty(header));
            }
            return rsc.get(ClientResponse.class);
        } catch (Exception e) {
            throw new SourceException(
                    "Error accessing web resource: '" + url + "': \n" + e.getMessage(), e);
        }
    }

    /**
     * Check a REST API response is good.
     *
     * @param response The response object to check
     */
    private void checkResponse(ClientResponse response) throws SourceException {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new SourceException(
                    "Unexpected response from web resource: " + response.getStatus());
        }
    }
}
