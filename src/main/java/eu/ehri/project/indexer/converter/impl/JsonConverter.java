package eu.ehri.project.indexer.converter.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import eu.ehri.project.indexer.converter.Converter;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 *         <p/>
 *         Convert from EHRI graph JSON to Solr documents.
 */
public class JsonConverter implements Converter<JsonNode> {

    /**
     * Set of key -> JsonPath extractors
     */
    private static final Map<String, List<JsonPath>> jsonPaths = Utils.loadPaths();

    /**
     * Keys which have types that require special handling.
     */
    private static final Map<String, List<String>> types = Utils.loadTypeKeys();

    /**
     * Keys which need a default value
     */
    private static final Map<String, List<String>> defaults = Utils.loadDefaultKeys();

    // JSON mapper
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Default constructor.
     */
    public JsonConverter() {
    }

    /**
     * Convert a individual item into one or more output items
     *
     * @param node A JSON node representing a single item
     * @return The output nodes
     *         the converted data
     */
    public Iterable<JsonNode> convert(JsonNode node) {
        List<JsonNode> out = Lists.newArrayList();
        Iterator<JsonNode> elements = node.path("relationships").path("describes").getElements();
        List<JsonNode> descriptions = Lists.newArrayList(elements);

        if (descriptions.size() > 0) {
            for (JsonNode description : descriptions) {
                out.add(mapper.valueToTree(getDescribedData(description, node)));
            }
        } else {
            out.add(mapper.valueToTree(getData(node)));
        }
        return out;
    }

    /**
     * Get data for items where most of it resides in the description
     * nodes.
     *
     * @param description The description's JSON node
     * @param item        The item's JSON node
     * @return A map of the extracted data
     */
    private static Map<String, Object> getDescribedData(JsonNode description, JsonNode item) {

        // Tricky code alert!
        // Matching paths in the 'item' node overrides that of the description,
        // though in practice there should almost never be any collisions. The
        // exceptions are the 'id', 'itemId' and 'type' fields. In these cases
        // we use the item's type (i.e. documentaryUnit instead of documentDescription)
        // but the description's id. We therefore only have to prevent the description's
        // 'id' field being overwritten when we merge the item and description data.
        Map<String, Object> descriptionData = getData(description);

        // Merge the data, preventing overwriting of the id key - any other
        // keys should be overwritten.
        for (Map.Entry<String, Object> itemData : getData(item).entrySet()) {
            if (!itemData.getKey().equals("id")) {
                descriptionData.put(itemData.getKey(), itemData.getValue());
            }
        }
        return descriptionData;
    }


    /**
     * Get data for non-described items.
     *
     * @param node The item's JSON node
     * @return A map of the extracted data
     */
    private static Map<String, Object> getData(JsonNode node) {
        Map<String, Object> data = Maps.newHashMap();

        final String nodeString = node.toString();

        // Extract specific properties
        for (Map.Entry<String, List<JsonPath>> attrPath : jsonPaths.entrySet()) {
            String attr = attrPath.getKey();
            // First successfully matched path wins...
            for (JsonPath path : attrPath.getValue()) {
                try {
                    data.put(attr, path.read(nodeString));
                    break;
                } catch (InvalidPathException e) {
                    // Intentionally ignore invalid paths - we might
                    // want to take a smarter approach in future.
                }
            }
        }

        // Any keys in the 'data' section are indexed in dynamic fields
        Iterator<Map.Entry<String, JsonNode>> dataFields = node.path("data").getFields();
        while (dataFields.hasNext()) {
            Map.Entry<String, JsonNode> field = dataFields.next();
            String key = field.getKey();
            if (!jsonPaths.containsKey(key)) {
                JsonToken value = field.getValue().asToken();
                switch (value) {
                    case VALUE_STRING:
                        data.put(key + "_s", field.getValue().asText());
                        break;
                    case VALUE_NUMBER_INT:
                        data.put(key + "_i", value.asString());
                        break;
                    case VALUE_NUMBER_FLOAT:
                        data.put(key + "_f", value.asString());
                        break;
                    case VALUE_TRUE:
                        data.put(key + "_b", Boolean.TRUE);
                        break;
                    case VALUE_FALSE:
                        data.put(key + "_b", Boolean.FALSE);
                        break;
                    case START_ARRAY:
                        data.put(key + "_ss", field.getValue().getElements());
                        break;
                    case VALUE_NULL:
                        break;
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
                    data.put(key, fixDates((String) data.get(key)));
                }
            }
        }

        // Add defaults
        for (Map.Entry<String, List<String>> entry : defaults.entrySet()) {
            if (!data.containsKey(entry.getKey())) {
                data.put(entry.getKey(), entry.getValue());
            }
        }

        // HACK! Set isTopLevel attr for items where parentId is not defined
        data.put("isTopLevel", !data.containsKey("parentId"));

        return data;
    }

    /**
     * Translate date strings
     *
     * @param date The date string to be fixed
     * @return A Solr-compliant version of input string
     */
    private static String fixDates(String date) {
        return ISODateTimeFormat.dateTime().withZoneUTC().print(new DateTime(date));
    }
}
