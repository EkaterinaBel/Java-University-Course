import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
     * The method checks the xml-file to the content in it of the key "newRecord".
     * @param newRecord - key, which will be found in the file
     * @return true if the key is found, false - in the opposite case
     * @throws IOException - error associated with reading or existence file
     */
    private boolean checkExistenceRecord(String newRecord) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("<" + newRecord + ">")) {
                return true;
            }
        }
        return false;
    }

    /**
     * The method adds a record (key - value) to the xml-file. To this purpose at first is read from a file
     * and write the information in the List. Then, a new record (key - value) is added to the list.
     * After that, the source file is overwritten with values from the List.
     * @param newKey - key to add
     * @param newValue - value to add
     * @throws IOException - error associated with reading, writing or existence file
     */
    public void addRecordInXML(String newKey, String newValue) throws IOException {
        if(checkExistenceRecord(newKey)) {
            System.out.println("Recording with this key already exists");
        }
        else {
            String stopWord = "</properties>";
            List<String> records = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(stopWord)) {
                    records.add(line);
                }
            }
            records.add("\t" + "<" + newKey + ">" + newValue + "</" + newKey + ">");
            records.add("</properties>");
            Writer writer = null;
            try {
                writer = new FileWriter(fileName);
                for (String o : records) {
                    writer.write(o);
                    writer.write(System.getProperty("line.separator"));
                }
                writer.flush();
            } catch (Exception e) {
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ex) {}
                }
            }
            createXMLFileObject();
        }
    }

    /**
     * The method replaces the value contained in the key to the new. To this purpose at first is read from a file
     * and write the information in the List (overwriting the old values to the new values on the key).
     * After that, the source file is overwritten with values from the List.
     * @param key - key to the new value
     * @param newValue - value to change
     * @throws IOException - error associated with reading, writing or existence file
     */
    public void setRecordInXML(String key, String newValue) throws IOException {
        if(!checkExistenceRecord(key)) {
            System.out.println("This key not exists in file");
        }
        else {
            List<String> records = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<" + key + ">")) records.add("\t" + "<" + key + ">" + newValue + "</" + key + ">");
                else records.add(line);
            }
            Writer writer = null;
            try {
                writer = new FileWriter(fileName);
                for (String o : records) {
                    writer.write(o);
                    writer.write(System.getProperty("line.separator"));
                }
                writer.flush();
            } catch (Exception e) {
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ex) {}
                }
            }
            createXMLFileObject();
        }
    }

    /**
     * The method deletes the record for a key. To this purpose at first is read from a file
     * and write the information in the List (deletes the record for a given key).
     * After that, the source file is overwritten with values from the List.
     * @param key - key to delete the record
     * @throws IOException - error associated with reading, writing or existence file
     */
    public void deleteRecordInXML(String key) throws IOException {
        if(!checkExistenceRecord(key)) {
            System.out.println("This key not exists in file");
        }
        else {
            List<String> records = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(key)) {
                    records.add(line);
                }
            }
            Writer writer = null;
            try {
                writer = new FileWriter(fileName);
                for (String o : records) {
                    writer.write(o);
                    writer.write(System.getProperty("line.separator"));
                }
                writer.flush();
            } catch (Exception e) {
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ex) {}
                }
            }
            createXMLFileObject();
        }
    }

}
