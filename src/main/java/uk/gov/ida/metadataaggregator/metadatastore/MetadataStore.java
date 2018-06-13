package uk.gov.ida.metadataaggregator.metadatastore;

public interface MetadataStore {
    void uploadMetadata(String url, String countryMetadata) throws MetadataStoreException;
}
