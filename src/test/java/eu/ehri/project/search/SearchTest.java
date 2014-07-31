package eu.ehri.project.search;

import com.jayway.jsonpath.JsonPath;
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
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
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
        System.out.println(JsonPath.read(data, "$.grouped.itemId.matches"));
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
}
