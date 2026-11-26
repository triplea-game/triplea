package org.triplea.build.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.flow.FlowAction;
import org.gradle.api.flow.FlowParameters;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This plugin sets up listeners to gather test failure information across all projects, and
 * report it at the end of a build.
 */
@SuppressWarnings("UnstableApiUsage") // For FlowAction API usage
public abstract class FailureSummaryPlugin implements Plugin<Settings> {
  @Inject
  protected abstract FlowScope getFlowScope();

  @Override
  public void apply(Settings settings) {
    settings.getGradle().getSharedServices().registerIfAbsent("failuresService", FailedTestsService.class, spec -> {});

    settings.getGradle().allprojects(project -> {
      TestFailureLoggingListener failureListener = project.getObjects().newInstance(TestFailureLoggingListener.class, project.getName());
      project.getTasks().withType(Test.class,  test -> {
        test.addTestListener(failureListener);
      });
    });

    getFlowScope().always(FailureReporter.class, spec -> {});
  }

  /**
   * This listener records all failed tests in the {@link FailedTestsService}, categorizing them by the project
   * that contains them.
   */
  public static abstract class TestFailureLoggingListener implements TestListener {
    private final String projectName;

    @Inject
    public TestFailureLoggingListener(String projectName) {
        this.projectName = projectName;
    }

    @ServiceReference
    public abstract Property<FailedTestsService> getFailedTestsService();

    @Override
    public void beforeSuite(TestDescriptor suite) {}

    @Override
    public void afterSuite(TestDescriptor suite, TestResult result) {}

    @Override
    public void beforeTest(TestDescriptor testDescriptor) {}

    @Override
    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
      if (result.getResultType() == TestResult.ResultType.FAILURE) {
        MapProperty<String, List<String>> failedTestsByProject = getFailedTestsService().get().getFailedTests();

        if (!failedTestsByProject.get().containsKey(projectName)) {
          failedTestsByProject.put(projectName, new ArrayList<>());
        }

        List<String> projectFailures = failedTestsByProject.get().get(projectName);
        projectFailures.add(testDescriptor.getClassName() + "::" + testDescriptor.getName());
      }
    }
  }

  /**
   * {@link FlowAction} that prints sorted information about any failures in a build that
   * have been reported to the {@link FailedTestsService}.
   */
  public static abstract class FailureReporter implements FlowAction<FailureReporter.Params> {
    private static final Logger logger = Logging.getLogger(FailureReporter.class);

    public interface Params extends FlowParameters {
      @ServiceReference
      Property<FailedTestsService> getFailedTestsService();
    }

    @Override
    public void execute(Params parameters) {
      Map<String, List<String>> failedTestsByProject = parameters.getFailedTestsService().get().getFailedTests().get();

      if (!failedTestsByProject.isEmpty()) {
        failedTestsByProject.keySet().stream().sorted().forEach(projectName -> {
          logger.warn("Failed tests for {}:", projectName);
          failedTestsByProject.get(projectName).stream().sorted().forEach(logger::warn);
          logger.warn("");
        });
      }
    }
  }
}
