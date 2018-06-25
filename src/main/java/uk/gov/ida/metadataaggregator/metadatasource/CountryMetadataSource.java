package uk.gov.ida.metadataaggregator.metadatasource;

import org.w3c.dom.Element;

public interface CountryMetadataSource {
    Element downloadMetadata(String url) throws MetadataSourceException;
}
