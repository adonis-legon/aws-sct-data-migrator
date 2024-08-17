package app.alegon.aws.sct.migrator.util;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.springframework.core.io.ResourceLoader;

public class XmlHelper {
    private static final XPath xPath = XPathFactory.newInstance().newXPath();

    private static final String UTF_8 = "UTF-8";
    private static final String ISO_8859_1 = "ISO-8859-1";

    public static Document loadDocumentFromString(String xmlContent) throws Exception {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            if (xmlContent.contains(UTF_8) || xmlContent.contains(UTF_8.toLowerCase())) {
                return builder.parse(new ByteArrayInputStream(xmlContent.getBytes(UTF_8)));
            } else {
                return builder.parse(new ByteArrayInputStream(xmlContent.getBytes(ISO_8859_1)));
            }
        } catch (Exception e) {
            throw new Exception("Error loading xml document.", e);
        }
    }

    public static Document loadDocumentFromResourceFile(String xmlResourceName, ResourceLoader resourceLoader)
            throws Exception {
        Resource resource = resourceLoader.getResource("classpath:/xml/" + xmlResourceName);
        Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        String xml = FileCopyUtils.copyToString(reader);

        return loadDocumentFromString(xml);
    }

    public static String getValueOfNode(String expression, Node node) throws XPathExpressionException {
        return xPath.compile(expression).evaluate(node, XPathConstants.STRING).toString();
    }

    public static NodeList getNodeList(Node node, String expression) throws XPathExpressionException {
        return (NodeList) xPath.compile(expression).evaluate(node, XPathConstants.NODESET);
    }
}
