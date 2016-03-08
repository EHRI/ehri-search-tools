package eu.ehri.project.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IndexHelperTest {

    // Reassign System.out/err/in to test the cmd-line output.
    // Store the original one here.
    private PrintStream _out;
    private PrintStream _err;
    private InputStream _in;
    private ByteArrayOutputStream out;

    @Before
    public void setUp() {
        _out = System.out;
        _err = System.err;
        _in = System.in;
        out = new ByteArrayOutputStream();
        System.setIn(getClass().getClassLoader()
                .getResourceAsStream("inputdoc1.json"));
        System.setOut(new PrintStream(out));
    }

    @After
    public void tearDown() {
        System.setOut(_out);
        System.setErr(_err);
        System.setIn(_in);
    }

    @Test
    public void testUrlsFromSpecs() throws Exception {
        String base = IndexHelper.DEFAULT_EHRI_URL;

        // Item classes, where the classes are "foo" and "bar"
        assertEquals(new URI(base + "/classes/foo?limit=-1"),
                IndexHelper.urlsFromSpecs(base, "foo", "bar").get(0));
        assertEquals(new URI(base + "/classes/bar?limit=-1"),
                IndexHelper.urlsFromSpecs(base, "foo", "bar").get(1));

        // Single items, where the item IDs are "foo" and "bar'
        assertEquals(new URI(base + "/entities?id=foo&id=bar&limit=-1"),
                IndexHelper.urlsFromSpecs(base, "@foo", "@bar").get(0));

        // An item tree, where the type is "foo" and the ID is "bar"
        assertEquals(new URI(base + "/classes/foo/bar/list?limit=-1&all=true"),
                IndexHelper.urlsFromSpecs(base, "foo|bar").get(0));
    }

    @Test
    public void testInputStream() throws Exception {
        IndexHelper.main(new String[]{"--file", "-", "--pretty"});

        JsonNode node = (new ObjectMapper()).readTree(out.toByteArray());
        assertEquals("Herta Berg: family recipe note books",
                node.path(0).path("name").asText());
    }

    @Test
    public void testInputStreamWithNoConversion() throws Exception {
        IndexHelper.main(new String[]{"--file", "-", "--pretty", "--noconvert"});

        JsonNode node = (new ObjectMapper()).readTree(out.toByteArray());
        assertEquals("Herta Berg: family recipe note books", node
                .path(0).path("relationships").path("describes")
                .path(0).path("data").path("name").asText());
    }

    @Test
    public void testInputStreamWithStatsAndVerbose() throws Exception {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        IndexHelper.main(new String[]{"--file", "-", "--stats", "--verbose"});

        JsonNode node = (new ObjectMapper()).readTree(out.toByteArray());
        List<String> info = Lists.newArrayList(Splitter.on("\n")
                .omitEmptyStrings().split(err.toString()));

        // One item converted, plus three lines stats
        assertEquals(4, info.size());
        assertEquals("DocumentaryUnit -> eb747649-4f7b-4874-98cf-f236d2b5fa1d",
                info.get(0));
        assertEquals(info.get(2), "Items indexed: " + node.size());
    }
}