package eu.ehri.project.indexing.source.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.project.indexing.source.Source;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Fetch JSON from a web resource. It must accept and
 *         return MediaType application/json.
 */
public class WebJsonSource implements Source<JsonNode> {
    private final URI url;
    private ClientResponse response = null;
    private InputStreamJsonSource ios = null;
    private Properties headers;
    private boolean finished = false;

    public WebJsonSource(URI url, Properties headers) {
        this.url = url;
        this.headers = headers;
    }

    public void finish() throws SourceException {
        if (!finished) {
            if (ios != null) {
                ios.finish();
                ios = null;
            }
            if (response != null) {
                response.close();
                response = null;
            }
            finished = true;
        }
    }

    @Override
    public Iterable<JsonNode> getIterable() throws SourceException {
        response = getResponse();
        checkResponse(response);
        InputStream entityInputStream = response.getEntityInputStream();
        if (entityInputStream == null) {
            throw new SourceException("Entity stream is null for url: " + url);
        }
        ios = new InputStreamJsonSource(entityInputStream);
        return ios.getIterable();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    private ClientResponse getResponse() throws SourceException {
        try {
            WebResource.Builder rsc = Client.create().resource(url)
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
