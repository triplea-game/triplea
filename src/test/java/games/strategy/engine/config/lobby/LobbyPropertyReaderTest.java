package games.strategy.engine.config.lobby;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.experimental.extensions.TemporaryFolder;
import org.junit.experimental.extensions.TemporaryFolderExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TemporaryFolderExtension.class)
public class LobbyPropertyReaderTest {
  private LobbyPropertyReader testObj;

  /**
   * Sets up an example lobby property file with some fake values, then sets up a property reader test object
   * pointed at this file.
   */
  @BeforeEach
  public void setup(TemporaryFolder tempFolderRule) throws IOException {
    final File testFile = tempFolderRule.newFile("testname");

    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write(keyValuePair(LobbyPropertyReader.PropertyKeys.port, String.valueOf(TestData.fakePort)));
      writer.write(keyValuePair(LobbyPropertyReader.PropertyKeys.postgresUser, TestData.fakeUser));
      writer.write(keyValuePair(LobbyPropertyReader.PropertyKeys.postgresPassword, TestData.fakePassword));
    }

    testObj = new LobbyPropertyReader(testFile);
  }

  private static String keyValuePair(final String key, final String value) {
    return key + "=" + value + "\n";
  }

  @Test
  public void getPort() throws Exception {
    assertThat(testObj.getPort(), is(TestData.fakePort));

  }

  @Test
  public void postgresUser() throws Exception {
    assertThat(testObj.getPostgresUser(), is(TestData.fakeUser));
  }

  @Test
  public void postgresPassword() throws Exception {
    assertThat(testObj.getPostgresPassword(), is(TestData.fakePassword));
  }

  private interface TestData {
    int fakePort = 100;
    String fakeUser = "funnyName";
    String fakePassword = "funnyPasssword";
  }
}
