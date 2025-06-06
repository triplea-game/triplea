package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BattleTrackerTest {

  @Mock private IDelegateBridge mockDelegateBridge;

  private final GameData mockGameData = givenGameData().build();

  @Mock private GameProperties mockGameProperties;

  @Mock private RelationshipTracker mockRelationshipTracker;

  @Mock private BiFunction<Territory, IBattle.BattleType, IBattle> mockGetBattleFunction;

  @Mock private IBattle mockBattle;

  private final BattleTracker testObj = new BattleTracker();

  @Test
  void verifyRaidsWithNoBattles() {
    testObj.fightAirRaidsAndStrategicBombing(mockDelegateBridge);
  }

  @Test
  void verifyRaids() {
    final Territory territory = new Territory("terrName", mockGameData);
    territory.addAttachment(
        Constants.TERRITORY_ATTACHMENT_NAME,
        new TerritoryAttachment("name", territory, mockGameData));
    final Route route = new Route(territory);
    final GamePlayer gamePlayer = new GamePlayer("name", mockGameData);

    // need at least one attacker for there to be considered a battle.
    final Unit unit = new Unit(new UnitType("unit", mockGameData), gamePlayer, mockGameData);
    final List<Unit> attackers = List.of(unit);

    when(mockDelegateBridge.getData()).thenReturn(mockGameData);
    when(mockGameData.getProperties()).thenReturn(mockGameProperties);
    when(mockGameData.getRelationshipTracker()).thenReturn(mockRelationshipTracker);
    when(mockGameProperties.get(Constants.RAIDS_MAY_BE_PRECEEDED_BY_AIR_BATTLES, false))
        .thenReturn(true);
    doReturn(null).when(mockGetBattleFunction).apply(territory, IBattle.BattleType.AIR_RAID);
    doReturn(mockBattle)
        .when(mockGetBattleFunction)
        .apply(territory, IBattle.BattleType.BOMBING_RAID);

    // set up the testObj to have the bombing battle
    testObj.addBattle(
        route, attackers, true, gamePlayer, mockDelegateBridge, null, null, null, false);

    testObj.fightAirRaidsAndStrategicBombing(
        mockDelegateBridge, () -> Set.of(territory), mockGetBattleFunction);

    verify(mockBattle).fight(mockDelegateBridge);
  }
}
