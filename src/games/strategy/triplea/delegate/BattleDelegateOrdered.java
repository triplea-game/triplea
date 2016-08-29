package games.strategy.triplea.delegate;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.util.Match;

public class BattleDelegateOrdered extends BattleDelegate {
  // Fight all air and bombing raids which are supposed to be fought first. Order shouldn't matter because there won't normally be an attacker decision
  private void stratBombing() {
    for( final Territory t : m_battleTracker.getPendingBattleSites(true) ) {
      final IBattle airRaid = m_battleTracker.getPendingBattle(t, true, BattleType.AIR_RAID);   // Get air raid for current territory (if any)
      if( airRaid != null ) {
        airRaid.fight(m_bridge);
      }
      final IBattle bombingRaid = m_battleTracker.getPendingBattle(t, true, BattleType.BOMBING_RAID );
      bombingRaid.fight(m_bridge);
    }
  }
  /**
   * After the super class .start() method runs, this is called to fight all battles not requiring significant attacker input,
   * in the order required for Global 1940. Other maps with the same required order can also use it or add it anyway
   */
  @Override
  public void start() {
    super.start();

    stratBombing();

    // Remove all undefended transport battles. Doing so here means that such a fight will not prevent an amphibious assault from being auto-resolved
    for( final Territory t : m_battleTracker.getPendingBattleSites(false) ) {  // Loop through normal combats i.e. not bombing or air raid
      final IBattle battle = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
      if( m_battleTracker.getDependentOn(battle).isEmpty() 
          && Match.allMatch( battle.getDefendingUnits(), Matches.UnitIsTransportButNotCombatTransport) ) {
        battle.fight( m_bridge );
      }
    }

    // Fight all amphibious assaults with no retreat option for the attacker and no sea combat - these have no real attacker decisions
    // Also remove all remaining defenseless fights by fighting them
    int otherBattleCount = 0;
    int amphibCount = 0;
    IBattle lastAmphib = null;
    for( final Territory t : m_battleTracker.getPendingBattleSites(false) ) {  // Loop through normal combats i.e. not bombing or air raid
      final IBattle battle = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
      if( battle instanceof NonFightingBattle && m_battleTracker.getDependentOn(battle).isEmpty() ) {
        battle.fight( m_bridge );         // Remove non fighting battles by fighting them automatically. Conveniently done here.
        continue;
      } else if (!(battle instanceof MustFightBattle) ) {
        continue;
      }
      if( battle.isAmphibious() ) {
        // If there is no dependent sea battle and no retreat, fight it now 
        if( m_battleTracker.getDependentOn(battle).isEmpty() && !( (MustFightBattle) battle).canAttackerRetreatSome() ) {
          battle.fight( m_bridge );
        } else {
          amphibCount++;
          lastAmphib = battle;      // Otherwise store it to see if it can be fought later i.e. if there's only one
        }
      } else {
        otherBattleCount++;
      }
    }

    // If there is more than one amphibious assault with a possibility of a real attacker decision, then don't do anything more.
    // Losing inf before art/arm when there are no overland ground troops or air involved is not a real attacker attacker decision in this context
    if( amphibCount > 1 ) {
      return;
    }

    // Fight amphibious assault if there is one remaining. Fight naval battles in random order if prerequisites first.
    if( amphibCount > 0 ) {
      if( m_currentBattle != null && m_currentBattle != lastAmphib ) {      // Not completely sure if this is needed but was in other code and does no harm
        m_currentBattle.fight( m_bridge );
      }
      
      // are there battles that must occur first
      for( final IBattle seaBattle : m_battleTracker.getDependentOn(lastAmphib) ) {
        if( seaBattle instanceof MustFightBattle && seaBattle != null ) {
          seaBattle.fight( m_bridge );
          otherBattleCount--;
        }
      }
      lastAmphib.fight( m_bridge );
    }

    if( otherBattleCount == 1 ) {  // If there is only one remaining normal combat, fight it here rather than requiring the user to click it.
      // This needs another loop just in case the last otherBattleCount was triggered by a sea combat already fought
      for( final Territory t : m_battleTracker.getPendingBattleSites(false) ) {  // Will only find one
        final IBattle lastBattle = m_battleTracker.getPendingBattle( t, false, BattleType.NORMAL );
        if( lastBattle instanceof MustFightBattle ) {
          lastBattle.fight( m_bridge );
        }
      }
    }
  }
}