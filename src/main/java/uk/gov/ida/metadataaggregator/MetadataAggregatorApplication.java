package uk.gov.ida.metadataaggregator;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;

public class MetadataAggregatorApplication extends Application<AggregatorConfig> {

    private GuiceBundle<AggregatorConfig> guiceBundle;

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
    public void initialize(Bootstrap<AggregatorConfig> bootstrap) {
        super.initialize(bootstrap);

        guiceBundle = GuiceBundle.defaultBuilder(AggregatorConfig.class)
            .modules(new MetadataAggregatorModule())
            .build();

        bootstrap.addBundle(guiceBundle);
        bootstrap.addCommand(guiceBundle.getInjector().getInstance(MetadataAggregatorCommand.class));
    }

    @Override
    public final void run(AggregatorConfig configuration, Environment environment) {
      environment.getObjectMapper().setDateFormat(StdDateFormat.getDateInstance());

      environment.lifecycle().manage(guiceBundle.getInjector().getInstance(ScheduledMetadataAggregator.class));
    }
}