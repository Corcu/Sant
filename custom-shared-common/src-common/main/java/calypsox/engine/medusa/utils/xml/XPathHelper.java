package calypsox.engine.medusa.utils.xml;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;

/**
 * XPathHelper class
 *
 * @author xIS15793
 */
public class XPathHelper {

    private final Document xmlDocument;

    /**
     * constructor
     *
     * @param xmlMessage     msg
     * @param namespaceAware namespace
     * @throws Exception exception
     */
    public XPathHelper(final String xmlMessage, final boolean namespaceAware)
            throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory
                .newInstance();
        factory.setNamespaceAware(namespaceAware);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        this.xmlDocument = builder.parse(new ByteArrayInputStream(xmlMessage
                .getBytes("UTF8")));

    }

    /**
     * count nodes
     *
     * @param expression expression
     * @return num of nodes
     * @throws Exception exception
     */
    public int countNodes(final String expression) throws Exception {
        final NodeList nodes = getNodeList(expression);
        if (nodes != null) {
            return nodes.getLength();
        }

        throw new Exception("Node " + expression + " not found"); // NOPMD
    }

    /**
     * get the list of nodes
     *
     * @param expression expression
     * @return nodelist
     * @throws Exception exception
     */
    public NodeList getNodeList(final String expression) throws Exception {
        final XPath xpath = XPathFactory.newInstance().newXPath();
        return (NodeList) xpath.evaluate(expression, this.xmlDocument,
                XPathConstants.NODESET);
    }

    /**
     * get value as string
     *
     * @param expression expression
     * @return value
     * @throws Exception exception
     */
    public String getValueAsString(final String expression) throws Exception {
        final XPath xpath = XPathFactory.newInstance().newXPath();
        return (String) xpath.evaluate(expression, this.xmlDocument,
                XPathConstants.STRING);
    }

    /**
     * get value as double
     *
     * @param expression expression
     * @return value
     * @throws Exception exception
     */
    public Double getValueAsDouble(final String expression) throws Exception {
        final XPath xpath = XPathFactory.newInstance().newXPath();
        return (Double) xpath.evaluate(expression, this.xmlDocument,
                XPathConstants.NUMBER);
    }

    /**
     * get the document
     *
     * @return document
     */
    public Document getDocument() {
        return this.xmlDocument;
    }
}
