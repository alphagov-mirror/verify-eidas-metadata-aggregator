package uk.gov.ida.metadataaggregator.metadatasource;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import java.net.URL;

public interface CountryMetadataSource {
    EntityDescriptor downloadMetadata(URL url) throws MetadataSourceException;
}
