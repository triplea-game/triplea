package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ProRetreatAiTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();

  @Test
  void capitalWhoseCaptureKeepsItsIncomeIsWorthItsFactoryOnly()
      throws MutableProperty.InvalidValueException {
    final PlayerAttachment pa =
        new PlayerAttachment(Constants.PLAYER_ATTACHMENT_NAME, germans(gameData), gameData);
    pa.getPropertyOrThrow("retainCapitalProduceNumber").setValue(0);
    germans(gameData).addAttachment(Constants.PLAYER_ATTACHMENT_NAME, pa);

    assertThat(
            ProRetreatAi.captureValue(territory("Germany", gameData), british(gameData), gameData))
        .isEqualTo(2.0 * 10 * 2);
  }
}
