package games.strategy.triplea.delegate;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.util.Match;

/**
 * This is to support the new maps added to world_war_ii_global which enforce strategic bombing first in the combat order @MapSupport
 * @author simon
 *
 */
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
  
  private void killUndefendedTransports() {
    // Remove all undefended transport battles. Doing so here means that such a fight will not prevent an amphibious assault from being auto-resolved
    for( final Territory t : m_battleTracker.getPendingBattleSites(false) ) {  // Loop through normal combats i.e. not bombing or air raid
      final IBattle battle = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
      if( m_battleTracker.getDependentOn(battle).isEmpty() 
          && Match.allMatch( battle.getDefendingUnits(), Matches.UnitIsTransportButNotCombatTransport) ) {
        battle.fight( m_bridge );
      }
    }
  }

  private void fightRemainingBattle() {
		// This needs another loop just in case the last otherBattleCount was triggered by a sea combat already fought
		for( final Territory t : m_battleTracker.getPendingBattleSites(false) ) {  // Will only find one
			final IBattle lastBattle = m_battleTracker.getPendingBattle( t, false, BattleType.NORMAL );
			if( lastBattle instanceof MustFightBattle ) {
				lastBattle.fight( m_bridge );
			}
		}
	}

	private int fightAmphib( IBattle amphib ) {
		// are there battles that must occur first
		int seaBattlesFought = 0;
		for( final IBattle seaBattle : m_battleTracker.getDependentOn( amphib ) ) {
			if( seaBattle instanceof MustFightBattle && seaBattle != null ) {
				seaBattle.fight( m_bridge );
				seaBattlesFought++;
			}
		}
		amphib.fight( m_bridge );
		
		return seaBattlesFought;
	}
	
  /**
   * After the super class .start() method runs, this is called to fight all battles not requiring significant attacker input,
   * in the order required for Global 1940. Other maps with the same required order can also use it or add it anyway
   */
  @Override
  public void start() {
    super.start();

    stratBombing();
    killUndefendedTransports();

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
      otherBattleCount -= fightAmphib( lastAmphib ); // Decrement otherBattles by the number of sea battles fought
    }

    if( otherBattleCount == 1 ) {  // If there is only one remaining normal combat, fight it here rather than requiring the user to click it.
    	fightRemainingBattle();
    }
  }
}