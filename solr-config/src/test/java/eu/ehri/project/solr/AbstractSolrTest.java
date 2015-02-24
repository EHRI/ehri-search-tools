package eu.ehri.project.solr;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.BeforeClass;

import java.io.File;
import java.net.URL;

/**
 * Base class for EHRI search engine tests. Currently
 * this assumes that the data does not change between tests,
 * i.e. that the index content is static.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public abstract class AbstractSolrTest extends SolrTestCaseJ4 {

    public static final String SOLRHOME = "solr";
    public static final String CORENAME = "portal";

    private static String getSolrHome() {
        return new File(System.getProperty("basedir"), SOLRHOME).getPath();
    }

    private static String getSolrConfigPath(String name) {
        String home = new File(getSolrHome(), CORENAME).getPath();
        String conf = new File(home, "conf").getPath();
        return new File(conf, name).getPath();
    }

    public static String getSolrConfigFile() {
        return getSolrConfigPath("solrconfig.xml");
    }

    public static String getSchemaFile() {
        return getSolrConfigPath("schema.xml");
    }

    /**
     * The standard EHRI query params. Items are grouped by itemId because
     * there may be multiple documents representing descriptions for the
     * same documentary unit.
     * @return A set of Solr Params.
     */
    public static ModifiableSolrParams templateQueryParams() {
        return params(
            "defType", "edismax",
            // group params
            "group", "true",
            "group.limit", "1",
            "group.ngroups", "true",
            "group.cache.percent", "0",
            "group.field", "itemId",
            "group.format", "simple",
            "group.offset", "0",
            "group.facet", "true",
            // paging
            "start", "0",
            "rows", "10",
            // format as json
            "wt", "json",
            // facets
            "facet", "true",
            "facet.minCount", "1",
            // query fields
            "qf", "itemId^15 identifier^10 name^8 title^8 otherFormsOfName^8 " +
                "parallelFormsOfName^8 altName^10 " +
                "name_sort text",
            // spellcheck
            "spellcheck", "true",
            "spellcheck.count", "10",
            "spellcheck.extendedResults", "true",
            "spellcheck.accuracy", "0.6",
            "spellcheck.collate", "true",
            "spellcheck.maxCollations", "10",
            "spellcheck.maxCollationTries", "10"
        );
    }

    public static ModifiableSolrParams queryParams(String query, String... otherParams) {
        Preconditions.checkArgument(otherParams.length % 2 == 0,
                "Invalid number of parameters given. Should be a list of key/value pairs.");
        ModifiableSolrParams basic = templateQueryParams();
        basic.set("q", query);
        for (int i = 0; i < otherParams.length; i += 2) {
            basic.set(otherParams[i], otherParams[i+1]);
        }
        System.out.println(basic.toString());
        return basic;
    }

    public static String runSearch(String q, String... otherParams) throws Exception {
        return JQ(req(queryParams(q, otherParams)));
    }

    @BeforeClass
    public static void setupCore() throws Exception {
        System.setProperty("solr.allow.unsafe", "true");
        System.setProperty("solr.allow.unsafe.resourceloading", "true");
        initCore(
                getSolrConfigPath("solrconfig.xml"),
                getSolrConfigPath("schema.xml"),
                getSolrHome(),
                CORENAME
        );

        // Load our data...
        URL url = Resources.getResource("searchdata.json");
        updateJ(Resources.toString(url, Charsets.UTF_8), params("commit", "true"));
    }
}
