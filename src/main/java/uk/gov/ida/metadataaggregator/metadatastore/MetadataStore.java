package uk.gov.ida.metadataaggregator.metadatastore;

import org.w3c.dom.Element;

import java.util.List;

public interface MetadataStore {
    void uploadMetadata(String url, Element countryMetadata) throws MetadataStoreException;
    void deleteMetadata(String bucketKey) throws MetadataStoreException;
    List<String> getAllHexEncodedUrlsFromS3Bucket() throws MetadataStoreException;
}
