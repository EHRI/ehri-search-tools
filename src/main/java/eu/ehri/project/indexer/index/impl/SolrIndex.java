package eu.ehri.project.indexer.index.impl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.project.indexer.index.Index;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.InputStream;

public class SolrIndex implements Index {
    /**
     * Fields.
     */

    private final String solrUrl;
    private static final Client client = Client.create();

    public SolrIndex(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    /**
     * Commit the Solr updates.
     */
    public void commit() {
        WebResource commitResource = client.resource(
                UriBuilder.fromPath(solrUrl).segment("update").build());
        ClientResponse response = commitResource
                .queryParam("commit", "true")
                .queryParam("optimize", "true")
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class);
        try {
            if (Response.Status.OK.getStatusCode() != response.getStatus()) {
                throw new IndexException(
                        "Error with Solr commit: " + response.getEntity(String.class));
            }
        } finally {
            response.close();
        }
    }

    /**
     * Index some JSON data.
     *
     * @param ios      The input stream containing update JSON
     * @param doCommit Whether or not to commit the update
     */
    public void update(InputStream ios, boolean doCommit) {
        WebResource resource = client.resource(
                UriBuilder.fromPath(solrUrl).segment("update").build());
        ClientResponse response = resource
                .queryParam("commit", String.valueOf(doCommit))
                .type(MediaType.APPLICATION_JSON)
                .entity(ios)
                .post(ClientResponse.class);
        try {
            if (Response.Status.OK.getStatusCode() != response.getStatus()) {
                throw new IndexException("Error with Solr upload: " + response.getEntity(String.class));
            }
        } finally {
            response.close();
        }
    }
}