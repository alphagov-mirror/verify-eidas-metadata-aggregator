package uk.gov.ida.metadataaggregator.metadatasource;

public interface CountryMetadataSource {
    String downloadMetadata(String url) throws MetadataSourceException;
}
