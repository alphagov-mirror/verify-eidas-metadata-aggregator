package uk.gov.ida.metadataaggregator.metadatastore;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import java.util.List;

public interface MetadataStore {
    void uploadMetadata(String url, EntityDescriptor countryMetadata) throws MetadataStoreException;

    void deleteMetadata(String bucketKey) throws MetadataStoreException;

    List<String> getAllHexEncodedUrlsFromS3Bucket() throws MetadataStoreException;
}
