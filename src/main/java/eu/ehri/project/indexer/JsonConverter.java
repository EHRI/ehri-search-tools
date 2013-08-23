package eu.ehri.project.indexer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonToken;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class JsonConverter {

    private static SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final String[] dateStrings = {
            "lastUpdated"
    };

    private static final Map<String,String> paths = ImmutableMap.<String,String>builder()
        .put("id", "$.id")
        .put("itemId", "$.id")
        .put("type", "$.type")
        .put("name", "$.data.name")
        .put("typeOfEntity", "$.data.typeOfEntity")
        .put("depthOfDescription", "$.data.depthOfDescription")
        .put("levelOfDescription", "$.data.levelOfDescription")
        .put("scope", "$.data.scope")
        .put("publicationStatus", "$.data.publicationStatus")
        .put("lastUpdated", "$.relationships.lifecycleEvent[0].data.timestamp")
        .put("accessPoints", "$.relationships.relatesTo[*].data.name")
        .put("parallelFormsOfName", "$.data.parallelFormsOfName[*]")
        .put("otherFormsOfName", "$.data.otherFormsOfName[*]")
        .put("identifier", "$.data.identifier")
        .put("languageCode", "$.data.languageCode")
        .put("repositoryId", "$.relationships.heldBy[0].id")
        .put("repositoryName", "$.relationships.heldBy[0].relationships.describes[0].data.name")
        .put("parentId",            "$.relationships.childOf[0].id")
        .put("countryCode",         "$.relationships.hasCountry[0].id")
        .put("holderName",          "$.relationships.heldBy[0].relationships.describes[0].data.name")
    .build();

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

        for (Map.Entry<String,String> attrPath : paths.entrySet()) {
            String attr = attrPath.getKey();
            String path = attrPath.getValue();

            try {
                data.put(attr, JsonPath.read(node.toString(), path));
            } catch (InvalidPathException e) {
            }
        }

        Iterator<Map.Entry<String,JsonNode>> dataFields = node.path("data").getFields();
        while (dataFields.hasNext()) {
            Map.Entry<String,JsonNode> field = dataFields.next();
            String key = field.getKey();
            if (!paths.containsKey(key)) {
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
        for (String key : dateStrings) {
            if (data.containsKey(key)) {
                data.put(key, fixDates((String)data.get(key)));
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
