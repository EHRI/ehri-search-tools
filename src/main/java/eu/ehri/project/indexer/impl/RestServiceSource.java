package eu.ehri.project.indexer.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import eu.ehri.project.indexer.CloseableIterable;
import eu.ehri.project.indexer.impl.JsonConverter;
import eu.ehri.project.indexer.Writer;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class RestServiceSource implements CloseableIterable<JsonNode> {

    public static final String URL = "http://localhost:7474/ehri";

    private final Client client = Client.create();
    private List<ServiceSource> readers = Lists.newArrayList();

    public RestServiceSource(String... specs) {
        List<String> ids = Lists.newArrayList();
        for (String spec : specs) {
            if (spec.contains("|")) {
                Iterable<String> split = Splitter.on("|").limit(2).split(spec);
                String type = Iterables.get(split, 0);
                String id = Iterables.get(split, 1);
                readers.add(new ChildItemSource(client, type, id));
            } else if (spec.startsWith("@")) {
                ids.add(spec.substring(1));
            } else {
                readers.add(new TypeSource(client, spec));
            }
        }
        readers.add(new IdSetSource(client, ids.toArray(new String[ids.size()])));
    }

    @Override
    public void close() {
        for (ServiceSource reader : readers) {
            reader.close();
        }
    }

    @Override
    public Iterator<JsonNode> iterator() {
        return Iterables.concat(readers.toArray(new ServiceSource[readers.size()])).iterator();
    }
}
