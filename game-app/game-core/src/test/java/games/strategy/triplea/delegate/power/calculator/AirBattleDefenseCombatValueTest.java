package games.strategy.triplea.delegate.power.calculator;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AirBattleDefenseCombatValueTest {

  @Nested
  class AirBattleDefenseStrengthTest {

    @Test
    void calculatesValue() {
      final GameData gameData = givenGameData().withDiceSides(6).build();

      final GamePlayer player = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setAirDefense(3);

      final AirBattleDefenseCombatValue.AirBattleDefenseStrength strength =
          new AirBattleDefenseCombatValue.AirBattleDefenseStrength(6);
      assertThat("Air defense is 3", strength.getStrength(unit).getValue(), is(3));
    }

    @Test
    void limitsToDiceSides() {
      final GameData gameData = givenGameData().withDiceSides(6).build();

      final GamePlayer player = mock(GamePlayer.class);

      final UnitType unitType = new UnitType("test", gameData);
      final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
      unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
      final Unit unit = unitType.createTemp(1, player).get(0);
      unit.getUnitAttachment().setAirDefense(8);

      final AirBattleDefenseCombatValue.AirBattleDefenseStrength strength =
          new AirBattleDefenseCombatValue.AirBattleDefenseStrength(6);
      assertThat(
          "Air defense is 8 but dice sides is 6 so it is limited to 6",
          strength.getStrength(unit).getValue(),
          is(6));
    }
  }
}
