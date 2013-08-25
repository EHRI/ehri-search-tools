package eu.ehri.project.indexer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    // JSON mapper and writer
    static final ObjectMapper mapper = new ObjectMapper();
    static final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

    /**
     * Default constructor.
     */
    public JsonConverter() {
    }

    /**
     * Write converted JSON data to an output stream.
     *
     * @param in    The type of item to reindex
     * @param out   The output stream for converted JSON
     * @param stats A Stats object for storing metrics
     * @throws java.io.IOException
     */
    void convertStream(InputStream in, OutputStream out, Indexer.Stats stats) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(br);

        try {
            jp.nextToken();

            JsonGenerator generator = f.createJsonGenerator(out);
            try {
                generator.writeStartArray();
                generator.writeRaw('\n');
                while (jp.nextToken() == JsonToken.START_OBJECT) {
                    JsonNode node = mapper.readValue(jp, JsonNode.class);
                    convertItem(node, generator);
                    stats.itemCount++;
                }
                generator.writeEndArray();
                generator.writeRaw('\n');
            } finally {
                generator.flush();
                generator.close();
            }
        } finally {
            jp.close();
            br.close();
        }
    }

    /**
     * Convert a individual item into one or more output items
     *
     * @param node      A JSON node representing a single item
     * @return          The output nodes
     *                  the converted data
     * @throws java.io.IOException
     */
    public Iterable<JsonNode> convertItem(JsonNode node) throws IOException {
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
     * Convert a individual item from the stream and write the results.
     *
     * @param node      A JSON node representing a single item
     * @param generator The JSON generator with which to write
     *                  the converted data
     * @throws java.io.IOException
     */
    void convertItem(JsonNode node, JsonGenerator generator) throws IOException {
        for (JsonNode out : convertItem(node)) {
            writer.writeValue(generator, out);
        }
    }


    /**
     * Get data for items where most of it resides in the description
     * nodes.
     *
     * @param description   The description's JSON node
     * @param item          The item's JSON node
     * @return              A map of the extracted data
     */
    public static Map<String,Object> getDescribedData(JsonNode description, JsonNode item) {

        // Tricky code alert!
        // Matching paths in the 'item' node overrides that of the description,
        // though in practice there should almost never be any collisions. The
        // exceptions are the 'id', 'itemId' and 'type' fields. In these cases
        // we use the item's type (i.e. documentaryUnit instead of documentDescription)
        // but the description's id. We therefore only have to prevent the description's
        // 'id' field being overwritten when we merge the item and description data.
        Map<String,Object> descriptionData = getData(description);

        // Merge the data, preventing overwriting of the id key - any other
        // keys should be overwritten.
        for (Map.Entry<String,Object> itemData : getData(item).entrySet()) {
            if (!itemData.getKey().equals("id")) {
                descriptionData.put(itemData.getKey(), itemData.getValue());
            }
        }
        return descriptionData;
    }


    /**
     * Get data for non-described items.
     * @param node  The item's JSON node
     * @return      A map of the extracted data
     */
    public static Map<String,Object> getData(JsonNode node) {
        Map<String, Object> data = Maps.newHashMap();

        final String nodeString = node.toString();

        // Extract specific properties
        for (Map.Entry<String,JsonPath> attrPath : jsonPaths.entrySet()) {
            String attr = attrPath.getKey();
            JsonPath path = attrPath.getValue();

            try {
                data.put(attr, path.read(nodeString));
            } catch (InvalidPathException e) {
                // Intentionally ignore invalid paths - we might
                // want to take a smarter approach in future.
            }
        }

        // Any keys in the 'data' section are indexed in dynamic fields
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
                    case VALUE_NULL: break;
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
     * @param date  The date string to be fixed
     * @return      A Solr-compliant version of input string
     */
    public static String fixDates(String date) {
        return ISODateTimeFormat.dateTime().withZoneUTC().print(new DateTime(date));
    }
}
