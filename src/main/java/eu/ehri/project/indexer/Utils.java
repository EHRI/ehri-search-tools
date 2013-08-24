package eu.ehri.project.indexer;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class Utils {

    public static Map<String,JsonPath> loadPaths() {
        Properties pathProperties = loadProperties("paths.properties");
        ImmutableMap.Builder<String, JsonPath> pathBuilder = ImmutableMap.builder();
        for (String pathKey : pathProperties.stringPropertyNames()) {
            // NB: Paths given in the properties file do not include the
            // leading '$.' JsonPath expects, so we add that.
            pathBuilder.put(pathKey, JsonPath.compile("$." + pathProperties.getProperty(pathKey)));
        }
        return pathBuilder.build();
    }

    public static Map<String,List<String>> loadTypeKeys() {
        Properties typeProperties = loadProperties("types.properties");
        ImmutableMap.Builder<String, List<String>> typeBuilder = ImmutableMap.builder();
        Splitter splitter = Splitter.on(",");
        for (String typeKey : typeProperties.stringPropertyNames()) {
            String commaSepKeys = typeProperties.getProperty(typeKey);
            Iterable<String> keys = splitter.split(commaSepKeys);
            typeBuilder.put(typeKey, Lists.newArrayList(keys));
        }
        return typeBuilder.build();
    }

    public static Properties loadProperties(String resourceName) {
        Properties properties = new Properties();
        InputStream pathIs = Utils.class.getClassLoader().getResourceAsStream(resourceName);
        if (pathIs == null)
            throw new RuntimeException("Cannot load resource: " + resourceName);
        try {
            properties.load(pathIs);
        } catch (IOException e) {
            throw new RuntimeException("Invalid properties file: " + resourceName, e);
        }
        return properties;
    }
}
