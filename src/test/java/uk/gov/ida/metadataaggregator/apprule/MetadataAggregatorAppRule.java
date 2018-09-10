package uk.gov.ida.metadataaggregator.apprule;

import certificates.values.CACertificates;
import com.amazonaws.regions.Regions;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import uk.gov.ida.metadataaggregator.MetadataAggregatorApplication;
import uk.gov.ida.metadataaggregator.MetadataAggregatorConfiguration;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class MetadataAggregatorAppRule extends DropwizardAppRule<MetadataAggregatorConfiguration> {

    private static final KeyStoreResource trustStore =
            KeyStoreResourceBuilder.aKeyStoreResource()
                    .withCertificate("idpCA", CACertificates.TEST_IDP_CA)
                    .withCertificate("rootCA", CACertificates.TEST_ROOT_CA).build();

    public MetadataAggregatorAppRule(ConfigOverride... otherConfigOverrides) {
        super(MetadataAggregatorApplication.class,
                ResourceHelpers.resourceFilePath("apprule/metadata-aggregator.yml"),
                configOverrides(otherConfigOverrides));
    }

    private static ConfigOverride[] configOverrides(ConfigOverride... otherConfigOverrides) {
        List<ConfigOverride> overrides = Stream.of(
                ConfigOverride.config("trustStore.type", "file"),
                ConfigOverride.config("trustStore.store", trustStore.getAbsolutePath()),
                ConfigOverride.config("trustStore.password", trustStore.getPassword()),
                ConfigOverride.config("awsRegion", pickARandomRegion().getName())
        ).collect(Collectors.toList());

        overrides.addAll(asList(otherConfigOverrides));

        return overrides.toArray(new ConfigOverride[0]);
    }

    private static Regions pickARandomRegion() {
        Regions[] allAwsRegions = Regions.values();
        int randomRegionIdx = new Random().nextInt(allAwsRegions.length);
        return allAwsRegions[randomRegionIdx];
    }

    @Override
    protected void before() {
        trustStore.create();
        super.before();
    }
}
