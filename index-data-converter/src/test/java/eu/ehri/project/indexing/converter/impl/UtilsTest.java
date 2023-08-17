package eu.ehri.project.indexing.converter.impl;

import com.jayway.jsonpath.JsonPath;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void loadPaths() {
        final Map<String, List<JsonPath>> paths = Utils.loadPaths();
        assertFalse(paths.isEmpty());
    }
}