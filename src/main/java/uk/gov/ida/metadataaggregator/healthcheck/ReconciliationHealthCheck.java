package uk.gov.ida.metadataaggregator.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.core.DecodingResults;
import uk.gov.ida.metadataaggregator.core.S3BucketMetadataStore;
import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;

import javax.inject.Inject;
import java.net.URL;
import java.util.Collection;
import java.util.List;
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
        DecodingResults decodedBucketUrls = metadataStore.getAllUrls();
        Collection<String> fileUrlsInBucket = decodedBucketUrls.urls();
        Collection<String> fileUrlsInConfig = getHexEncodeConfigUrls();

        Collection<String> inConfigNotInBucket = subtract(fileUrlsInConfig, fileUrlsInBucket);
        Collection<String> inBucketNotInConfig = subtract(fileUrlsInBucket, fileUrlsInConfig);

        Collection<String> invalidEncodingUrls = decodedBucketUrls.invalidEncodingUrls();

        if (inConfigNotInBucket.isEmpty() && inBucketNotInConfig.isEmpty() && invalidEncodingUrls.isEmpty()) {
            return Result.healthy();
        } else {
            return Result.builder().unhealthy()
                    .withDetail("inConfigNotInBucket", inConfigNotInBucket)
                    .withDetail("inBucketNotInConfig", inBucketNotInConfig)
                    .withDetail("invalidHexEncodedUrl", invalidEncodingUrls)
                    .build();
        }
    }

    private Collection<String> subtract(Collection<String> a, Collection<String> b) {
        return a.stream()
                .filter(s -> !b.contains(s))
                .collect(Collectors.toSet());
    }

    private Collection<String> getHexEncodeConfigUrls() {
        return config.getMetadataUrls().values().stream()
                .map(URL::toString)
                .collect(Collectors.toSet());
    }
}
