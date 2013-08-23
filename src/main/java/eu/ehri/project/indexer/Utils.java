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
        Properties pathProperties = new Properties();
        InputStream pathIs = Utils.class.getClassLoader().getResourceAsStream("paths.properties");
        if (pathIs == null)
            throw new RuntimeException("Cannot load paths.properties resource");
        try {
            pathProperties.load(pathIs);
        } catch (IOException e) {
            throw new RuntimeException("Invalid paths.properties file", e);
        }


        ImmutableMap.Builder<String, JsonPath> pathBuilder = ImmutableMap.builder();
        for (String pathKey : pathProperties.stringPropertyNames()) {
            pathBuilder.put(pathKey, JsonPath.compile("$." + pathProperties.getProperty(pathKey)));
        }
        return pathBuilder.build();
    }

    public static Map<String,List<String>> loadTypeKeys() {
        Properties typeProperties = new Properties();
        InputStream typeIs = Utils.class.getClassLoader().getResourceAsStream("types.properties");
        if (typeIs == null)
            throw new RuntimeException("Cannot load types.properties resource");
        try {
            typeProperties.load(typeIs);
        } catch (IOException e) {
            throw new RuntimeException("Invalid types.properties file", e);
        }
        ImmutableMap.Builder<String, List<String>> typeBuilder = ImmutableMap.builder();
        Splitter splitter = Splitter.on(",");
        for (String typeKey : typeProperties.stringPropertyNames()) {
            String commaSepKeys = typeProperties.getProperty(typeKey);
            Iterable<String> keys = splitter.split(commaSepKeys);
            typeBuilder.put(typeKey, Lists.newArrayList(keys));
        }
        return typeBuilder.build();
    }
}
