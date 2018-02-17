package games.strategy.triplea.delegate;

import java.io.Serializable;

class BattleExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 7899007486408723505L;
  Serializable superState;
  // add other variables here:
  BattleTracker m_battleTracker = new BattleTracker();
  // public OriginalOwnerTracker m_originalOwnerTracker = new OriginalOwnerTracker();
  public boolean m_needToInitialize;
  boolean m_needToScramble;
  boolean m_needToKamikazeSuicideAttacks;
  boolean m_needToClearEmptyAirBattleAttacks;
  boolean m_needToAddBombardmentSources;
  boolean m_needToRecordBattleStatistics;
  boolean m_needToCheckDefendingPlanesCanLand;
  boolean m_needToCleanup;
  IBattle m_currentBattle;
}
