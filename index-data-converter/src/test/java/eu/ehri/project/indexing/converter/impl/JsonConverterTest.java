package eu.ehri.project.indexing.converter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jayway.jsonassert.JsonAsserter;
import eu.ehri.project.indexing.source.Source;
import eu.ehri.project.indexing.source.impl.InputStreamJsonSource;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class JsonConverterTest {

    private static final List<String> inputResources = ImmutableList.of(
            "inputdoc1.json", "inputdoc2.json", "inputdoc3.json"
    );

    private static final List<Integer> expectedNodeCount = ImmutableList.of(1, 2, 1);

    private static final ImmutableList<ImmutableMap<String,Object>> expected = ImmutableList.of(
            ImmutableMap.<String,Object>of(
                    "id", "eb747649-4f7b-4874-98cf-f236d2b5fa1d",
                    "itemId", "003348-wl1729",
                    "type", "documentaryUnit",
                    "name", "Herta Berg: family recipe note books",
                    "isParent", false
            ),
            ImmutableMap.<String,Object>of(
                    "id", "be-002112-ca-eng",
                    "itemId", "be-002112-ca",
                    "type", "documentaryUnit",
                    "name", "Photographic archives",
                    "isParent", true
            ),
            ImmutableMap.<String,Object>of(
                    "id", "mike",
                    "itemId", "mike",
                    "type", "userProfile",
                    "name", "Mike",
                    "isParent", false
            )
    );

    private List<JsonNode> inputs = Lists.newArrayList();

    @Before
    public void setUp() throws Exception {
        for (String testResource : inputResources) {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(testResource);
            Source<JsonNode> source = new InputStreamJsonSource(stream);
            try {
                inputs.add(Iterables.get(source.getIterable(), 0));
            } finally {
                source.finish();
                stream.close();
            }
        }
    }

    @Test
    public void testCorrectNumberOfOutputNodes() throws Exception {
        for (int i = 0; i < expectedNodeCount.size(); i++) {
            assertEquals(expectedNodeCount.get(i),
                    Integer.valueOf(Iterables.size(new JsonConverter().convert(inputs.get(i)))));
        }
    }

    @Test
    public void testOutputDocIsDifferent() throws Exception {
        for (int i = 0; i < expectedNodeCount.size(); i++) {
            JsonNode out = Iterables.get(new JsonConverter().convert(inputs.get(i)), 0);
            assertNotSame(out, inputs.get(i));
        }
    }

    @Test
    public void testOutputDoc1ContainsRightValues() throws Exception {
        for (int i = 0; i < expected.size(); i++) {
            JsonNode out = Iterables.get(new JsonConverter().convert(inputs.get(i)), 0);
            JsonAsserter asserter = with(out.toString());
            for (Map.Entry<String,Object> entry : expected.get(i).entrySet()) {
                asserter.assertThat("$." + entry.getKey(), equalTo(entry.getValue()), "Doc " + i + " incorrect");
            }
        }
    }
}
