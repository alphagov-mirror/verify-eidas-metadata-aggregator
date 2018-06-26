package uk.gov.ida.metadataaggregator.metadatasource;

import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.xml.sax.SAXException;
import uk.gov.ida.saml.deserializers.parser.SamlObjectParser;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class CountryMetadataCurler implements CountryMetadataSource {

    private final DocumentBuilder documentBuilder = XmlUtils.newDocumentBuilder();
    private final SamlObjectParser samlObjectParser = new SamlObjectParser();

    public CountryMetadataCurler() throws ParserConfigurationException {
    }

    @Override
    public EntityDescriptor downloadMetadata(URL url) throws MetadataSourceException {

        try {
            URLConnection urlConnection = url.openConnection();
            InputStream in = urlConnection.getInputStream();

            return samlObjectParser.getSamlObject(documentBuilder.parse(in).getDocumentElement());
        } catch (IOException | SAXException | UnmarshallingException e) {
            throw new MetadataSourceException("Unable to retrieve metadatasource from " + url, e);
        }
    }
}
