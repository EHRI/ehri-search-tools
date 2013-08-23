package eu.ehri.project.indexer;

import com.google.common.collect.Maps;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonToken;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *
 * Convert from EHRI graph JSON to Solr documents.
 */
public class JsonConverter {

    /**
     * Set of key -> JsonPath extractors
     */
    private static final Map<String,JsonPath> jsonPaths = Utils.loadPaths();

    /**
     * Keys which have types that require special handling.
     */
    private static final Map<String,List<String>> types = Utils.loadTypeKeys();

    /**
     * Get data for items where most of it resides in the description
     * nodes.
     *
     * @param description
     * @param item
     * @return
     */
    public static Map<String,Object> getDescribedData(JsonNode description, JsonNode item) {
        Map<String,Object> descriptionData = getData(description);
        for (Map.Entry<String,Object> itemData : getData(item).entrySet()) {
            if (!itemData.getKey().equals("id")) {
                descriptionData.put(itemData.getKey(), itemData.getValue());
            }
        }
        return descriptionData;
    }


    /**
     * Get data for non-described items.
     * @param node
     * @return
     */
    public static Map<String,Object> getData(JsonNode node) {
        Map<String, Object> data = Maps.newHashMap();

        for (Map.Entry<String,JsonPath> attrPath : jsonPaths.entrySet()) {
            String attr = attrPath.getKey();
            JsonPath path = attrPath.getValue();

            try {
                data.put(attr, path.read(node.toString()));
            } catch (InvalidPathException e) {
            }
        }

        Iterator<Map.Entry<String,JsonNode>> dataFields = node.path("data").getFields();
        while (dataFields.hasNext()) {
            Map.Entry<String,JsonNode> field = dataFields.next();
            String key = field.getKey();
            if (!jsonPaths.containsKey(key)) {
                JsonToken value = field.getValue().asToken();
                switch (value) {
                    case VALUE_STRING: data.put(key + "_s", field.getValue().asText()); break;
                    case VALUE_NUMBER_INT: data.put(key + "_i", value.asString()); break;
                    case VALUE_NUMBER_FLOAT: data.put(key + "_f", value.asString()); break;
                    case VALUE_TRUE: data.put(key + "_b", Boolean.TRUE); break;
                    case VALUE_FALSE: data.put(key + "_b", Boolean.FALSE); break;
                    case START_ARRAY: data.put(key + "_ss", field.getValue().getElements()); break;
                    default:
                        System.err.println("Unknown token " + value);
                }
            }
        }

        // Fix date format
        List<String> dateKeys = types.get("date");
        if (dateKeys != null) {
            for (String key : dateKeys) {
                if (data.containsKey(key)) {
                    data.put(key, fixDates((String)data.get(key)));
                }
            }
        }

        return data;
    }

    /**
     * Translate date strings
     * @param date
     * @return
     */
    public static String fixDates(String date) {
        return ISODateTimeFormat.dateTime().withZoneUTC().print(new DateTime(date));
    }
}
