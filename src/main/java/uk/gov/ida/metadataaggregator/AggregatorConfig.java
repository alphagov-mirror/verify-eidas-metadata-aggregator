package uk.gov.ida.metadataaggregator;

import java.io.Serializable;
import java.util.Collection;

public class AggregatorConfig implements Serializable {

    private Collection<String> metadataUrls;

    @SuppressWarnings("unused")
    public AggregatorConfig() {}

    @SuppressWarnings("unused")
    public AggregatorConfig(Collection<String> metadataUrls) {
        this.metadataUrls = metadataUrls;
    }

    public Collection<String> getMetadataUrls() {
        return metadataUrls;
    }

    public void setMetadataUrls(Collection<String> metadataUrls) {
        this.metadataUrls = metadataUrls;
    }
}
