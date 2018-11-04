package games.strategy.triplea.delegate;

import java.io.Serializable;

class BattleExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 7899007486408723505L;

  Serializable superState;
  // add other variables here:
  BattleTracker battleTracker = new BattleTracker();
  public boolean needToInitialize;
  boolean needToScramble;
  boolean needToCreateRockets;
  boolean needToKamikazeSuicideAttacks;
  boolean needToClearEmptyAirBattleAttacks;
  boolean needToAddBombardmentSources;
  boolean needToFireRockets;
  boolean needToRecordBattleStatistics;
  boolean needToCheckDefendingPlanesCanLand;
  boolean needToCleanup;
  RocketsFireHelper rocketHelper = null;
  IBattle currentBattle;
}
