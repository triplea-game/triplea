package games.strategy.triplea.delegate;


import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BattleTrackerTest {


	@Mock
	private IDelegateBridge mockDelegateBridge;
	@Mock
	private GameData mockGameData;
	@Mock
	private GameProperties mockGameProperties;
	@Mock
	private RelationshipTracker mockRelationshipTracker;
	@Mock
	private BiFunction<Territory, IBattle.BattleType, IBattle> mockGetBattleFunction;
	@Mock
	private IBattle mockBattle;

	private BattleTracker testObj;

	@Before
	public void setup() {
		testObj = new BattleTracker();
	}

	@Test
	public void verifyRaidsWithNoBattles() {
		testObj.fightAirCombats(mockDelegateBridge);
	}

	@Test
	public void verifyRaids() {
		Territory territory = new Territory("terrName", mockGameData);
		Route route = new Route(territory);
		PlayerID playerId = new PlayerID("name", mockGameData);

		// need at least one attacker for there to be considered a battle.
		Unit unit = new TripleAUnit(new UnitType("unit", mockGameData), playerId, mockGameData);
		List<Unit> attackers = Collections.singletonList(unit);

		when(mockDelegateBridge.getData()).thenReturn(mockGameData);
		when(mockGameData.getProperties()).thenReturn(mockGameProperties);
		when(mockGameData.getRelationshipTracker()).thenReturn(mockRelationshipTracker);
		when(mockGameProperties.get(Constants.RAIDS_MAY_BE_PRECEEDED_BY_AIR_BATTLES, false))
			.thenReturn(true);
		when(mockGetBattleFunction.apply(territory, IBattle.BattleType.BOMBING_RAID)).thenReturn(mockBattle);

		// set up the testObj to have the bombing battle
		testObj.addBombingBattle(route, attackers, playerId, mockDelegateBridge, null, null);

		testObj.fightAirCombats(mockDelegateBridge, () -> Collections.singleton(territory), mockGetBattleFunction);

		verify(mockBattle, times(1)).fight(mockDelegateBridge);
	}
}
