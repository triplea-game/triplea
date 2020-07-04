package games.strategy.triplea.delegate.battle.casualty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.UnitBattleComparator;
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

  @BeforeEach
  void clearCache() {
    CasualtyOrderOfLosses.clearOolCache();
  }

  @Test
  void oolCacheKeyIsUniqueWhenUnitTypeHashCodesHaveSameSum() {
    final UnitType typePikemen = new UnitType("Pikemen", gameData);
    final UnitType typeFootmen = new UnitType("Footmen", gameData);
    final UnitType typeVeteranPikemen = new UnitType("Veteran-Pikemen", gameData);
    final UnitType typeVeteranFootmen = new UnitType("Veteran-Footmen", gameData);

    final String key1 =
        CasualtyOrderOfLosses.computeOolCacheKey(
            withFakeParameters(), List.of(typePikemen, typeVeteranFootmen), List.of());

    final String key2 =
        CasualtyOrderOfLosses.computeOolCacheKey(
            withFakeParameters(), List.of(typeFootmen, typeVeteranPikemen), List.of());

    assertThat(key1, is(not(key2)));
  }

  private CasualtyOrderOfLosses.Parameters withFakeParameters() {
    final GamePlayer player = mock(GamePlayer.class);
    when(player.getName()).thenReturn("player");
    final Territory territory = mock(Territory.class);
    when(territory.getName()).thenReturn("territory");
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(List.of())
        .combatModifiers(
            UnitBattleComparator.CombatModifiers.builder()
                .defending(false)
                .amphibious(false)
                .territoryEffects(List.of())
                .build())
        .player(player)
        .enemyUnits(List.of())
        .amphibiousLandAttackers(List.of())
        .battlesite(territory)
        .costs(IntegerMap.of(Map.of()))
        .data(gameData)
        .build();
  }
}
