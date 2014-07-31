package eu.ehri.project.search.converter.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import eu.ehri.project.search.converter.Converter;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.*;

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
     * Default locale for language/country conversions...
     */
    private static final Locale defaultLocale = Locale.ENGLISH;

    /**
     * Format dates and times for Solr.
     */
    private static final DateTimeFormatter dateTimeFormatter
            = ISODateTimeFormat.dateTime().withZoneUTC();

    /**
     * Static lookup of country names.
     */
    private static final ImmutableMap<String,String> countryLookup;

    static {
        Map<String,String> countries = Maps.newHashMap();
        for (String cc : Locale.getISOCountries()) {
            countries.put(cc.toLowerCase(),
                    new Locale(defaultLocale.getLanguage(), cc).getDisplayCountry());
        }
        countryLookup = ImmutableMap.copyOf(countries);
    }


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
    public Iterable<JsonNode> convert(JsonNode node) throws ConverterException {
        List<JsonNode> out = Lists.newArrayList();
        Iterator<JsonNode> elements = node.path("relationships").path("describes").getElements();
        List<JsonNode> descriptions = Lists.newArrayList(elements);

        if (descriptions.size() > 0) {
            for (JsonNode description : descriptions) {
                out.add(mapper.valueToTree(postProcess(getDescribedData(description, node))));
            }
        } else {
            out.add(mapper.valueToTree(postProcess(getData(node))));
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
            if (!itemData.getKey().equals("id") && itemData.getValue() != null) {
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
                    Object value = path.read(nodeString);
                    if (value != null) {
                        data.put(attr, value);
                        break;
                    }
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
                        data.put(key + "_t", field.getValue().asText());
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

        return data;
    }

    /**
     * Do various post-processing steps on index data.
     * @param data The original data.
     * @return The enhanced data.
     */
    public static Map<String,Object> postProcess(Map<String,Object> data) {
        // Fix date format
        List<String> dateKeys = types.get("date");
        if (dateKeys != null) {
            for (String key : dateKeys) {
                if (data.containsKey(key)) {
                    try {
                        data.put(key, fixDates((String) data.get(key)));
                    } catch (IllegalArgumentException e) {
                        data.remove(key);
                        System.err.println("Invalid date: " + data.get(key) + " (in: " + data.get("id") + ")");
                    }
                }
            }
        }

        // HACK! Combine dateStart and dateEnd into dateRange,
        // ensuring there are no duplicates.
        // FIXME: Unsafe cast
        Set<Object> dateRanges = (HashSet<Object>)data.get("dateRange");
        if (dateRanges == null) {
            dateRanges = Sets.newHashSet();
        }
        for (String d : new String[]{"dateStart", "dateEnd"}) {
            if (data.containsKey(d)) {
                dateRanges.add(data.get(d));
            }
        }
        if (!dateRanges.isEmpty()) {
            data.put("dateRange", dateRanges);
        }

        // HACK! Set restricted=true for items that have accessors.
        // NB: This should be done before setting field defaults, because
        // the default for items without accessibleTo is ["ALLUSERS"]
        data.put("restricted", data.containsKey("accessibleTo"));

        // Add defaults
        // NB: Simple defaults can be set directly in the schema, so this is
        // only necessary when more complex operations need to be performed,
        // i.e. defaults for multivalue fields with more than one entry.
        for (Map.Entry<String, List<String>> entry : defaults.entrySet()) {
            if (!data.containsKey(entry.getKey())) {
                data.put(entry.getKey(), entry.getValue());
            }
        }

        // HACK! Create a composite 'location' field from latitude and longitude
        Object latitude = data.get("latitude");
        Object longitude = data.get("longitude");
        if (latitude != null && longitude != null) {
            data.put("location", latitude + "," + longitude);
        }

        // HACK! Set isTopLevel attr for items where parentId is not defined
        data.put("isTopLevel", !data.containsKey("parentId"));

        // HACK: if countryCode is set, translate it to a name in the default locale:
        if (data.containsKey("countryCode")) {
            data.put("countryName", countryLookup.get(data.get("countryCode")));
        }

        // HACK: Set country name to name field on country type
        if (data.containsKey("type") && data.get("type").equals("country")) {
            data.put("name", countryLookup.get(data.get("id")));
        }

        // HACK: Set charCount field as sum of string field data...
        int charCount = 0;
        for (Map.Entry<String,Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof String) {
                charCount += ((String) entry.getValue()).length();
            }
        }
        data.put("charCount", charCount);

        return data;
    }

    /**
     * Translate date strings
     *
     * @param date The date string to be fixed
     * @return A Solr-compliant version of input string
     */
    private static String fixDates(String date) {
        return dateTimeFormatter.print(new DateTime(date));
    }
}
