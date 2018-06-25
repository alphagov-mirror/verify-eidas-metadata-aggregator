package uk.gov.ida.metadataaggregator.metadatasource;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class CountryMetadataCurler implements CountryMetadataSource {

    private DocumentBuilder documentBuilder = XmlUtils.newDocumentBuilder();

    public CountryMetadataCurler() throws ParserConfigurationException {
    }

    @Override
    public Element downloadMetadata(String url) throws MetadataSourceException {

        try {
            URLConnection urlConnection = new URL(url).openConnection();
            InputStream in = urlConnection.getInputStream();

            return documentBuilder.parse(in).getDocumentElement();
        } catch (IOException | SAXException e) {
            throw new MetadataSourceException("Unable to retrieve metadatasource from " + url, e);
        }
    }
}
