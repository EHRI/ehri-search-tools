package eu.ehri.project.search;

import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import static com.jayway.jsonassert.JsonAssert.*;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for query functionality.
 *
 * Details on JSON path matching here:
 *
 *   http://code.google.com/p/json-path/
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class BasicSearchTest extends AbstractSolrTest {

    /**
     * Test the total number of docs in the index is 27.
     * (Adjust count as necessary.)
     */
    @Test
    public void testNumResults() throws Exception {
        with(runSearch("*")).assertThat("$.grouped.itemId.matches", equalTo(27));
    }

    @Test
    public void testBasicSearch() throws Exception {
        String data = runSearch("data");
        //System.out.println(JsonPath.read(data, "$.grouped.itemId.matches"));
        with(data).assertThat("$.grouped.itemId.matches", equalTo(3));
    }
}
