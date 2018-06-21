package uk.gov.ida.metadataaggregator.metadatastore;

import java.util.List;

public interface MetadataStore {
    void uploadMetadata(String url, String countryMetadata) throws MetadataStoreException;
    void deleteMetadata(String bucketKey) throws MetadataStoreException;
    List<String> getAllHexEncodedUrlsFromS3Bucket() throws MetadataStoreException;
}
