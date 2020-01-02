package org.triplea.live.servers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.test.common.StringToInputStream.asInputStream;
import static org.triplea.test.common.TestDataFileReader.readContents;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.util.Version;

class ServerYamlParserTest {

  private final ServerYamlParser serverYamlParser = new ServerYamlParser();

  private static InputStream sampleFileInput() {
    return asInputStream(readContents("live.servers.yaml.examples/servers_example.yaml"));
  }

  @MethodSource
  @ParameterizedTest
  void invalidInputs(final InputStream inputStream) {
    assertThrows(IllegalArgumentException.class, () -> serverYamlParser.apply(inputStream));
  }

  @SuppressWarnings("unused")
  private static List<InputStream> invalidInputs() {
    return List.of(asInputStream(""), asInputStream("-\n"));
  }

  @Test
  void parsingLatestVersion() {
    final LiveServers liveServers = serverYamlParser.apply(sampleFileInput());

    assertThat(liveServers.getLatestEngineVersion(), is(new Version("1.0.300")));
  }

  @Test
  void parsingProperties() {
    final LiveServers liveServers = serverYamlParser.apply(sampleFileInput());

    assertThat(liveServers.getServers(), hasSize(3));
    assertThat(liveServers.getServers().get(0).getMinEngineVersion(), is(new Version("2.0")));
    assertThat(
        liveServers.getServers().get(0).getUri(),
        is(URI.create("https://prerelease.triplea-game.org")));
    assertThat(liveServers.getServers().get(0).getMessage(), is("Welcome to prerelease\n"));
    assertThat(liveServers.getServers().get(0).isInactive(), is(false));

    assertThat(liveServers.getServers().get(1).getMinEngineVersion(), is(new Version("1.0.154")));
    assertThat(
        liveServers.getServers().get(1).getUri(), is(URI.create("https://lobby.triplea-game.org")));
    assertThat(liveServers.getServers().get(1).getMessage(), is("Welcome to production\n"));
    assertThat(liveServers.getServers().get(1).isInactive(), is(false));

    assertThat(liveServers.getServers().get(2).getMinEngineVersion(), is(new Version("0.0")));
    assertThat(liveServers.getServers().get(2).getUri(), nullValue());
    assertThat(liveServers.getServers().get(2).getMessage(), emptyString());
    assertThat(liveServers.getServers().get(2).isInactive(), is(true));
  }
}
