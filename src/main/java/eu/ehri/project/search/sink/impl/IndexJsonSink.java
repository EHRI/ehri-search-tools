package eu.ehri.project.search.sink.impl;

import com.google.common.io.FileBackedOutputStream;
import eu.ehri.project.search.index.Index;
import eu.ehri.project.search.sink.Sink;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IndexJsonSink implements Sink<JsonNode> {

    private final Index index;
    private final FileBackedOutputStream out;
    private final OutputStreamJsonSink npw;
    private int writeCount = 0;

    public IndexJsonSink(Index index) {
        this.index = index;
        this.out = new FileBackedOutputStream(1024 * 1024);
        this.npw = new OutputStreamJsonSink(out);
    }

    public void write(JsonNode node) throws SinkException {
        writeCount++;
        npw.write(node);
    }

    public void finish() throws SinkException {
        npw.finish();
        try {
            if (writeCount > 0) {
                index.update(out.getSupplier().getInput(), true);
                writeCount = 0;
            }
        } catch (Exception e) {
            throw new SinkException("Error updating Solr", e);
        }
        try {
            out.reset();
        } catch (IOException e) {
            throw new RuntimeException("Error closing temp stream for index: ", e);
        }
    }
}
