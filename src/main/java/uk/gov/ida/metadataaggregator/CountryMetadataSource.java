package uk.gov.ida.metadataaggregator;

import java.io.IOException;

public interface CountryMetadataSource {
    String downloadMetadata(String url) throws IOException;
}
