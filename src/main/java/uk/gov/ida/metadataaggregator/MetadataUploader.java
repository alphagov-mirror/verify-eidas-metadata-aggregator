package uk.gov.ida.metadataaggregator;

import java.io.IOException;

public interface MetadataUploader {
    void uploadMetadata(String url, String countryMetadata) throws IOException;
}
