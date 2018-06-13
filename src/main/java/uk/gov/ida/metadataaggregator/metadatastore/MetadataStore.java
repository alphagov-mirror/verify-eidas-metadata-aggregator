package uk.gov.ida.metadataaggregator.metadatastore;

public interface MetadataStore {
    void uploadMetadata(String url, String countryMetadata) throws MetadataStoreException;
    void deleteMetadata(String s3Key) throws MetadataStoreException;
}
