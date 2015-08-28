package eu.ehri.project.indexing;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IndexHelperTest {

    @Test
    public void testUrlsFromSpecs() throws Exception {
        String base = IndexHelper.DEFAULT_EHRI_URL;

        // Item classes, where the classes are "foo" and "bar"
        assertEquals(new URI(base + "/foo/list?limit=-1"),
                IndexHelper.urlsFromSpecs(base, "foo", "bar").get(0));
        assertEquals(new URI(base + "/bar/list?limit=-1"),
                IndexHelper.urlsFromSpecs(base, "foo", "bar").get(1));

        // Single items, where the item IDs are "foo" and "bar'
        assertEquals(new URI(base + "/entities?id=foo&id=bar&limit=-1"),
                IndexHelper.urlsFromSpecs(base, "@foo", "@bar").get(0));

        // An item tree, where the type is "foo" and the ID is "bar"
        assertEquals(new URI(base + "/foo/bar/list?limit=-1&all=true"),
                IndexHelper.urlsFromSpecs(base, "foo|bar").get(0));
    }
}