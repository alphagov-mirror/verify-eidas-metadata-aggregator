package uk.gov.ida.metadataaggregator.config;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;

public class AggregatorConfig implements Serializable {

    private HashMap<String,URL> metadataUrls;
    private String keyStore;

    @SuppressWarnings("unused")
    public AggregatorConfig() {}

    @SuppressWarnings("unused")
    public AggregatorConfig(HashMap<String,URL> metadataUrls,
                            String keyStore) {
        this.metadataUrls = metadataUrls;
        this.keyStore = keyStore;
    }

    public HashMap<String,URL> getMetadataUrls() {
        return metadataUrls;
    }

    public void setMetadataUrls(HashMap<String,URL> metadataUrls) {
        this.metadataUrls = metadataUrls;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }
}
