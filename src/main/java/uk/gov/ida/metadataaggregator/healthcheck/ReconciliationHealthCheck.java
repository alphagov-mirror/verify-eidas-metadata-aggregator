package uk.gov.ida.metadataaggregator.healthcheck;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;

import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.core.MetadataStore;
import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;

public class ReconciliationHealthCheck extends HealthCheck {
    private final MetadataStore metadataStore;
    private final MetadataSourceConfiguration config;

    @Inject
    public ReconciliationHealthCheck(MetadataStore metadataStore, MetadataSourceConfiguration config) {
        this.metadataStore = metadataStore;
        this.config = config;
    }

    @Override
    public Result check() throws Exception {
        Set<String> filesInBucket = new HashSet<String>(metadataStore.list());
        Set<String> filesInConfig = config.getMetadataUrls().values().stream().map(URL::toString).collect(Collectors.toSet());
        Set<String> missingFromBucket = new HashSet<String>(filesInConfig);
        missingFromBucket.removeAll(filesInBucket);
        Set<String> missingFromConfig = new HashSet<String>(filesInBucket);
        missingFromConfig.removeAll(filesInConfig);

        Set<String> invalidFiles = new HashSet<String>(filesInBucket.size());
        for (String name : filesInBucket) {
            try {
                metadataStore.download(name);
            } catch (MetadataStoreException e) {
                invalidFiles.add(name);
            }
        }

        if (!missingFromBucket.isEmpty() || !missingFromConfig.isEmpty() || !invalidFiles.isEmpty()) {
            return Result.builder().unhealthy()
                .withDetail("missingFromBucket", missingFromBucket)
                .withDetail("missingFromConfig", missingFromConfig)
                .withDetail("invalid", invalidFiles)
                .build();
        } else {
            return Result.healthy();
        }
    }

}