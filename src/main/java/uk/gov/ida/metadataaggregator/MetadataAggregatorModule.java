package uk.gov.ida.metadataaggregator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.ws.rs.client.ClientBuilder;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.StringInputStream;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import ch.qos.logback.core.util.Duration;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.config.DropwizardConfigSource;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;

import static uk.gov.ida.metadataaggregator.Constants.*;

class MetadataAggregatorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ConfigSource.class).to(DropwizardConfigSource.class);
        bind(MetadataAggregationTaskRunner.class);
        bind(MetadataStore.class).to(S3BucketClient.class);
    }

    @Provides
    @Named("TrustAnchorURI")
    public URI getTrustAnchorURI(AggregatorConfig configuration) {
        return configuration.getTrustAnchorUri();
    }

    @Provides
    @Named("ScheduleFrequency")
    public Duration getScheduleFrequency(AggregatorConfig configuration) {
        return Duration.buildByMilliseconds(configuration.getScheduleMilliseconds());
    }

    @Provides
    public KeyStore getTrustStoreForTrustAnchor(AggregatorConfig configuration) throws MetadataSourceException, EnvironmentVariableException {
        String trustAnchorKeyStorePassword = getEnvironmentVariable(TRUST_ANCHOR_PASSCODE); // TODO
        try (InputStream stream = new StringInputStream(configuration.getKeyStore())) {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(stream, trustAnchorKeyStorePassword.toCharArray());
            return trustStore;
        } catch (IOException e) {
            throw new MetadataSourceException("Unable to read key store string", e);
        } catch (GeneralSecurityException e) {
            throw new MetadataSourceException("Error building trust store", e);
        }
    }

    @Provides
    public EidasTrustAnchorResolver getEidasTrustAnchorResolver(
        @Named("TrustAnchorURI") URI eidasTrustAnchorUriString,
        KeyStore trustStore
    ) {
        return new EidasTrustAnchorResolver(
            eidasTrustAnchorUriString,
            ClientBuilder.newClient(),
            trustStore);
    }

    @Provides
    private S3BucketClient getS3BucketClient(AmazonS3 amazonS3Client) throws EnvironmentVariableException {
        String bucketName = getEnvironmentVariable(BUCKET_NAME);
        return new S3BucketClient(bucketName, amazonS3Client);
    }

    @Provides
    private AmazonS3 getAmazonS3Client() {
        return AmazonS3ClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();
    }

    private static String getEnvironmentVariable(String environmentVariableName) throws EnvironmentVariableException {
        String environmentVariable = System.getenv(environmentVariableName);
        if (environmentVariable == null) {
            throw new EnvironmentVariableException(environmentVariableName + " is not defined");
        }

        return environmentVariable;
    }
}