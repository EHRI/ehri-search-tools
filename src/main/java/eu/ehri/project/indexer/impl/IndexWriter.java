package eu.ehri.project.indexer.impl;

import com.google.common.io.FileBackedOutputStream;
import eu.ehri.project.indexer.SolrIndexer;
import eu.ehri.project.indexer.Writer;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IndexWriter implements Writer<JsonNode> {

    private final String solrUrl;
    private final FileBackedOutputStream out;
    private final OutputStreamWriter npw;

    public IndexWriter(String solrUrl) {
        this.solrUrl = solrUrl;
        this.out = new FileBackedOutputStream(1024 * 1024);
        this.npw = new OutputStreamWriter(out);
    }

    public void write(JsonNode node) {
        npw.write(node);
    }

    public void close() {
        npw.close();
        SolrIndexer indexer = new SolrIndexer(solrUrl);
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
