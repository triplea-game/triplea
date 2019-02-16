package games.strategy.engine.lobby.client.login;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.triplea.util.Version;

class LobbyPropertyFileParserTest {

  private static TestProps[] testDataSet() {
    final TestProps testProps1 = new TestProps();
    testProps1.host = TestData.host;
    testProps1.port = TestData.port;
    testProps1.errorMessage = TestData.errorMessage;
    testProps1.message = TestData.message;
    testProps1.version = TestData.version0;
    testProps1.httpHostUri = TestData.httpHostUri;

    final TestProps testProps2 = new TestProps();
    testProps2.host = TestData.host;
    testProps2.port = TestData.port;
    testProps2.errorMessage = TestData.errorMessage;
    testProps2.message = TestData.message;
    testProps2.version = TestData.version1;
    testProps2.httpHostUri = TestData.httpHostUri;

    final TestProps testPropsMatch = new TestProps();
    testPropsMatch.host = TestData.hostOther;
    testPropsMatch.port = TestData.portOther;
    testPropsMatch.errorMessage = "";
    testPropsMatch.message = "";
    testPropsMatch.version = TestData.clientCurrentVersion;
    testPropsMatch.httpHostUri = TestData.httpHostUri;

    return new TestProps[] {
        testProps1, testProps2, testPropsMatch
    };
  }

  /**
   * Just one set of values in a config file with no verson. The props we return should be a pretty
   * straight forward 1:1
   */
  @Test
  void parseWithSimpleCase() {
    final TestProps testProps = new TestProps();
    testProps.host = TestData.host;
    testProps.port = TestData.port;
    testProps.httpHostUri = TestData.httpHostUri;
    testProps.errorMessage = TestData.errorMessage;
    testProps.message = TestData.message;
    testProps.version = TestData.clientCurrentVersion;

    final InputStream stream = newYaml(testProps);

    final LobbyServerProperties result =
        LobbyPropertyFileParser.parse(stream, new Version(TestData.clientCurrentVersion));
    assertThat(result.getHost(), is(TestData.host));
    assertThat(result.getPort(), is(Integer.valueOf(TestData.port)));
    assertThat(result.getHttpServerUri(), is(TestData.httpHostUri));
    assertThat(result.getServerMessage(), isPresentAndIs(TestData.message));
    assertThat(result.getServerErrorMessage(), isPresentAndIs(TestData.errorMessage));
  }

  private static InputStream newYaml(final TestProps... testProps) {
    return new ByteArrayInputStream(Arrays.stream(testProps)
        .map(TestProps::toYaml)
        .collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * YAML config has multple lobby configs depending on client version. Here we make sure the version checks
   * line up and we get the expected lobby config back.
   */
  @Test
  void checkVersionSelection() {
    final InputStream stream = newYaml(testDataSet());

    final LobbyServerProperties result =
        LobbyPropertyFileParser.parse(stream, new Version(TestData.clientCurrentVersion));

    assertThat(result.getHost(), is(TestData.hostOther));
    assertThat(result.getPort(), is(Integer.valueOf(TestData.portOther)));
    assertThat(result.getServerMessage(), isEmpty());
    assertThat(result.getServerErrorMessage(), isEmpty());
  }

  private interface TestData {
    String port = "521";
    String portOther = "4141";
    String host = "host";
    String hostOther = "another_host";
    URI httpHostUri = URI.create("hostDomain.com:1255");
    String message = "message";
    String errorMessage = "err err err test message";
    String version0 = "0.0.0.0";
    String version1 = "1.0.0.0";
    String clientCurrentVersion = "2.0.0.0";
  }

  /**
   * Simple struct-like object to keep our test data together and form a YAML more easily.
   */
  private static class TestProps {
    String host;
    String port;
    URI httpHostUri;
    String message;
    String errorMessage;
    String version;

    String toYaml() {
      final String printVersion = (version == null) ? "" : ("version: \"" + version + "\"");
      return String.format(
          "- %s: %s\n"
              + "  %s: %s\n"
              + "  %s: %s\n"
              + "  %s: %s\n"
              + "  %s: %s\n"
              + "  %s\n",
          LobbyPropertyFileParser.YAML_HOST,
          host,
          LobbyPropertyFileParser.YAML_PORT,
          port,
          LobbyPropertyFileParser.YAML_HTTP_SERVER_URI,
          httpHostUri,
          LobbyPropertyFileParser.YAML_MESSAGE,
          message,
          LobbyPropertyFileParser.YAML_ERROR_MESSAGE,
          errorMessage,
          printVersion);
    }
  }
}
