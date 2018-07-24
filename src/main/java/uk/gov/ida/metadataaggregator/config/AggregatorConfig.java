package uk.gov.ida.metadataaggregator.config;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AggregatorConfig implements Serializable {

    private Map<String, URL> metadataUrls;
    private String keyStore;

    @SuppressWarnings("unused")
    public AggregatorConfig() {
    }

    @SuppressWarnings("unused")
    public AggregatorConfig(Map<String, URL> metadataUrls,
                            String keyStore) {
        this.metadataUrls = metadataUrls;
        this.keyStore = keyStore;
    }

    public Map<String, URL> getMetadataUrls() {
        return metadataUrls;
    }

    public void setMetadataUrls(Map<String, URL> metadataUrls) {
        this.metadataUrls = metadataUrls;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }
}
