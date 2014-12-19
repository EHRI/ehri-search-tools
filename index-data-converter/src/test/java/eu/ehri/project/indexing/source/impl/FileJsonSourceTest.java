package eu.ehri.project.indexing.source.impl;

import com.google.common.collect.Iterables;
import eu.ehri.project.indexing.source.Source;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class FileJsonSourceTest {

    public static String testResource = "inputdoc1.json";

    private String getResourcePath() throws Exception {
        URL resource = getClass().getClassLoader().getResource(testResource);
        assert resource != null;
        return new File(resource.toURI()).getAbsolutePath();
    }

    @Test
    public void testGetIterable() throws Exception {
        FileJsonSource source = new FileJsonSource(getResourcePath());
        try {
            assertEquals(1, Iterables.size(source.getIterable()));
        } finally {
            source.finish();
        }
    }

    @Test(expected = Source.SourceException.class)
    public void testGetIterableWithBadSource() throws Exception {
        FileJsonSource source = new FileJsonSource("DOES_NOT_EXIST");
        assertEquals(1, Iterables.size(source.getIterable()));
    }
}
