package eu.ehri.project.indexer.source.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import eu.ehri.project.indexer.source.Source;
import org.codehaus.jackson.JsonNode;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class RestServiceSource implements Source<JsonNode> {

    private final List<WebSource> readers = Lists.newArrayList();

    public RestServiceSource(String serviceUrl, String... specs) {
        Client client = Client.create();
        for (URI uri : urlsFromSpecs(serviceUrl, specs)) {
            readers.add(new WebSource(client, uri.toString()));
        }
    }

    @Override
    public void finish() {
        for (WebSource reader : readers) {
            reader.finish();
        }
    }

    @Override
    public Iterator<JsonNode> iterator() {
        return Iterables.concat(
                readers.toArray(new WebSource[readers.size()])).iterator();
    }

    /**
     * Turn a list of specs into a set of EHRI REST URLs to download
     * JSON lists from.
     *
     * This is gross and subject to change.
     *
     * @param serviceUrl    The base REST URL
     * @param specs         A list of specs
     * @return              A list of URLs
     */
    private List<URI> urlsFromSpecs(String serviceUrl, String... specs) {
        List<URI> urls = Lists.newArrayList();
        List<String> ids = Lists.newArrayList();
        for (String spec : specs) {
            // Item type and id - denotes fetching child items (?)
            if (spec.contains("|")) {
                Iterable<String> split = Splitter.on("|").limit(2).split(spec);
                String type = Iterables.get(split, 0);
                String id = Iterables.get(split, 1);
                URI url = UriBuilder.fromPath(serviceUrl)
                        .segment(type).segment(id).segment("list")
                        .queryParam("limit", "-1").build();
                urls.add(url);
            } else if (spec.startsWith("@")) {
                ids.add(spec.substring(1));
            } else {
                URI url = UriBuilder.fromPath(serviceUrl)
                        .segment(spec).segment("list")
                        .queryParam("limit", "-1").build();
                urls.add(url);
            }
        }

        // Unlike types or children, multiple ids are done in one request.
        UriBuilder idBuilder = UriBuilder.fromPath(serviceUrl).segment("entities");
        for (String id : ids) {
            idBuilder = idBuilder.queryParam("id", id);
        }
        urls.add(idBuilder.queryParam("limit", "-1").build());
        return urls;
    }
}
