package eu.ehri.project.indexer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolrDocument {
    private String id;
    private String type;

    public SolrDocument() {

    }

    public SolrDocument(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
