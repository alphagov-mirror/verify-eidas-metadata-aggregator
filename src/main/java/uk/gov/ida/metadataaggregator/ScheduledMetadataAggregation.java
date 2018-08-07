package uk.gov.ida.metadataaggregator;

import java.util.Timer;
import java.util.TimerTask;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import io.dropwizard.lifecycle.Managed;

class ScheduledMetadataAggregator implements Managed {
    private final Timer timer;
    private final Duration timeBetweenRuns;

    private final MetadataAggregationTaskRunner taskRunner;

    @Inject
    public ScheduledMetadataAggregator(
    MetadataAggregationTaskRunner taskRunner,
    @Named("ScheduleFrequency") Duration timeBetweenRuns) {
        this.taskRunner = taskRunner;
        this.timeBetweenRuns = timeBetweenRuns;
        this.timer = new Timer();
    }

    @Override
    public void start() throws Exception {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                taskRunner.run("Scheduled");
            }
         }, DateTime.now().toDate(), timeBetweenRuns.getMillis());
    }

    @Override
    public void stop() throws Exception {
        timer.cancel();
    }
}