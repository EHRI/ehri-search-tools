package eu.ehri.project.solr;

import com.jayway.jsonpath.JsonPath;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Test;
import static com.jayway.jsonassert.JsonAssert.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Test for query functionality.
 *
 * Details on JSON path matching here:
 *
 *   http://code.google.com/p/json-path/
 */
@SolrTestCaseJ4.SuppressSSL
public class SearchTest extends AbstractSolrTest {

    /**
     * Test the total number of docs in the index is not 0
     * (Actual count will change a lot as we update the test data.)
     */
    @Test
    public void testNumResults() throws Exception {
        with(runSearch("*")).assertThat("$.grouped.itemId.matches", greaterThan(0));
    }

    @Test
    public void testBasicSearch() throws Exception {
        String data = runSearch("warsaw");
        System.out.println(JsonPath.read(data, "$.grouped.itemId.matches").toString());
        with(data).assertThat("$.grouped.itemId.matches", equalTo(132));
    }

    @Test
    public void testSpellcheck() throws Exception {
        String result = runSearch("arcchives", "rows", "0");
        //System.out.println(result);
        with(result)
                .assertThat("$.grouped.itemId.matches", equalTo(0))
                .assertThat("$.spellcheck.suggestions[0]", equalTo("arcchives"))
                .assertThat("$.spellcheck.suggestions[1].numFound", equalTo(10))
                .assertThat("$.spellcheck.suggestions[1].suggestion[0].word", equalTo("archives"));

    }

    @Test
    public void testSpellcheck2() throws Exception {
        String result = runSearch("warsav", "rows", "0");
        //System.out.println(result);
        with(result)
                .assertThat("$.grouped.itemId.matches", equalTo(0))
                .assertThat("$.spellcheck.suggestions[0]", equalTo("warsav"))
                .assertThat("$.spellcheck.suggestions[1].numFound", equalTo(3))
                .assertThat("$.spellcheck.suggestions[1].suggestion[0].word", equalTo("warsaw"));

    }

    @Test
    public void testFindingExactMatchAltNameInStopwords() throws Exception {
        // This is a test for a specific problem: we have a generic stop word
        // list, one of whose words in "dans". This is also the acronym of a
        // repository. Because of this a case-insensitive altName field was
        // introduced to match alternative names such as acronyms exactly,
        // without stop word filtering.
        String result = runSearch("dans", "fq", "type:Repository");
        //System.out.println(result);
        with(result)
                .assertThat("$.grouped.itemId.matches", equalTo(1));
        assertTrue(result.contains("Data Archiving and Networked Services")); // DANS
    }

    @Test
    public void testExactIdMatch() throws Exception {
        // Test that we can find exact match ids and that the item we
        // want is the first search result
        with(runSearch("ua-003307-p-1265"))
                .assertThat("$.grouped.itemId.doclist.docs[0].itemId",
                        equalTo("ua-003307-p-1265"));
    }

    @Test
    public void testCaseInsensitiveLocalIdentifierMatch() throws Exception {
        // Test that:
        //  - we can find things with case-insensitive local identifiers
        //  - the boost pushes matches to the top
        with(runSearch("p-1265"))
                .assertThat("$.grouped.itemId.doclist.docs[0].itemId",
                        equalTo("ua-003307-p-1265"));
    }

    @Test
    public void testSearchForHansFrank() throws Exception {
        // NB: Hans is a stop-word
        String json = runSearch("\"hans frank\"");
        //System.out.println(json);
        with(json)
                .assertThat("$.grouped.itemId.matches", greaterThan(2));
    }

    @Test
    public void testFacetCaseInsensitiveExactMatch() throws Exception {
        // Filtering on the facet (_f) field won't match because it's case-sensitive
        String json = runSearch("*", "fq", "accessPoints_facet:ESTONIA", "facet.field", "accessPoints_facet");
        with(json)
                .assertThat("$.grouped.itemId.matches", equalTo(0));
        // But it will match with the standard case-insentitive field.
        String json2 = runSearch("*", "fq", "accessPoints:ESTONIA", "facet.field", "accessPoints_facet");
        with(json2)
                .assertThat("$.grouped.itemId.matches", equalTo(18));
    }

    @Test
    public void testFacetOnIntValues() throws Exception {
        // Test for SOLR-7495 bug which breaks faceting on non-multi-valued
        // int fields. As a workaround the fields where made multi-valued.
        // https://issues.apache.org/jira/browse/SOLR-7495
        // Update: Fixed in Solr 6.4.0
        String json = runSearch("*", "fq", "priority:5", "facet.field", "priority", "rows", "0");
        //System.out.println(json);
        with(json)
                .assertThat("$.grouped.itemId.matches", equalTo(221));

        String json2 = runSearch("*", "fq", "promotionScore:1", "facet.field", "promotionScore", "rows", "0");
        with(json2)
                .assertThat("$.grouped.itemId.matches", equalTo(0));
    }

    @Test
    public void testMixedTextAndNumbers() throws Exception {
        // Test for searching for item's with a mixed text/numerical component
        // in the title. This doesn't work correctly unless 'splitOnNumerics' is false
        // in the WordDelimiterFilterFactory
        String json = runSearch("sonderkommando 4b", "fq", "type:HistoricalAgent", "rows", "1");
        with(json)
                .assertThat("$.grouped.itemId.doclist.docs[0].itemId",
                        equalTo("ehri-cb-556"));
    }

    @Test
    public void testMixedNumbersAndPunctuation() throws Exception {
        // Test for searching for item's with a numerical/punctuation components
        // in the title. This doesn't work correctly unless 'generateNumberParts' is true
        // in the WordDelimiterFilterFactory
        String json = runSearch("1923-2000", "fq", "type:DocumentaryUnit");
        System.out.println(json);
        with(json)
                .assertThat("$.grouped.itemId.doclist.docs[0].itemId",
                        equalTo("lu-002885-af-ae-aw"));
    }
}
