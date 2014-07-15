package eu.ehri.project.search;

import org.junit.Test;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for spell check functionality.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SpellSuggestionTest extends AbstractSolrTest {

    @Test
    public void testSpellcheck() throws Exception {
        String result = runSearch("arcchives", "rows", "0");
        //System.out.println(result);
        with(result)
                .assertThat("$.grouped.itemId.matches", equalTo(0))
                .assertThat("$.spellcheck.suggestions[0]", equalTo("arcchives"))
                .assertThat("$.spellcheck.suggestions[1].numFound", equalTo(3))
                .assertThat("$.spellcheck.suggestions[1].suggestion[0].word", equalTo("archives"));

    }
}
