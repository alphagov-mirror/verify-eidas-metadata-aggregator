package uk.gov.ida.metadataaggregator.apprule;

import io.dropwizard.lifecycle.JettyManaged;
import io.dropwizard.setup.Environment;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.metadataaggregator.managed.ScheduledMetadataAggregator;

import static org.assertj.core.api.Assertions.assertThat;

public class SchedulingTest {

    @ClassRule
    public static final MetadataAggregatorAppRule RULE = new MetadataAggregatorAppRule();

    @Test
    public void shouldRegisterSchedulerToLifecycle() {
        Environment environment = RULE.getEnvironment();
        boolean metadataIsScheduled = environment.lifecycle().getManagedObjects()
                .stream()
                .anyMatch(lifeCycle ->
                        lifeCycle instanceof JettyManaged
                        && ((JettyManaged) lifeCycle).getManaged() instanceof ScheduledMetadataAggregator
                );
        assertThat(metadataIsScheduled).isTrue();
    }
}
