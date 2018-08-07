package uk.gov.ida.metadataaggregator;

import com.google.inject.Inject;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;

class MetadataAggregatorCommand extends ConfiguredCommand<AggregatorConfig> {
    public static final String NAME = "aggregate";
    public static final String DESCRIPTION = "Manually performs a single run of metadata aggregation using the supplied config.";

    private final MetadataAggregationTaskRunner taskRunner;

    @Inject
    public MetadataAggregatorCommand(MetadataAggregationTaskRunner taskRunner) {
        super(NAME, DESCRIPTION);
        this.taskRunner = taskRunner;
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
    }

    @Override
    protected void run(Bootstrap<AggregatorConfig> bootstrap, Namespace namespace, AggregatorConfig configuration)
    throws Exception {
        taskRunner.run("Manual");
    }
}