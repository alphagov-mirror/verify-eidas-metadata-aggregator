package uk.gov.ida.metadataaggregator.metadatasource;

import java.io.IOException;

public interface CountryMetadataSource {
    String downloadMetadata(String url) throws IOException, MetadataResolverException;
}
