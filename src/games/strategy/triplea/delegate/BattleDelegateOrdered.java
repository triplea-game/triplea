package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.util.Match;
import games.strategy.triplea.MapSupport;

/**
 * This is to support the new maps added to world_war_ii_global which enforce strategic bombing first in the combat
 * order
 * 
 */
@MapSupport
public class BattleDelegateOrdered extends BattleDelegate {
  /**
   * After the super class .start() method runs, this is called to fight all battles not requiring significant attacker
   * input,
   * in the order required for Global 1940. Other maps with the same required order can also use it or add it anyway
   */
  private Collection<IBattle> battleList = new ArrayList<>();

  @Override
  public void start() {
    super.start();
    
    // Remove air raids - this adds all the bombing raids to the battle tracker
    for( final Territory t : m_battleTracker.getPendingSBRSites() ) {
      final IBattle airRaid = m_battleTracker.getPendingBattle(t, true, BattleType.AIR_RAID);   // Get air raid for current territory (if any)
      if( airRaid != null ) {
        airRaid.fight(m_bridge);        // Can't add it to the list of battles because the bombing raid wouldn't then be able to be added
      }
      battleList.add( m_battleTracker.getPendingBattle( t, true, BattleType.BOMBING_RAID ) );
    }

    // Kill undefended transports. Done here to remove potentially dependent sea battles below
    for( final Territory t : m_battleTracker.getPendingNonSBRSites() ) {  // Loop through normal combats i.e. not bombing or air raid
      final IBattle battle = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
      if( m_battleTracker.getDependentOn(battle).isEmpty() 
          && Match.allMatch( battle.getDefendingUnits(), Matches.UnitIsTransportButNotCombatTransport) ) {
        battle.fight( m_bridge );           // Must be fought here to remove dependencies
      }
    }

    // Fight all amphibious assaults with no retreat option for the attacker and no sea combat - these have no real attacker decisions
    // Also remove all remaining defenseless fights by fighting them
    int battleCount = 0;
    int amphibCount = 0;
    IBattle lastAmphib = null;
    for( final Territory t : m_battleTracker.getPendingNonSBRSites() ) {  // Loop through normal combats i.e. not bombing or air raid
      final IBattle battle = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
      if( battle instanceof NonFightingBattle && m_battleTracker.getDependentOn(battle).isEmpty() ) {
        battleList.add( battle );         // Remove non fighting battles by fighting them automatically. Conveniently done here.
        System.out.format("Adding non fight battle in %s\n", battle.getTerritory().getName() );
        continue;
      } else if (!(battle instanceof MustFightBattle) ) {
        continue;
      }
      battleCount++;
      
      if( battle.isAmphibious() ) {
        // If there is no dependent sea battle and no retreat, fight it now 
        if( m_battleTracker.getDependentOn(battle).isEmpty() && !( (MustFightBattle) battle).canAnyAttackersRetreat() ) {
          battleList.add( battle );
          System.out.format("Adding non retreat amphib battle in %s\n", battle.getTerritory().getName() );
        } else {
          amphibCount++;
          lastAmphib = battle;      // Otherwise store it to see if it can be fought later i.e. if there's only one
        }
      }
    }

    // Fight amphibious assault if there is one remaining. Fight naval battles in random order if prerequisites first.
    if( amphibCount == 1 ) {
      // are there battles that must occur first
      for( final IBattle seaBattle : m_battleTracker.getDependentOn( lastAmphib ) ) {
        System.out.format("Sea Battle in %s\n", seaBattle.getTerritory().getName() );
        if( seaBattle instanceof MustFightBattle && seaBattle != null ) {
          battleList.add( seaBattle );
          System.out.format("Adding sea battle in %s\n", seaBattle.getTerritory().getName() );
          battleCount--;
        }
      }
      battleList.add( lastAmphib );
      battleCount--;
      
      //battleCount -= fightAmphib( lastAmphib );   // Subtract off the number of fought battles 
    }

    for( IBattle x : battleList) {
      System.out.format("Battle in %s\n", x.getTerritory().getName() );

    }
    
    battleList.forEach( battle -> battle.fight( m_bridge ) );

    if( battleCount == 1 ) {  // If there is only one remaining normal combat, fight it here rather than requiring the user to click it.
      // This needs another loop just in case the last otherBattleCount was triggered by a sea combat already fought
      for( final Territory t : m_battleTracker.getPendingNonSBRSites() ) {  // Will only find one
        final IBattle lastBattle = m_battleTracker.getPendingBattle( t, false, BattleType.NORMAL );
        if( lastBattle instanceof MustFightBattle ) {
          //battleList.add( lastBattle );
          System.out.format("Adding last battle in %s\n", lastBattle.getTerritory().getName() );
          lastBattle.fight( m_bridge );
        }
      }
    }
  }
  /*
   * private int fightAmphib( IBattle amphib ) {
   * int foughtBattleCount = 0;
   * 
   * return foughtBattleCount;
   * }
   */
}
