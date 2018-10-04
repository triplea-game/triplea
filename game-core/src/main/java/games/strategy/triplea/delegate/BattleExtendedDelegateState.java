package games.strategy.triplea.delegate;

import java.io.Serializable;

class BattleExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 7899007486408723505L;

  Serializable superState;
  // add other variables here:
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  BattleTracker m_battleTracker = new BattleTracker();
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public boolean m_needToInitialize;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  boolean m_needToScramble;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  boolean m_needToKamikazeSuicideAttacks;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  boolean m_needToClearEmptyAirBattleAttacks;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  boolean m_needToAddBombardmentSources;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  boolean m_needToRecordBattleStatistics;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  boolean m_needToCheckDefendingPlanesCanLand;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  boolean m_needToCleanup;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  IBattle m_currentBattle;
}
