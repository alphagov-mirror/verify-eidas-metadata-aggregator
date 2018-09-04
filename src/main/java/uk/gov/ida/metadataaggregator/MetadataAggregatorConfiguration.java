package uk.gov.ida.metadataaggregator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import uk.gov.ida.saml.metadata.TrustStoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.security.KeyStore;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataAggregatorConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty
    private URI trustAnchorUri;

    @Valid
    @NotNull
    @JsonProperty
    private TrustStoreConfiguration trustStore;

    @Valid
    @JsonProperty
    private String s3BucketName;

    @Valid
    @JsonProperty
    private String metadataSourcesFile;

    protected MetadataAggregatorConfiguration() { }

    public URI getTrustAnchorUri() {
        return trustAnchorUri;
    }

    public KeyStore getTrustStore() {
        return trustStore.getTrustStore();
    }

    public String getS3BucketName() {
        return s3BucketName;
    }

    public String getMetadataSourcesFile() { return metadataSourcesFile; }
}
