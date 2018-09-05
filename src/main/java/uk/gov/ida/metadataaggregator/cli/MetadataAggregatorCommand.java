package uk.gov.ida.metadataaggregator.cli;

import com.google.inject.Inject;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import uk.gov.ida.metadataaggregator.MetadataAggregatorConfiguration;
import uk.gov.ida.metadataaggregator.managed.MetadataAggregationTaskRunner;

public class MetadataAggregatorCommand extends ConfiguredCommand<MetadataAggregatorConfiguration> {
    private static final String NAME = "aggregate";
    private static final String DESCRIPTION = "Manually performs a single run of metadata aggregation using the supplied config.";

    private final MetadataAggregationTaskRunner taskRunner;

    @Inject
    public MetadataAggregatorCommand(MetadataAggregationTaskRunner taskRunner) {
        super(NAME, DESCRIPTION);
        this.taskRunner = taskRunner;
    }

    @Override
    protected void run(Bootstrap<MetadataAggregatorConfiguration> bootstrap, Namespace namespace, MetadataAggregatorConfiguration configuration) {
        taskRunner.run("Manual");
    }
}
