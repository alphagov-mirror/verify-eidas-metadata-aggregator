package uk.gov.ida.metadataaggregator.config;

import java.io.Serializable;
import java.util.Collection;

public class AggregatorConfig implements Serializable {

    private Collection<String> metadataUrls;
    private String keyStore;

    @SuppressWarnings("unused")
    public AggregatorConfig() {}

    @SuppressWarnings("unused")
    public AggregatorConfig(Collection<String> metadataUrls,
                            String keyStore) {
        this.metadataUrls = metadataUrls;
        this.keyStore = keyStore;
    }

    public Collection<String> getMetadataUrls() {
        return metadataUrls;
    }

    public void setMetadataUrls(Collection<String> metadataUrls) {
        this.metadataUrls = metadataUrls;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }
}
