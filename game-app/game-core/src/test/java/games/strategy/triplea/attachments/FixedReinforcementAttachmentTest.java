package games.strategy.triplea.attachments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixedReinforcementAttachmentTest {
  private final GameData data = new GameData();
  private final GamePlayer player = new GamePlayer("Allies", data);
  private final FixedReinforcementAttachment attachment =
      new FixedReinforcementAttachment("fixedReinforcementAttachment", player, data);

  @BeforeEach
  void setUp() {
    data.getPlayerList().addPlayerId(player);
    data.getMap().addTerritory(new Territory("Front", data));
    final UnitType infantry = new UnitType("infantry", data);
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);
    player.addAttachment("fixedReinforcementAttachment", attachment);
  }

  @Test
  void parsesRepeatedRulesInDeclarationOrder() throws Exception {
    attachment.setReinforcement("1:Front:infantry:3");
    attachment.setReinforcement("2:Front:infantry:1");

    assertThat(attachment.getReinforcements())
        .containsExactly(
            new FixedReinforcementRule(1, "Front", "infantry", 3),
            new FixedReinforcementRule(2, "Front", "infantry", 1));
    assertThat(FixedReinforcementAttachment.get(player)).containsSame(attachment);
    assertThat(attachment.getPropertyOrEmpty("reinforcement")).isPresent();
  }

  @Test
  void rejectsMalformedOrUnknownTargets() {
    assertThatThrownBy(() -> attachment.setReinforcement("0:Front:infantry:1"))
        .isInstanceOf(GameParseException.class);
    assertThatThrownBy(() -> attachment.setReinforcement("1:Missing:infantry:1"))
        .isInstanceOf(GameParseException.class);
    assertThatThrownBy(() -> attachment.setReinforcement("1:Front:missing:1"))
        .isInstanceOf(GameParseException.class);
    assertThatThrownBy(() -> attachment.setReinforcement("1:Front:infantry"))
        .isInstanceOf(GameParseException.class);
  }
}
