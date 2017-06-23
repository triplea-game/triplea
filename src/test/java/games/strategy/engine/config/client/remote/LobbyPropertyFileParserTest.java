package games.strategy.engine.config.client.remote;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.util.Version;

public class LobbyPropertyFileParserTest {

  private LobbyPropertyFileParser testObj;

  private static TestProps[] testDataSet() {
    final TestProps testProps1 = new TestProps();
    testProps1.host = TestData.host;
    testProps1.port = TestData.port;
    testProps1.errorMessage = TestData.errorMessage;
    testProps1.message = TestData.message;
    testProps1.version = TestData.version0;


    final TestProps testProps2 = new TestProps();
    testProps2.host = TestData.host;
    testProps2.port = TestData.port;
    testProps2.errorMessage = TestData.errorMessage;
    testProps2.message = TestData.message;
    testProps2.version = TestData.version1;

    final TestProps testPropsMatch = new TestProps();
    testPropsMatch.host = TestData.hostOther;
    testPropsMatch.port = TestData.portOther;
    testPropsMatch.errorMessage = "";
    testPropsMatch.message = "";
    testPropsMatch.version = TestData.clientCurrentVersion;

    return new TestProps[] {
        testProps1, testProps2, testPropsMatch
    };
  }

  @Before
  public void setup() {
    testObj = new LobbyPropertyFileParser();
  }

  /**
   * Just one set of values in a config file with no verson. The props we return should be a pretty
   * straight forward 1:1
   */
  @Test
  public void parseWithSimpleCase() throws Exception {
    final TestProps testProps = new TestProps();
    testProps.host = TestData.host;
    testProps.port = TestData.port;
    testProps.errorMessage = TestData.errorMessage;
    testProps.message = TestData.message;

    final File testFile = createTempFile(testProps);

    final LobbyServerProperties result = testObj.parse(testFile, new Version(TestData.clientCurrentVersion));
    assertThat(result.host, is(TestData.host));
    assertThat(result.port, is(Integer.valueOf(TestData.port)));
    assertThat(result.serverMessage, is(TestData.message));
    assertThat(result.serverErrorMessage, is(TestData.errorMessage));
  }

  private File createTempFile(final TestProps... testProps) throws Exception {
    final File f = File.createTempFile("testing", ".tmp");
    for (final TestProps testProp : Arrays.asList(testProps)) {
      try (FileWriter writer = new FileWriter(f)) {
        writer.write(testProp.toYaml());
      }
    }
    f.deleteOnExit();
    return f;
  }

  /**
   * YAML config has multple lobby configs depending on client version. Here we make sure the version checks
   * line up and we get the expected lobby config back.
   */
  @Test
  public void checkVersionSelection() throws Exception {
    final File testFile = createTempFile(testDataSet());

    final LobbyServerProperties result = testObj.parse(testFile, new Version(TestData.clientCurrentVersion));

    assertThat(result.host, is(TestData.hostOther));
    assertThat(result.port, is(Integer.valueOf(TestData.portOther)));
    assertThat(result.serverMessage, is(""));
    assertThat(result.serverErrorMessage, is(""));
  }

  private interface TestData {
    String port = "521";
    String portOther = "4141";
    String host = "host";
    String hostOther = "another_host";
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
    String message;
    String errorMessage;
    String version;

    String toYaml() {
      final String printVersion = (version == null) ? "" : "  version: " + version + "\n";
      return "- host: " + host + "\n"
          + printVersion
          + "  port: " + port + "\n"
          + "  message: " + message + "\n"
          + "  error_message: " + errorMessage + "\n";
    }
  }
}
