package org.triplea.build.plugins;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.util.List;

/**
 * This service stores information about failed tests in the build.
 */
public interface FailedTestsService extends BuildService<BuildServiceParameters.None> {
    /**
     * Map from Project Name -> List of failed tests in that project.
     *
     * @return map as described
     */
    MapProperty<String, List<String>> getFailedTests();
}
