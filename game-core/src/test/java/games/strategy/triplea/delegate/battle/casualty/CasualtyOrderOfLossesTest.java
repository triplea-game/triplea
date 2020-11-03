package games.strategy.triplea.delegate.battle.casualty;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.IntegerMap;

@ExtendWith(MockitoExtension.class)
class CasualtyOrderOfLossesTest {

  @Mock GameData gameData;
  @Mock UnitTypeList unitTypeList;
  @Mock GamePlayer player;
  @Mock UnitAttachment unitAttachment;

  @BeforeEach
  void clearCache() {
    CasualtyOrderOfLosses.clearOolCache();
  }

  @Test
  void oolCacheKeyIsUniqueWhenUnitTypeHashCodesHaveSameSum() {
    final UnitType typePikemen = new UnitType("Pikemen", gameData);
    typePikemen.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final UnitType typeFootmen = new UnitType("Footmen", gameData);
    typeFootmen.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final UnitType typeVeteranPikemen = new UnitType("Veteran-Pikemen", gameData);
    typeVeteranPikemen.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final UnitType typeVeteranFootmen = new UnitType("Veteran-Footmen", gameData);
    typeVeteranFootmen.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);

    final String key1 =
        CasualtyOrderOfLosses.computeOolCacheKey(
            withFakeParameters(),
            List.of(
                CasualtyOrderOfLosses.AmphibType.of(typePikemen.create(1, player, true).get(0)),
                CasualtyOrderOfLosses.AmphibType.of(
                    typeVeteranFootmen.create(1, player, true).get(0))));

    final String key2 =
        CasualtyOrderOfLosses.computeOolCacheKey(
            withFakeParameters(),
            List.of(
                CasualtyOrderOfLosses.AmphibType.of(typeFootmen.create(1, player, true).get(0)),
                CasualtyOrderOfLosses.AmphibType.of(
                    typeVeteranPikemen.create(1, player, true).get(0))));

    assertThat(key1, is(not(key2)));
  }

  private CasualtyOrderOfLosses.Parameters withFakeParameters() {
    when(gameData.getUnitTypeList()).thenReturn(unitTypeList);
    final GamePlayer player = mock(GamePlayer.class);
    when(player.getName()).thenReturn("player");
    final Territory territory = mock(Territory.class);
    when(territory.getName()).thenReturn("territory");
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(List.of())
        .player(player)
        .combatValue(
            CombatValue.buildMainCombatValue(
                List.of(), List.of(), false, gameData, territory, List.of()))
        .battlesite(territory)
        .costs(IntegerMap.of(Map.of()))
        .data(gameData)
        .build();
  }
}
