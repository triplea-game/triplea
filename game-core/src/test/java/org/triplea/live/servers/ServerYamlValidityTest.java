package org.triplea.live.servers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.triplea.test.common.StringToInputStream.asInputStream;
import static org.triplea.test.common.TestDataFileReader.readContents;

import feign.template.UriUtils;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import org.hamcrest.core.StringEndsWith;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;
import org.triplea.test.common.TestType;
import org.triplea.util.Version;

/**
 * Checks the current 'servers.yml' to verify it is semantically valid. eg:
 *
 * <ul>
 *   <li>Contains a latest version, greater than 0.0
 *   <li>Inactive versions have null URI and no message
 *   <li>Active versions have a URI and a message
 *   <li>URI does not end with a trailing slash
 *   <li>URI is absolute, it contains protocol and host
 *   <li>No exact version duplicates
 * </ul>
 */
@Integration(type = TestType.ACCEPTANCE)
class ServerYamlValidityTest {
  private static final LiveServers liveServers = new ServerYamlParser().apply(serversYml());

  private static InputStream serversYml() {
    return asInputStream(readContents("servers.yml"));
  }

  @Test
  void verifyLatestVersion() {
    assertThat(liveServers.getLatestEngineVersion(), notNullValue());
    assertThat(liveServers.getLatestEngineVersion().isGreaterThan(new Version("0.0")), is(true));
  }

  @Test
  void atLeastOneActiveServerIsConfigured() {
    assertThat(countActiveServers() > 0, is(true));
  }

  private long countActiveServers() {
    return liveServers.getServers().stream().filter(props -> !props.isInactive()).count();
  }

  @Test
  void uriValuesDoNotEndInSlash() {
    liveServers.getServers().stream()
        .map(ServerProperties::getUri)
        .filter(Objects::nonNull)
        .map(URI::toString)
        .forEach(uriString -> assertThat(uriString, not(StringEndsWith.endsWith("/"))));
  }

  @Test
  void uriValuesAreAbsolute() {
    liveServers.getServers().stream()
        .map(ServerProperties::getUri)
        .filter(Objects::nonNull)
        .map(URI::toString)
        .forEach(uri -> assertThat(UriUtils.isAbsolute(uri), is(true)));
  }

  @Test
  void nonNullUrisAreMarkedAsActive() {
    liveServers.getServers().stream()
        .filter(props -> props.getUri() != null)
        .forEach(props -> assertThat(props.isInactive(), is(false)));
  }

  @Test
  void nullUrisAreMarkedAsInactive() {
    liveServers.getServers().stream()
        .filter(props -> props.getUri() == null)
        .forEach(props -> assertThat(props.isInactive(), is(true)));
  }

  @Test
  void inactiveVersionsDoNotHaveAMessage() {
    liveServers.getServers().stream()
        .filter(ServerProperties::isInactive)
        .forEach(props -> assertThat(props.getMessage(), emptyString()));
  }

  @Test
  void activeVersionsHaveAMessage() {
    liveServers.getServers().stream()
        .filter(props -> !props.isInactive())
        .forEach(props -> assertThat(props.getMessage(), not(emptyString())));
  }

  @Test
  void noVersionDuplicates() {
    final long uniqCount =
        liveServers.getServers().stream()
            .map(ServerProperties::getMinEngineVersion)
            .distinct()
            .count();
    assertThat((long) liveServers.getServers().size(), is(uniqCount));
  }
}
