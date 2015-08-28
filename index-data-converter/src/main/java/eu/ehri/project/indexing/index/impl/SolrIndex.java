package eu.ehri.project.indexing.index.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import eu.ehri.project.indexing.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

/**
 * Index implementation for an Apache Solr instance. NB: We use
 * the (non-default) Apache client instance here to avoid problems
 * with the default implementation's handling of chunked POST data
 * (which is necessary because we're streaming large files to Solr.)
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SolrIndex implements Index {

    private static final Logger logger = LoggerFactory.getLogger(SolrIndex.class);
    private static final Client client = ApacheHttpClient.create(getClientConfig());
    private static final JsonFactory jsonFactory = new JsonFactory();

    private final String url;

    /**
     * Constructor.
     *
     * @param url The URL of the Solr instance.
     */
    public SolrIndex(String url) {
        this.url = url;
    }

    @Override
    public void commit() {
        logger.debug("Committing solr index...");
        WebResource commitResource = client.resource(
                UriBuilder.fromPath(url).segment("update").build());
        ClientResponse response = commitResource
                .queryParam("commit", "true")
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

    @Override
    public void deleteAll(boolean commit) throws IndexException {
        deleteByQuery("id:*", commit);
    }

    @Override
    public void deleteItem(String id, boolean commit) throws IndexException {
        deleteByQuery(idMatchQuery(id), commit);
    }

    @Override
    public void deleteByFieldValue(String field, String value, boolean commit) throws IndexException {
        deleteByQuery(keyValueQuery(field, value), commit);
    }

    @Override
    public void deleteItems(List<String> ids, boolean commit) throws IndexException {
        List<String> queries = Lists.newArrayList();
        for (String id : ids) {
            queries.add(idMatchQuery(id));
        }
        deleteByQueryList(queries, commit);
    }

    @Override
    public void deleteType(String type, boolean commit) throws IndexException {
        deleteByQuery("type:" + type, commit);
    }

    @Override
    public void deleteTypes(List<String> types, boolean commit) throws IndexException {
        List<String> queries = Lists.newArrayList();
        for (String type : types) {
            queries.add("type:" + type);
        }
        deleteByQueryList(queries, commit);
    }

    @Override
    public void update(InputStream ios, boolean doCommit) {
        WebResource resource = client.resource(
                UriBuilder.fromPath(url).segment("update").build());
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

    // Helpers

    private static ClientConfig getClientConfig() {
        // Get the default client configuration. In order to prevent
        // OutOfMemory Heap-space errors when POSTing large files to Solr
        // we have to enable chunked encoding, which is done by setting the
        // PROPERTY_CHUNKED_ENCODING_SIZE to a non-null value, with 0 indicating
        // the default value.
        ClientConfig config = new DefaultClientConfig();
        config.getProperties().put(
                DefaultApacheHttpClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, 0);
        return config;
    }

    private String keyValueQuery(String field, String value) {
        // Generate a Solr query matching all items with the given field value,
        // i.e. "heldBy:us-005248"
        return String.format("%s:\"%s\"", field, value);
    }

    private String idMatchQuery(String id) {
        // Generate a Solr query matching EITHER the id or the itemId. This is
        // because item's in the EHRI index are always grouped by the item id
        // and so subject to deletion if the itemId is given.
        return String.format("id:\"%s\" OR itemId:\"%s\"", id, id);
    }

    private void deleteByQuery(String query, boolean commit) throws IndexException {
        deleteByQueryList(Lists.newArrayList(query), commit);
    }

    private void deleteByQueryList(List<String> queries, boolean commit) throws IndexException {
        // Generate an update statement containing one or more deleteByQuery queries.
        // Note that there can be several deleteByQuery queries in the update object,
        // which is valid JSON, see:
        // http://wiki.apache.org/solr/UpdateJSON
        // See Solr update syntax with duplicate object keys:
        StringWriter stringWriter = new StringWriter();
        try {
            try (JsonGenerator g = jsonFactory.createGenerator(stringWriter)) {
                g.writeStartObject();
                for (String query : queries) {
                    g.writeFieldName("delete");
                    g.writeStartObject();
                    g.writeObjectField("query", query);
                    g.writeEndObject();
                }
                g.writeEndObject();
                g.flush();

                stringWriter.flush();
                String str = stringWriter.toString();
                try (InputStream stream = new ByteArrayInputStream(
                        str.getBytes("UTF-8"))) {
                    update(stream, commit);
                }
            } catch (IOException e) {
                throw new IndexException("Error creating delete payload", e);
            } finally {
                stringWriter.close();
            }
        } catch (IOException e) {
            throw new IndexException("Error creating delete payload: " + queries, e);
        }
    }
}
