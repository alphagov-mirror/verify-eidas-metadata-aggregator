package uk.gov.ida.metadataaggregator.metadatastore;

import java.io.IOException;

public interface MetadataStore {
    void uploadMetadata(String url, String countryMetadata) throws IOException;
}
