package uk.gov.ida.metadataaggregator.healthcheck;

import java.util.concurrent.atomic.AtomicReference;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;

import uk.gov.ida.metadataaggregator.core.StatusReport;

public class AggregationStatusHealthCheck extends HealthCheck {

    private AtomicReference<StatusReport> lastReportRef;

    @Inject
    public AggregationStatusHealthCheck(AtomicReference<StatusReport> lastReportRef) {
        this.lastReportRef = lastReportRef;
    }

    @Override
    public Result check() throws Exception {
        StatusReport report = lastReportRef.get();
        if (report == null) return Result.unhealthy("No refresh has been run yet");
        ResultBuilder healthCheckResult = report.wasSuccessful() ?
            Result.builder().healthy() :
            Result.builder().unhealthy();
        return healthCheckResult
            .withDetail("runAt", report.getRunAt())
            .withDetail("errors", report.getErrors())
            .build();
    }
}
