package org.triplea.live.servers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.util.Version;

/**
 * This test runs through various configurations and verifies that the right 'version' is selected.
 * The rule for selecting version is we want the greatest available version that is less than or
 * equal to the current version.
 *
 * <p>For context, general expectations around server configuration would be:
 *
 * <pre>
 * - version: 2.0.500  // release version
 * - version: 2.0.0    // prerelease version
 * - version 0.0       // fallback to tell users to upgrade
 *   inactive: true
 * </pre>
 *
 * <p>If we are doing a non-compatible release, the versioning config should look like:
 *
 * <pre>
 * - version: 3.0.0    // prerelease version
 * - version: 2.0.500  // release version
 * - version 0.0       // fallback to tell users to upgrade
 *   inactive: true
 * </pre>
 */
@SuppressWarnings("TypeMayBeWeakened")
class CurrentVersionSelectorTest {

  @Test
  @DisplayName("Verify illegal arg when there is no server list, represents a parse error")
  void noServersErrorCondition() {
    final var currentVersionSelector = new CurrentVersionSelector(new Version("1.0"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            currentVersionSelector.apply(
                LiveServers.builder()
                    .latestEngineVersion(new Version("10.0"))
                    .servers(List.of())
                    .build()));
  }

  /**
   * The configured versions all being less than current version is considered an error case. We
   * expect this to be handled by having a min version that is an inactive server. This way users
   * will get a notification that their version of TripleA is out of date.
   */
  @Test
  @DisplayName("Verify illegal config where current version is less than all configured versions")
  void noMinVersionErrorCondition() {
    final var currentVersionSelector = new CurrentVersionSelector(new Version("1.0"));

    final var liveServers =
        LiveServers.builder()
            .latestEngineVersion(new Version("10.0"))
            .servers(List.of(serverPropertiesWithVersion("10.0")))
            .build();

    assertThrows(IllegalArgumentException.class, () -> currentVersionSelector.apply(liveServers));
  }

  private static ServerProperties serverPropertiesWithVersion(final String version) {
    return ServerProperties.builder()
        .message("message")
        .uri(URI.create("http://uri.com"))
        .minEngineVersion(new Version(version))
        .build();
  }

  @ParameterizedTest
  @MethodSource
  @DisplayName("Verify selection is the max of server versions that are less than current version")
  void verifyMinVersion(
      final String currentVersion,
      final List<ServerProperties> servers,
      final Version expectedVersion) {

    final var currentVersionSelector = new CurrentVersionSelector(new Version(currentVersion));
    final var liveServers =
        LiveServers.builder().latestEngineVersion(new Version("10.0")).servers(servers).build();

    final Version versionResult = currentVersionSelector.apply(liveServers).getMinEngineVersion();

    assertThat(versionResult, is(expectedVersion));
  }

  /** Each argument is a triplet: {current_version} {test values} {expected_version} */
  @SuppressWarnings("unused")
  private static List<Arguments> verifyMinVersion() {
    return List.of(
        // single version to select from centered at 0.0
        Arguments.of("0.0", List.of(serverPropertiesWithVersion("0.0")), "0.0"),
        Arguments.of("0.0.0", List.of(serverPropertiesWithVersion("0.0")), "0.0"),
        Arguments.of("1.0.0", List.of(serverPropertiesWithVersion("0.0")), "0.0"),
        Arguments.of("0.0.1", List.of(serverPropertiesWithVersion("0.0")), "0.0"),
        Arguments.of("0.0.1", List.of(serverPropertiesWithVersion("0.0.0")), "0.0.0"),
        // variants of selecting a min version below 10.0.0
        Arguments.of(
            "10.0.0",
            List.of(
                serverPropertiesWithVersion("0.0.0"),
                serverPropertiesWithVersion("10.0.0"),
                serverPropertiesWithVersion("10.0.1")),
            "10.0.0"),
        Arguments.of(
            "10.0.0",
            List.of(
                serverPropertiesWithVersion("0.0.0"),
                serverPropertiesWithVersion("9.8.0"),
                serverPropertiesWithVersion("10.0.1")),
            "9.8.0"),
        Arguments.of(
            "10.0.10",
            List.of(
                serverPropertiesWithVersion("10.0.0"),
                serverPropertiesWithVersion("10.0.5"),
                serverPropertiesWithVersion("10.0.20")),
            "10.0.5"),
        // verify if major version is not matching that we can get a right value
        Arguments.of(
            "10.0.10",
            List.of(serverPropertiesWithVersion("9.0.0"), serverPropertiesWithVersion("11.1.5")),
            "9.0.0"),
        // verify least minor version can be selected
        Arguments.of(
            "10.10.0",
            List.of(serverPropertiesWithVersion("10.0.0"), serverPropertiesWithVersion("10.20.0")),
            "10.0.0"),
        // verify least minor version with exact match
        Arguments.of(
            "10.10.0",
            List.of(serverPropertiesWithVersion("10.0.0"), serverPropertiesWithVersion("10.10.0")),
            "10.10.0"),
        Arguments.of(
            "10.20.0",
            List.of(serverPropertiesWithVersion("10.0.0"), serverPropertiesWithVersion("10.10.0")),
            "10.10.0"),
        // larger number with major version
        Arguments.of(
            "90.0",
            List.of(serverPropertiesWithVersion("0.0"), serverPropertiesWithVersion("5.0")),
            "5.0"),
        Arguments.of("90.0", List.of(serverPropertiesWithVersion("0.0")), "0.0"),
        Arguments.of(
            "90.0",
            List.of(serverPropertiesWithVersion("0.0"), serverPropertiesWithVersion("100.0")),
            "0.0"),
        Arguments.of(
            "100.0",
            List.of(serverPropertiesWithVersion("0.0"), serverPropertiesWithVersion("100.0")),
            "100.0"),
        Arguments.of(
            "110.0",
            List.of(serverPropertiesWithVersion("0.0"), serverPropertiesWithVersion("100.0")),
            "100.0"),
        // selection around point version
        Arguments.of(
            "100.0",
            List.of(serverPropertiesWithVersion("0.0"), serverPropertiesWithVersion("99.9.9")),
            "99.9.9"),
        Arguments.of(
            "100.0",
            List.of(
                serverPropertiesWithVersion("0.0"),
                serverPropertiesWithVersion("99.9.9"),
                serverPropertiesWithVersion("100.0")),
            "100.0"),
        Arguments.of(
            "100.0",
            List.of(
                serverPropertiesWithVersion("0.0"),
                serverPropertiesWithVersion("99.9.9"),
                serverPropertiesWithVersion("100.0.1")),
            "99.9.9"),
        Arguments.of(
            "0.0.10",
            List.of(serverPropertiesWithVersion("0.0"), serverPropertiesWithVersion("0.0.5")),
            "0.0.5"));
  }
}
