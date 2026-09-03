package games.strategy.engine.framework;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.xml.TestMapGameData;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.triplea.io.IoUtils;

/**
 * End-to-end plumbing test for the text save format. At this point no {@link
 * games.strategy.engine.data.serializer.TextSaver} is registered, so every section is a legacy blob;
 * this proves the sentinel/header/gameData/delegate framing, format auto-detection, and the
 * reflective delegate reconstruction all work before any native saver exists.
 */
class TextGameDataRoundTripTest extends AbstractClientSettingTestCase {

  // A real game map (with a real BattleDelegate) so the full save/load path — including
  // GameData.fixUpNullPlayersInDelegates — round-trips as it would in production.
  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void textPipelineRoundTripsAllBlobSections() {
    assertThat(
        "fixture should have delegates to exercise",
        gameData.getDelegates(),
        hasSize(greaterThan(0)));

    final GameData reloaded = GameDataOracle.assertReconstructs(gameData);

    assertThat(reloaded.getGameName(), is(gameData.getGameName()));
    assertThat(reloaded.getDelegates(), hasSize(gameData.getDelegates().size()));
  }

  @Test
  void writeFlagRoutesSaveGameToTextFormatAndReadAutoDetects() throws Exception {
    final byte[] legacyBytes = IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, gameData));

    ClientSetting.writeTextSaveFormat.setValue(true);
    final byte[] textBytes = IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, gameData));

    assertThat("flag should route saveGame to the text format", decompressHead(textBytes),
        startsWith("TRIPLEA-JSONL"));

    final GameData reloaded =
        IoUtils.readFromMemory(textBytes, GameDataManager::loadGame).orElseThrow();
    assertThat(reloaded.getGameName(), is(gameData.getGameName()));

    // legacy bytes must still auto-detect and load.
    final GameData reloadedLegacy =
        IoUtils.readFromMemory(legacyBytes, GameDataManager::loadGame).orElseThrow();
    assertThat(reloadedLegacy.getGameName(), is(gameData.getGameName()));
  }

  private static String decompressHead(final byte[] gzipped) throws Exception {
    try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
      return new String(gz.readNBytes(16), StandardCharsets.UTF_8);
    }
  }
}
