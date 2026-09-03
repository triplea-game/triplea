package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.serializer.GameDataOracle;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class BattleStateSaverTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  private BattleExtendedDelegateState sampleState() {
    final BattleExtendedDelegateState s = new BattleExtendedDelegateState();
    // superState is a package-private delegate-package type not visible here; null exercises the
    // "none" nested path and is a valid Serializable state for the round-trip.
    s.superState = null;
    s.needToInitialize = true;
    s.needToScramble = true;
    s.needToCreateRockets = true;
    s.needToKamikazeSuicideAttacks = true;
    s.needToClearEmptyAirBattleAttacks = true;
    s.needToAddBombardmentSources = true;
    s.needToFireRockets = true;
    s.needToRecordBattleStatistics = true;
    s.needToCheckDefendingPlanesCanLand = true;
    s.needToCleanup = true;
    s.battleTracker = new BattleTracker();
    s.rocketHelper = null;
    s.currentBattle = null;
    return s;
  }

  @Test
  void saverRoundTripsState() {
    GameDataOracle.assertStateRoundTrips(new BattleStateSaver(), sampleState(), gameData);
  }
}
