package eg.com.adcb.IPN.utils;

import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class XMLParser {
    public static Document ReadXML(String xmlBody) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputStream targetStream = new ByteArrayInputStream(xmlBody.getBytes());
        Document doc = db.parse(targetStream);
        doc.getDocumentElement().normalize();
        return doc;
    }

}
