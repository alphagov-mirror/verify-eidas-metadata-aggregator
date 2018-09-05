package uk.gov.ida.metadataaggregator.managed;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledMetadataAggregator implements Managed {
    private final long hoursBetweenRuns;

    private final MetadataAggregationTaskRunner taskRunner;
    private final ScheduledExecutorService scheduler;

    private ScheduledFuture<?> scheduleFuture;

    @Inject
    public ScheduledMetadataAggregator(
            MetadataAggregationTaskRunner taskRunner,
            @Named("ScheduleFrequency") long hoursBetweenRuns,
            @Named("BlockingExecutor") ScheduledExecutorService scheduler) {
        this.taskRunner = taskRunner;
        this.hoursBetweenRuns = hoursBetweenRuns;
        this.scheduler = scheduler;
    }

    @Override
    public void start() {
        scheduleFuture = scheduler.scheduleWithFixedDelay(taskRunner.scheduled(), 0, hoursBetweenRuns, TimeUnit.HOURS);
    }

    @Override
    public void stop() {
        scheduleFuture.cancel(true);
    }
}
