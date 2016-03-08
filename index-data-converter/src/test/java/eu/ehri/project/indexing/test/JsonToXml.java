package eu.ehri.project.indexing.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import eu.ehri.project.indexing.converter.Converter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * Test class for crude JSON-XML conversion.
 */
public class JsonToXml implements Converter<JsonNode, Node> {

    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static final Document document;
    static {
        try {
            document = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private final String eleName;

    public JsonToXml(String name) {
        eleName = name;
    }

    @Override
    public Iterable<Node> convert(JsonNode t) throws ConverterException {
        if (!t.isObject()) {
            return Lists.newArrayList();
        }

        Node node = document.createElement(eleName);
        Iterator<Map.Entry<String, JsonNode>> fields = t.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> kv = fields.next();
            Node child = document.createElement(kv.getKey());
            node.appendChild(child);
            if (kv.getValue().isValueNode()) {
                child.appendChild(document.createTextNode(kv.getValue().asText()));
            } else {
                for (Node c : convert(kv.getValue())) {
                    child.appendChild(c);
                }
            }
        }
        return Lists.newArrayList(node);
    }

    public static String nodeToString(Node node) {
        try {
            StringWriter sw = new StringWriter();
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException te) {
            throw new RuntimeException("nodeToString Transformer Exception");
        }
    }
}
