package eu.ehri.project.indexer.sink.impl;

import com.google.common.io.FileBackedOutputStream;
import eu.ehri.project.indexer.index.Index;
import eu.ehri.project.indexer.sink.Sink;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IndexingSink implements Sink<JsonNode> {

    private final Index index;
    private final FileBackedOutputStream out;
    private final OutputStreamSink npw;

    public IndexingSink(Index index) {
        this.index = index;
        this.out = new FileBackedOutputStream(1024 * 1024);
        this.npw = new OutputStreamSink(out);
    }

    public void write(JsonNode node) {
        npw.write(node);
    }

    public void close() {
        npw.close();
        try {
            index.update(out.getSupplier().getInput(), true);
        } catch (IOException e) {
            throw new RuntimeException("Error updating Solr: ", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing temp stream for index: ", e);
            }
        }
    }
}
