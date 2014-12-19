package eu.ehri.project.indexing.source.impl;

import com.google.common.collect.Lists;
import eu.ehri.project.indexing.source.Source;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tes the InputStreamJsonSource behaves properly.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class InputStreamJsonSourceTest {

    @Test
    public void testDocContainsOneNode() throws Exception {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("inputdoc1.json");
        assertEquals(1, listFromStream(stream).size());
    }

    @Test
    public void testEmptyDoc() throws Exception {
        InputStream stream = new ByteArrayInputStream("[]".getBytes());
        assertEquals(0, listFromStream(stream).size());
    }

    // NB: There are two failure modes for a bad JSON stream, which
    // is decidedly less than perfect. In the first instance the
    // stream is either unreadable, or does not initiate as a list.
    // In this case we throw a SourceException immediately.
    //
    // In the second case, the stream opens okay, but when it
    // is iterated an item is malformed. In this case the parser
    // throws a RuntimeError "Unexpected character..."
    //
    // This is an unfortunate side-affect of laziness and I'm not
    // sure how we can fix it.

    @Test(expected = Source.SourceException.class)
    public void testBadJson() throws Exception {
        InputStream stream = new ByteArrayInputStream("bad".getBytes());
        assertEquals(0, listFromStream(stream).size());
    }

    @Test(expected = RuntimeException.class)
    public void testBadJsonStreamItem() throws Exception {
        InputStream stream = new ByteArrayInputStream("[bad".getBytes());
        assertEquals(0, listFromStream(stream).size());
    }

    private List<JsonNode> listFromStream(InputStream stream) throws Exception {
        Source<JsonNode> source = new InputStreamJsonSource(stream);
        try {
            return Lists.newArrayList(source.getIterable());
        } finally {
            source.finish();
            stream.close();
            assertTrue(source.isFinished());
        }
    }
}
