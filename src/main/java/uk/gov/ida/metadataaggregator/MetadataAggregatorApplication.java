package uk.gov.ida.metadataaggregator;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.ida.metadataaggregator.managed.ScheduledMetadataAggregator;

public class MetadataAggregatorApplication extends Application<MetadataAggregatorConfiguration> {

    private GuiceBundle<MetadataAggregatorConfiguration> guiceBundle;

    public static void main(String[] args) {
        // running this method here stops the odd exceptions/double-initialisation that happens without it
        // - it's the same fix that was required in the tests...
        JerseyGuiceUtils.reset();

        try {
            new MetadataAggregatorApplication().run(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(Bootstrap<MetadataAggregatorConfiguration> bootstrap) {
        super.initialize(bootstrap);

        guiceBundle = GuiceBundle.defaultBuilder(MetadataAggregatorConfiguration.class)
                .modules(new MetadataAggregatorModule())
                .build();

        bootstrap.addBundle(guiceBundle);
    }

    @Override
    public final void run(MetadataAggregatorConfiguration configuration, Environment environment) {
        environment.getObjectMapper().setDateFormat(StdDateFormat.getDateInstance());
        environment.lifecycle().manage(guiceBundle.getInjector().getInstance(ScheduledMetadataAggregator.class));
    }
}
