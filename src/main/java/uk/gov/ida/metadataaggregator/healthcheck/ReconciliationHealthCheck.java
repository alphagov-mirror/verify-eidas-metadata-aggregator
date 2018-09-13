package uk.gov.ida.metadataaggregator.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.core.S3BucketMetadataStore;
import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;
import uk.gov.ida.metadataaggregator.util.HexUtils;

import javax.inject.Inject;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ReconciliationHealthCheck extends HealthCheck {

    private final S3BucketMetadataStore metadataStore;
    private final MetadataSourceConfiguration config;

    @Inject
    public ReconciliationHealthCheck(S3BucketMetadataStore metadataStore, MetadataSourceConfiguration config) {
        this.metadataStore = metadataStore;
        this.config = config;
    }

    @Override
    public Result check() throws MetadataStoreException {
        HashSet<String> fileUrlsInBucket = new HashSet<String>(metadataStore.getAllHexEncodedUrlsFromS3Bucket());
        HashSet<String> inBucketNotInConfig = new HashSet<String>(fileUrlsInBucket);

        HashSet<String> filesUrlsInConfig = getHexEncodeConfigUrls();
        HashSet<String> inConfigNotInBucket = new HashSet<String>(filesUrlsInConfig);

        inConfigNotInBucket.removeAll(fileUrlsInBucket);
        inBucketNotInConfig.removeAll(filesUrlsInConfig);

        if (!inConfigNotInBucket.isEmpty() || !inBucketNotInConfig.isEmpty()) {
            return Result.builder().unhealthy()
                    .withDetail("inConfigNotInBucket", inConfigNotInBucket)
                    .withDetail("inBucketNotInConfig", inBucketNotInConfig)
                    .build();
        } else {
            return Result.healthy();
        }
    }

    private HashSet<String> getHexEncodeConfigUrls() {
        Collection<String> filesInConfig = config.getMetadataUrls().values().stream().map(URL::toString).collect(Collectors.toSet());
        HashSet<String> hexedConfigUrls = new HashSet<>();

        for (String configMetadataUrl : filesInConfig) {
            hexedConfigUrls.add(HexUtils.encodeString(configMetadataUrl));
        }
        return hexedConfigUrls;
    }
}
