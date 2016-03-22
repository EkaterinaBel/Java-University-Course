package javahibernate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class is designed to work with xml-files. It contains methods that allow you to receive, replace
 * or remove the value for the key, and add a new record (key - value) to a file.
 */
public class ConfigurationRead {

    private final String fileName;
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    Document documentConfig = null;

    /**
     * This is a simple constructor that initializes the file name and calls the method for definition API
     * to obtain instance of the Document class from xml-file.
     * @param fileName - name xml-fail
     */
    public ConfigurationRead(String fileName) {
        this.fileName = fileName;
        createXMLFileObject();
    }

    /**
     * This method defines the API to obtain instance the Document class of the xml-file.
     */
    public void createXMLFileObject() {

        try {
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            try {
                documentConfig = documentBuilder.parse(fileName);
            } catch (FileNotFoundException o) {System.err.println("File " + fileName + " not found");}
        } catch (ParserConfigurationException
                | SAXException
                | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The method returns a value for a key. Uses the XPath technology to parse xml-document.
     * @param key - key whose value should be returned
     * @return property value
     */
    public String getProperty(String key) {

        String property = "";
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            XPathExpression expr = xpath.compile("/properties");
            Object result = expr.evaluate(documentConfig, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                property = xpath.compile("./" + key).evaluate(nodes.item(i));
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return property;
    }

    /**
     * The method adds a record (key - value) to the xml-file.
     * @param newKey - key to add
     * @param newValue - value to add
     * @throws TransformerException specifies an exceptional condition that occured during the transformation process
     */
    public void addRecordInXML(String newKey, String newValue) throws TransformerException {

        if (newKey != null && newValue != null && getProperty(newKey).equals("")) {
            Node root = documentConfig.getFirstChild();
            Element element = documentConfig.createElement(newKey);
            root.appendChild(element);
            element.appendChild(documentConfig.createTextNode(newValue));
            updateXML();
        }
    }

    /**
     * The method deletes the record for a key.
     * @param key - key to delete the record
     * @throws TransformerException specifies an exceptional condition that occured during the transformation process
     */
    public void deleteRecordInXML(String key) throws TransformerException {

        if (key != null && !getProperty(key).equals("")) {
            NodeList el = documentConfig.getElementsByTagName(key);
            documentConfig.getFirstChild().removeChild(el.item(0));
            updateXML();
        }
    }

    /**
     * The method replaces the value contained in the key to the new.
     * @param key - key to the new value
     * @param newValue - value to change
     * @throws TransformerException specifies an exceptional condition that occured during the transformation process
     */
    public void replaceRecordInXML(String key, String newValue) throws TransformerException {

        if (key != null && newValue != null && !getProperty(key).equals("")) {
            NodeList el = documentConfig.getElementsByTagName(key);
            el.item(0).setTextContent(newValue);
            updateXML();
        }
    }

    /**
     * This method updates the xml-file in accordance with changes
     * @throws TransformerException specifies an exceptional condition that occured during the transformation process
     */
    private void updateXML() throws TransformerException {

        StreamSource styleSource = new StreamSource(new File("res/Config.xml"));

        DOMSource source = new DOMSource(documentConfig);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer(styleSource);
        StreamResult result = new StreamResult("configuration.xml");
        transformer.transform(source, result);
    }

}
