package uk.gov.ida.metadataaggregator.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import org.apache.commons.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.core.S3BucketMetadataStore;
import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;
import uk.gov.ida.metadataaggregator.util.HexUtils;

import javax.inject.Inject;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ReconciliationHealthCheck extends HealthCheck {

    private final S3BucketMetadataStore metadataStore;
    private final MetadataSourceConfiguration config;
    private final Logger logger = LoggerFactory.getLogger(ReconciliationHealthCheck.class);

    @Inject
    public ReconciliationHealthCheck(S3BucketMetadataStore metadataStore, MetadataSourceConfiguration config) {
        this.metadataStore = metadataStore;
        this.config = config;
    }

    @Override
    public Result check() throws MetadataStoreException {
        Set<String> fileUrlsInBucket = new HashSet<>(metadataStore.getAllHexEncodedUrlsFromS3Bucket());

        Set<String> fileUrlsInConfig = getHexEncodeConfigUrls();

        Set<String> inConfigNotInBucket = new HashSet<>();
        Set<String> invalidHexEncodedUrl = new HashSet<>();

        for (String hexEncodedUrl : fileUrlsInConfig) {
            if (!fileUrlsInBucket.contains(hexEncodedUrl)) {
                String decodeString;
                try {
                    decodeString = HexUtils.decodeString(hexEncodedUrl);
                    inConfigNotInBucket.add(decodeString);
                } catch (DecoderException e) {
                    logger.error("Unable to decode string {} ", hexEncodedUrl, e);
                    invalidHexEncodedUrl.add(hexEncodedUrl);
                }
            }
        }

        Set<String> inBucketNotInConfig = new HashSet<>();

        for (String hexEncodedUrl : fileUrlsInBucket) {
            if (!fileUrlsInConfig.contains(hexEncodedUrl)) {
                String decodeString;
                try {
                    decodeString = HexUtils.decodeString(hexEncodedUrl);
                    inBucketNotInConfig.add(decodeString);
                } catch (DecoderException e) {
                    logger.error("Unable to decode string {} ", hexEncodedUrl, e);
                    invalidHexEncodedUrl.add(hexEncodedUrl);
                }
            }
        }

        if (inConfigNotInBucket.isEmpty() && inBucketNotInConfig.isEmpty()) {
            return Result.healthy();
        } else {
            return Result.builder().unhealthy()
                    .withDetail("inConfigNotInBucket", inConfigNotInBucket)
                    .withDetail("inBucketNotInConfig", inBucketNotInConfig)
                    .withDetail("invalidHexEncodedUrl", invalidHexEncodedUrl)
                    .build();
        }
    }

    private Set<String> getHexEncodeConfigUrls() {
        Collection<String> filesInConfig = config.getMetadataUrls().values().stream().map(URL::toString).collect(Collectors.toSet());
        Set<String> hexedConfigUrls = new HashSet<>();

        for (String configMetadataUrl : filesInConfig) {
            hexedConfigUrls.add(HexUtils.encodeString(configMetadataUrl));
        }
        return hexedConfigUrls;
    }
}
