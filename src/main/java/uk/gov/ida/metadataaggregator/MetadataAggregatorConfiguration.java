package uk.gov.ida.metadataaggregator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.configuration.ServiceNameConfiguration;
import uk.gov.ida.saml.metadata.TrustStoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.security.KeyStore;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataAggregatorConfiguration extends Configuration implements ServiceNameConfiguration {

    @JsonProperty
    @NotNull
    @Valid
    private ServiceInfoConfiguration serviceInfo;

    @Valid
    @NotNull
    @JsonProperty
    private URI trustAnchorUri;

    @Valid
    @JsonProperty
    private String environment;

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

    @Valid
    @JsonProperty
    private long hoursBetweenEachRun = 1;

    @Valid
    @JsonProperty
    private String awsRegion;

    public MetadataAggregatorConfiguration() { }

    public URI getTrustAnchorUri() {
        return trustAnchorUri;
    }

    public KeyStore getTrustStore() {
        return trustStore.getTrustStore();
    }

    public String getS3BucketName() {
        return s3BucketName;
    }

    public String getEnvironment() {
        return environment;
    }

    public long getHoursBetweenEachRun() {
        return hoursBetweenEachRun;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    @Override
    public String getServiceName() {
        return serviceInfo.getName();
    }
}
