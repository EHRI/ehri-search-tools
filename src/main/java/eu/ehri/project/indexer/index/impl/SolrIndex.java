package eu.ehri.project.indexer.index.impl;

import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import eu.ehri.project.indexer.index.Index;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.util.List;

public class SolrIndex implements Index {
    /**
     * Fields.
     */

    private final String solrUrl;
    private static final Client client = Client.create();
    private static final JsonFactory jsonFactory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final static ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

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

    private void delete(String query, boolean commit) throws IndexException {
        List<String> queries = Lists.newArrayList();
        queries.add(query);
        delete(queries, commit);
    }

    private void delete(List<String> queries, boolean commit) throws IndexException {
        // FIXME: This function is a bit suspect.
        // See Solr update syntax with duplicate object keys:
        // http://wiki.apache.org/solr/UpdateJSON
        StringWriter stringWriter = new StringWriter();
        try {
            JsonGenerator g = jsonFactory.createJsonGenerator(stringWriter);
            try {
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
                InputStream stream = new ByteArrayInputStream(
                        str.getBytes("UTF-8"));
                try {
                    update(stream, commit);
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                throw new IndexException("Error creating delete payload", e);
            } finally {
                g.close();
                stringWriter.close();
            }
        } catch (IOException e) {
            throw new IndexException("Error creating delete payload: " + queries, e);
        }
    }

    @Override
    public void deleteAll(boolean doCommit) throws IndexException {
        delete("id:*", doCommit);
    }

    @Override
    public void deleteItem(String id, boolean doCommit) throws IndexException {
        delete(idMatchQuery(id), doCommit);
    }

    @Override
    public void deleteItems(List<String> ids, boolean doCommit) throws IndexException {
        List<String> queries = Lists.newArrayList();
        for (String id : ids) {
            queries.add(idMatchQuery(id));
        }
        delete(queries, doCommit);
    }

    private String idMatchQuery(String id) {
        return String.format("id:\"%s\" OR itemId:\"%s\"", id, id);
    }

    @Override
    public void deleteType(String type, boolean doCommit) throws IndexException {
        delete("type:"+ type, doCommit);
    }

    @Override
    public void deleteTypes(List<String> types, boolean doCommit) throws IndexException {
        List<String> queries = Lists.newArrayList();
        for (String type : types) {
            queries.add("type:"+ type);
        }
        delete(queries, doCommit);
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