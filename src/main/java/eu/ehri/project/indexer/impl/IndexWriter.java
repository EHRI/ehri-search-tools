package eu.ehri.project.indexer.impl;

import com.google.common.io.FileBackedOutputStream;
import eu.ehri.project.indexer.SolrIndexer;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IndexWriter {

    private final FileBackedOutputStream out;
    private NodePrintWriter npw;


    public IndexWriter() {
        this.out = new FileBackedOutputStream(1024 * 1024);
        this.npw = new NodePrintWriter(out);
    }

    public void write(JsonNode node) {
        npw.write(node);
    }

    public void close() {
        npw.close();
        SolrIndexer indexer = new SolrIndexer("http://localhost:8983/solr/portal");
        try {
            indexer.update(out.getSupplier().getInput(), true);
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
