package ra.hul.sdet.dataprocessing;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * XML Parsing and Validation - Parse XML with DocumentBuilder, extract nodes via XPath,
 * and validate against an XSD schema using javax.xml.validation.
 * Common SDET question: "Parse a config/SOAP-style XML, pull out values with XPath, and
 * validate the document against an XSD (good doc passes, bad doc fails)."
 *
 * Self-contained: XML and XSD are inline strings (NO files, NO network).
 */
public class Ques3_XmlParsingAndValidation {

    static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <catalog>
              <book id="b1">
                <title>Effective Java</title>
                <price>45.00</price>
              </book>
              <book id="b2">
                <title>Clean Code</title>
                <price>38.50</price>
              </book>
              <book id="b3">
                <title>The Pragmatic Programmer</title>
                <price>52.00</price>
              </book>
            </catalog>
            """;

    static final String XSD = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element name="catalog">
                <xs:complexType>
                  <xs:sequence>
                    <xs:element name="book" maxOccurs="unbounded">
                      <xs:complexType>
                        <xs:sequence>
                          <xs:element name="title" type="xs:string"/>
                          <xs:element name="price" type="xs:decimal"/>
                        </xs:sequence>
                        <xs:attribute name="id" type="xs:string" use="required"/>
                      </xs:complexType>
                    </xs:element>
                  </xs:sequence>
                </xs:complexType>
              </xs:element>
            </xs:schema>
            """;

    static Document parse(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // Hardening: disable external entity resolution (XXE) — good SDET practice.
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    /** Extract all book titles via XPath. */
    static List<String> titles(Document doc) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/catalog/book/title/text()", doc, XPathConstants.NODESET);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) out.add(nodes.item(i).getNodeValue());
        return out;
    }

    /** Extract a single value with XPath: the title of the book whose price is highest > threshold. */
    static String titleById(Document doc, String id) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        return (String) xpath.evaluate("/catalog/book[@id='" + id + "']/title/text()", doc, XPathConstants.STRING);
    }

    /** Sum of all prices via XPath number extraction. */
    static double totalPrice(Document doc) throws Exception {
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/catalog/book/price/text()", doc, XPathConstants.NODESET);
        double sum = 0;
        for (int i = 0; i < nodes.getLength(); i++) sum += Double.parseDouble(nodes.item(i).getNodeValue().trim());
        return sum;
    }

    /** Validate an XML string against the XSD. Returns null if valid, else the error message. */
    static String validate(String xml, String xsd) {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(new StreamSource(new StringReader(xsd)));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
            return null; // valid
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    static void main() throws Exception {
        Document doc = parse(XML);

        List<String> titles = titles(doc);
        System.out.println("Titles (XPath): " + titles);
        check("XPath extracts all titles",
                titles.equals(List.of("Effective Java", "Clean Code", "The Pragmatic Programmer")));

        String b2 = titleById(doc, "b2");
        System.out.println("book[@id='b2']/title = " + b2);
        check("XPath predicate by attribute", "Clean Code".equals(b2));

        double total = totalPrice(doc);
        System.out.printf("Total price (XPath sum): %.2f%n", total);
        check("XPath numeric aggregation", Math.abs(total - 135.50) < 1e-9);

        // Valid document passes XSD validation
        String validErr = validate(XML, XSD);
        System.out.println("Valid doc validation result: " + (validErr == null ? "VALID" : validErr));
        check("valid XML passes XSD", validErr == null);

        // Invalid document (missing required <price>, missing 'id' attribute) fails XSD validation
        String badXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <catalog>
                  <book>
                    <title>Broken Book</title>
                  </book>
                </catalog>
                """;
        String badErr = validate(badXml, XSD);
        System.out.println("Invalid doc validation result: " + badErr);
        check("invalid XML fails XSD", badErr != null);

        System.out.println("PASSED: XML parsed, XPath extraction OK, XSD validation accepts good & rejects bad.");
    }

    static void check(String label, boolean ok) {
        System.out.println((ok ? "  PASS: " : "  FAIL: ") + label);
        if (!ok) throw new AssertionError("Check failed: " + label);
    }
}
