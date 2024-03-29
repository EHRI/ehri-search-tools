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


public class JsonConverterTest {

    private static final List<String> inputResources = ImmutableList.of(
            "inputdoc1.json", "inputdoc2.json", "inputdoc3.json", "inputdoc4.json", "inputdoc5.json",
            "inputdoc1.json", "inputdoc2.json", "inputdoc6.json", "inputdoc7.json", "inputdoc8.json"
    );

    private static final List<Integer> expectedNodeCount = ImmutableList.of(1, 2, 1, 1, 1, 1, 2, 1, 1, 1);

    private static final ImmutableList<ImmutableMap<String, Object>> expected = ImmutableList.of(
            ImmutableMap.of(
                    "id", "eb747649-4f7b-4874-98cf-f236d2b5fa1d",
                    "itemId", "003348-wl1729",
                    "type", "DocumentaryUnit",
                    "name", "Herta Berg: family recipe note books",
                    "isParent", false
            ),
            ImmutableMap.of(
                    "id", "be-002112-ca-eng",
                    "itemId", "be-002112-ca",
                    "type", "DocumentaryUnit",
                    "otherFormsOfName", Lists.newArrayList("CEGESOMA Photographic Archives"),
                    "isParent", true
            ),
            ImmutableMap.of(
                    "id", "mike",
                    "itemId", "mike",
                    "type", "UserProfile",
                    "name", "Mike",
                    "isParent", false
            ),
            ImmutableMap.of(
                    "id", "380f80b0-7490-11e4-813b-a3ef93d0d496",
                    "annotatorId", "mike",
                    "annotatorName", "Mike",
                    "type", "Annotation"
            ),
            ImmutableMap.of(
                    "id", "hierarchy-test",
                    "parentId", "hierarchy-test-p1",
                    "isTopLevel", false,
                    "ancestorIds", Lists.newArrayList("hierarchy-test-p1", "hierarchy-test-p2", "hierarchy-test-p3")
            ),
            ImmutableMap.of(
                    "subjects", Lists.newArrayList("Refugees", "Emigration", "Persecution", "Jewish", "Holocaust",
                            "Migration", "Nazism", "Jews"),
                    "places", Lists.newArrayList("London", "Vienna - Austria"),
                    "people", Lists.newArrayList("Berg, Susanne", "Berg, Gustav", "Berg, Herta. née Bass")
            ),
            ImmutableMap.of(
                    // Language of materials, an array value in multiple descriptions means
                    // we'll get all values of all descriptions, which probably means duplication.
                    // However, I think we can live with this.
                    "languageOfMaterial", Lists.newArrayList("eng", "fre", "eng", "fre")
            ),
            ImmutableMap.of(
                    "holderName", "Ehri Corporate Bodies"
            ),
            ImmutableMap.<String, Object>of(
                    "linkType", Lists.newArrayList("associative")
            ),
            ImmutableMap.of(
                    "id", "ar",
                    "name", "Argentina",
                    "parallelFormsOfName", Lists.newArrayList("Argentinien","Argentine","Argentina","Argentina","Argentinië","Argentyna","Argentina","Argentína","Аргентина","Аргентина","ארגנטינה","Argentina"),
                    "type", "Country"
            )
    );

    private final List<JsonNode> inputs = Lists.newArrayList();

    @Before
    public void setUp() throws Exception {
        for (String testResource : inputResources) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(testResource);
                 Source<JsonNode> source = new InputStreamJsonSource(stream)) {
                inputs.add(Iterables.get(source.iterable(), 0));
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
            // System.out.println(out.toString());
            JsonAsserter asserter = with(out.toString());
            for (Map.Entry<String, Object> entry : expected.get(i).entrySet()) {
                asserter.assertThat("$." + entry.getKey(), equalTo(entry.getValue()), "Doc " + (i + 1) + " incorrect");
            }
        }
    }
}
