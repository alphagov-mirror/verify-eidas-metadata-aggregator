package uk.gov.ida.metadataaggregator.metadatasource;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

public interface CountryMetadataSource {
    EntityDescriptor downloadMetadata(String url) throws MetadataSourceException;
}
