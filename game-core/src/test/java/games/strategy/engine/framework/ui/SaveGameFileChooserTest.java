package games.strategy.engine.framework.ui;

import static games.strategy.engine.framework.ui.SaveGameFileChooser.EVEN_ROUND_AUTOSAVE_FILE_NAME;
import static games.strategy.engine.framework.ui.SaveGameFileChooser.HEADLESS_AUTOSAVE_FILE_NAME;
import static games.strategy.engine.framework.ui.SaveGameFileChooser.ODD_ROUND_AUTOSAVE_FILE_NAME;
import static games.strategy.engine.framework.ui.SaveGameFileChooser.getEvenRoundAutoSaveFileName;
import static games.strategy.engine.framework.ui.SaveGameFileChooser.getHeadlessAutoSaveFileName;
import static games.strategy.engine.framework.ui.SaveGameFileChooser.getOddRoundAutoSaveFileName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.framework.CliProperties;

final class SaveGameFileChooserTest {
  abstract class AbstractAutoSaveFileNameTestCase {
    static final String HOST_NAME = "hostName";
    static final String PLAYER_NAME = "playerName";

    void givenHostName(final String hostName) {
      System.setProperty(CliProperties.LOBBY_GAME_HOSTED_BY, hostName);
    }

    void givenHostNameNotDefined() {
      System.clearProperty(CliProperties.LOBBY_GAME_HOSTED_BY);
    }

    void givenPlayerName(final String playerName) {
      System.setProperty(CliProperties.TRIPLEA_NAME, playerName);
    }

    void givenPlayerNameNotDefined() {
      System.clearProperty(CliProperties.TRIPLEA_NAME);
    }

    @AfterEach
    void clearSystemProperties() {
      givenHostNameNotDefined();
      givenPlayerNameNotDefined();
    }
  }

  @Nested
  final class GetHeadlessAutoSaveFileNameTest extends AbstractAutoSaveFileNameTestCase {
    @Test
    void shouldPrefixFileNameWithPlayerName() {
      givenPlayerName(PLAYER_NAME);
      givenHostName(HOST_NAME);

      assertThat(getHeadlessAutoSaveFileName(), is(PLAYER_NAME + "_" + HEADLESS_AUTOSAVE_FILE_NAME));
    }

    @Test
    void shouldPrefixFileNameWithHostNameWhenPlayerNameNotDefined() {
      givenPlayerNameNotDefined();
      givenHostName(HOST_NAME);

      assertThat(getHeadlessAutoSaveFileName(), is(HOST_NAME + "_" + HEADLESS_AUTOSAVE_FILE_NAME));
    }

    @Test
    void shouldNotPrefixFileNameWhenPlayerNameNotDefinedAndHostNameNotDefined() {
      givenPlayerNameNotDefined();
      givenHostNameNotDefined();

      assertThat(getHeadlessAutoSaveFileName(), is(HEADLESS_AUTOSAVE_FILE_NAME));
    }
  }

  @Nested
  final class GetEvenRoundAutoSaveFileNameTest extends AbstractAutoSaveFileNameTestCase {
    @Test
    void shouldNotPrefixFileNameWhenHeaded() {
      assertThat(getEvenRoundAutoSaveFileName(false), is(EVEN_ROUND_AUTOSAVE_FILE_NAME));
    }

    @Test
    void shouldPrefixFileNameWithPlayerNameWhenHeadless() {
      givenPlayerName(PLAYER_NAME);
      givenHostName(HOST_NAME);

      assertThat(getEvenRoundAutoSaveFileName(true), is(PLAYER_NAME + "_" + EVEN_ROUND_AUTOSAVE_FILE_NAME));
    }

    @Test
    void shouldPrefixFileNameWithHostNameWhenHeadlessAndPlayerNameNotDefined() {
      givenPlayerNameNotDefined();
      givenHostName(HOST_NAME);

      assertThat(getEvenRoundAutoSaveFileName(true), is(HOST_NAME + "_" + EVEN_ROUND_AUTOSAVE_FILE_NAME));
    }

    @Test
    void shouldNotPrefixFileNameWhenHeadlessAndPlayerNameNotDefinedAndHostNameNotDefined() {
      givenPlayerNameNotDefined();
      givenHostNameNotDefined();

      assertThat(getEvenRoundAutoSaveFileName(true), is(EVEN_ROUND_AUTOSAVE_FILE_NAME));
    }
  }

  @Nested
  final class GetOddRoundAutoSaveFileNameTest extends AbstractAutoSaveFileNameTestCase {
    @Test
    void shouldNotPrefixFileNameWhenHeaded() {
      assertThat(getOddRoundAutoSaveFileName(false), is(ODD_ROUND_AUTOSAVE_FILE_NAME));
    }

    @Test
    void shouldPrefixFileNameWithPlayerNameWhenHeadless() {
      givenPlayerName(PLAYER_NAME);
      givenHostName(HOST_NAME);

      assertThat(getOddRoundAutoSaveFileName(true), is(PLAYER_NAME + "_" + ODD_ROUND_AUTOSAVE_FILE_NAME));
    }

    @Test
    void shouldPrefixFileNameWithHostNameWhenHeadlessAndPlayerNameNotDefined() {
      givenPlayerNameNotDefined();
      givenHostName(HOST_NAME);

      assertThat(getOddRoundAutoSaveFileName(true), is(HOST_NAME + "_" + ODD_ROUND_AUTOSAVE_FILE_NAME));
    }

    @Test
    void shouldNotPrefixFileNameWhenHeadlessAndPlayerNameNotDefinedAndHostNameNotDefined() {
      givenPlayerNameNotDefined();
      givenHostNameNotDefined();

      assertThat(getOddRoundAutoSaveFileName(true), is(ODD_ROUND_AUTOSAVE_FILE_NAME));
    }
  }
}
