package uk.gov.ida.metadataaggregator.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.net.URL;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataSourceConfiguration implements Serializable {

    private Map<String, URL> metadataUrls;

    @SuppressWarnings("unused")
    public MetadataSourceConfiguration() {
    }

    @SuppressWarnings("unused")
    public MetadataSourceConfiguration(Map<String, URL> metadataUrls) {
        this.metadataUrls = metadataUrls;
    }

    public Map<String, URL> getMetadataUrls() {
        return metadataUrls;
    }
}
