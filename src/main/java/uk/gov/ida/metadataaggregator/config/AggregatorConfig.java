package uk.gov.ida.metadataaggregator.config;

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;

public class AggregatorConfig implements Serializable {

    private Collection<URL> metadataUrls;
    private String keyStore;

    @SuppressWarnings("unused")
    public AggregatorConfig() {}

    @SuppressWarnings("unused")
    public AggregatorConfig(Collection<URL> metadataUrls,
                            String keyStore) {
        this.metadataUrls = metadataUrls;
        this.keyStore = keyStore;
    }

    public Collection<URL> getMetadataUrls() {
        return metadataUrls;
    }

    public void setMetadataUrls(Collection<URL> metadataUrls) {
        this.metadataUrls = metadataUrls;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }
}
