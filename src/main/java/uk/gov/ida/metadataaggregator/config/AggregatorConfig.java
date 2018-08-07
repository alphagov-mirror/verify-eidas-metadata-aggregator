package uk.gov.ida.metadataaggregator.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AggregatorConfig extends Configuration {

    @Valid
    @JsonProperty
    private Map<String, URL> metadataUrls = new HashMap<String, URL>(0);

    @Valid
    @JsonProperty
    @NotNull
    private String keyStore;

    @Valid
    @JsonProperty
    @NotNull
    private URI trustAnchorUri;

    @Valid
    @JsonProperty
    private long scheduleMilliseconds = 0;

    public AggregatorConfig() {
    }

    public AggregatorConfig(Map<String, URL> metadataUrls,
                            String keyStore) {
        this.metadataUrls = metadataUrls;
        this.keyStore = keyStore;
    }

    public Map<String, URL> getMetadataUrls() {
        return metadataUrls;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public URI getTrustAnchorUri() {
        return trustAnchorUri;
    }

    public long getScheduleMilliseconds() {
        return scheduleMilliseconds;
    }
}
