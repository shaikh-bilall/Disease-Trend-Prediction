package diseaseTrendPrediction;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;

public class RunAllTests {

    public static void main(String[] args) {
        // Discovers every test class under this package,
        // so tests added later are picked up automatically.
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClasspathRoots(Set.of(Path.of("target/test-classes"))))
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);

        TestExecutionSummary summary = listener.getSummary();
        long found = summary.getTestsFoundCount();
        long passed = summary.getTestsSucceededCount();
        long failed = summary.getTestsFailedCount();
        long skipped = summary.getTestsSkippedCount();
        long aborted = summary.getTestsAbortedCount();
        long millis = summary.getTimeFinished() - summary.getTimeStarted();

        if (!summary.getFailures().isEmpty()) {
            System.out.println("========== FAILURES ==========");
            for (TestExecutionSummary.Failure failure : summary.getFailures()) {
                System.out.println("FAIL: " + failure.getTestIdentifier().getDisplayName());
                System.out.println("      " + failure.getException().getMessage());
            }
            System.out.println();
        }

        System.out.println("========== SUMMARY ==========");
        System.out.printf("Total:   %d%n", found);
        System.out.printf("Passed:  %d%n", passed);
        System.out.printf("Failed:  %d%n", failed);
        System.out.printf("Skipped: %d%n", skipped);
        System.out.printf("Aborted: %d%n", aborted);
        System.out.printf("Time:    %d ms%n", millis);
        System.out.println(
                failed == 0 && aborted == 0 ?
                "RESULT: ALL TESTS PASSED" :
                "RESULT: SOME TESTS FAILED"
        );

        System.exit(failed == 0 && aborted == 0 ? 0 : 1);
    }

}
