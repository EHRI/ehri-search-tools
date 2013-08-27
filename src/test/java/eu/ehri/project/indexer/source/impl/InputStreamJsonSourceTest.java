package eu.ehri.project.indexer.source.impl;

import com.google.common.collect.Iterables;
import eu.ehri.project.indexer.source.Source;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class InputStreamJsonSourceTest {

    private static String testResource = "inputdoc.json";

    @Test
    public void testDocContainsOneNode() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(testResource);
        Source<JsonNode> source = new InputStreamJsonSource(stream);
        try {
            assertEquals(1, Iterables.size(source));
        } finally {
            source.finish();
            stream.close();
        }
    }
}
