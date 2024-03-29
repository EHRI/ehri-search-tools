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
 */
public abstract class AbstractSolrTest extends SolrTestCaseJ4 {

    public static final String CORENAME = "core";

    private static String getSolrHome() {
        return new File(System.getProperty("basedir")).getPath();
    }

    private static String getSolrConfigPath(String name) {
        String home = new File(getSolrHome()).getPath();
        String core = new File(home, CORENAME).getPath();
        String conf = new File(core, "conf").getPath();
        return new File(conf, name).getPath();
    }

    public static String getSolrConfigFile() {
        return getSolrConfigPath("solrconfig.xml");
    }

    public static String getSchemaFile() {
        return getSolrConfigPath("schema.xml");
    }

    public static final String standardFields =
            "itemId^15 identifier^10 name^8 title^8 otherFormsOfName^8 parallelFormsOfName^8 altName^10 name_sort text ";
    public static final String multiLingualFields =
            "txt_bg txt_cs txt_da txt_en txt_de txt_el txt_fi txt_fr txt_hu txt_it txt_lv txt_no txt_pl txt_ro txt_ru";

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
            "qf", standardFields + multiLingualFields,
            // spellcheck
            "spellcheck", "true",
            "spellcheck.count", "10",
            "spellcheck.extendedResults", "true",
            "spellcheck.accuracy", "0.6",
            "spellcheck.collate", "true",
            "spellcheck.maxCollations", "10",
            "spellcheck.maxCollationTries", "10",
            // minimum match, simulating AND for 3 or fewer terms
            "mm", "3<90%",
            "mm.autoRelax", "true"
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
        System.out.println(basic);
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
