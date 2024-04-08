package calypsox.util;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.util.DOMUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public class DOMUtility {

    /**
     * Get the document from an inputStream
     * 
     * @return the document from the file
     * @throws IOException
     * @throws SAXException
     * @throws Exception
     *             If Input Stream doesn't exist or error parsing.
     */
    public static Document createDOMDocument(InputStream streamXML)
	    throws SAXException, IOException {
	DOMParser parser = null;
	parser = new DOMParser();
	parser.parse(new InputSource(streamXML));
	return parser.getDocument();
    }

    /**
     * Look for in the DOM tree an elemento named childName. If there are more
     * than one I only return the first one. Notice: Look for in all tree, not
     * only in childs of document<br>
     * 
     * @param documento
     *            the document from that get the child node
     * @param nameChild
     *            name of child node to look for
     * @param mandatory
     *            true if child node is mandatory
     * @return the child node
     * @throws Exception
     *             if parent node is null or child node is null when mandatory
     *             is true
     */
    public static Node getFirstChildElementRecursivoObligatorio(
	    Document documento, String childName, boolean mandatory) {
	Node nodoHijo = null;
	if (documento != null) {
	    NodeList nodosHijos = documento.getElementsByTagName(childName);
	    if (nodosHijos != null && nodosHijos.getLength() != 0) {
		nodoHijo = nodosHijos.item(0);
	    }
	    /*
	     * if (mandatory && nodoHijo == null) { Object[] param = {
	     * documento.getNodeName(), childName }; throw new
	     * Exception("Child node not found"); }
	     */
	}
	return nodoHijo;
    }

    /**
     * Get the attribute text in a Node. It can validate that the attribute
     * exists and this is not empty.
     * 
     * @param node
     *            the node from which get the text
     * @param mandatoryText
     *            true if we want to validate that the text exists and it's not
     *            empty.
     * @param attribute
     *            the attribute of the node to look for
     * @return the text linked to the node. Empty string if it's empty
     * @throws Exception
     *             When mandatoryText=true and text linked is empty or null
     */
    public static String getAttribute(Node node, String attribute,
	    boolean mandatoryText) throws Exception {
	String retorno = null;
	retorno = DOMUtil.getAttrValue((Element) node, attribute);
	// Remove white spaces in the ends
	if (retorno != null)
	    retorno = retorno.trim();
	// If it's mandatory but the text is null or empty --> Exception
	if (mandatoryText && (retorno == null || retorno.length() < 1)) {
	    if (node == null) {
		throw new Exception("Null node");
	    } else {
		throw new Exception("Attribute is empty.");
	    }
	}
	return retorno;
    }

    /**
     * From a parent node get the text of first child element
     * 
     * @param parentNode
     *            the node with childs
     * @param childName
     *            the child node to look for
     * @param mandatoryText
     *            true if we want to validate that the text exists and it's not
     *            empty.
     * @return the text linked to the node. Empty string if it's empty.
     * @throws Exception
     *             When mandatoryText=true and text linked is empty or null
     */
    public static String getTextFromChild(Node parentNode, String childName,
	    boolean mandatoryText) throws Exception {
	if (parentNode == null) {
	    throw new Exception("Parent node is null.");
	}
	Node childNode = DOMUtil.getFirstChildElement(parentNode, childName);
	if (childNode == null && mandatoryText) {
	    throw new Exception("Child node not found.");
	}
	return DOMUtility.getText(childNode, mandatoryText);
    }

    /**
     * Get the text linked to a node. It can validate that the attribute exists
     * and this is not empty.
     * 
     * @param node
     *            the node from which get the text
     * @param mandatoryText
     *            true if we want to validate that the text exists and it's not
     *            empty.
     * @return text linked to the node. Empty string if it's empty.
     * @throws Exception
     *             When mandatoryText=true and text linked is empty or null
     */
    public static String getText(Node node, boolean mandatoryText)
	    throws Exception {
	String retorno = null;
	retorno = DOMUtil.getChildText(node);

	// Remove white spaces in the ends
	if (retorno != null)
	    retorno = retorno.trim();
	// If it's mandatory but the text is null or empty --> Exception
	if (mandatoryText && (retorno == null || retorno.length() < 1)) {
	    if (node == null) {
		throw new Exception("Null node.");
	    } else {
		throw new Exception("Empty node.");
	    }
	}
	return retorno;
    }

}
