package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.Map;
import java.util.List;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.AbstractBattle;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.util.Match;

public class BattleDelegateOrdered extends BattleDelegate {

  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
    // Fight all air and bombing raids
    for( final Territory t : m_battleTracker.getPendingBattleSites(true) ) {   // loop through bombing/air raids
      final IBattle raid = m_battleTracker.getPendingBattle(t, true, null);             // Get air/bombing raid for current territory
      if( raid == null ) {
        try {
          throw new Exception("Air/Bombing Raid gone missing in BattleDelegate");
        } catch( Exception e ) {  // Will crash below
        }
      }
      raid.fight(m_bridge);
      final IBattle bombingRaid = m_battleTracker.getPendingBattle(t, true, BattleType.BOMBING_RAID );  // check to see if there's still a bombing raid for the territory - i.e. previous battle was an air raid
      if( bombingRaid != null ) {
        bombingRaid.fight(m_bridge);
      }
    }

    // Fight all amphibious assaults with no retreat option for the attacker and no sea combat
    int otherBattleCount = 0;
    int amphibCount = 0;
    IBattle lastAmphib = null;
    for( final Territory t : m_battleTracker.getPendingBattleSites(false) ) {  // Loop through normal combats i.e. not bombing or air raid
      final IBattle battle = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
      boolean amphib = false;
      System.out.println(t.getName());
      if( battle instanceof NonFightingBattle        // Remove non fighting battles by fighting them automatically
       || Match.allMatch( battle.getDefendingUnits(), Matches.UnitIsTransportButNotCombatTransport) ) { // Also fight all battles with only TTs defending 
        battle.fight( m_bridge );
        continue;
      } else if (!(battle instanceof MustFightBattle) ) {
        continue;
      }
      final MustFightBattle fightingBattle = (MustFightBattle) battle;
      if( !t.isWater() ) {
        final Map<Territory, Collection<Unit>> attackingFromMap = fightingBattle.getAttackingFromMap();
        for( final Territory neighbor : fightingBattle.getAttackingFrom() ) {
          if (!neighbor.isWater() || Match.allMatch(attackingFromMap.get(neighbor), Matches.UnitIsAir) ) {
            continue;
          }
          amphib = true;
          break;
        }
        if( amphib ) {
          System.out.format("amphib in %s\n", t.getName());
        }
        // If there is no dependent sea battle and no retreat, fight it now 
        if( amphib && m_battleTracker.getDependentOn(fightingBattle).isEmpty() && !fightingBattle.canAttackerRetreatSome() ) {
          fightingBattle.fight( m_bridge );
          lastAmphib = null;
          continue;
        } else if( amphib ) {
          System.out.println("Stored");
          amphibCount++;
          lastAmphib = battle;
        }
      }
      if( !amphib ) {
        otherBattleCount++;
      }
    }

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
      for( final Territory t : m_battleTracker.getPendingBattleSites(false) ) {  // Will only find one
        final IBattle lastBattle = m_battleTracker.getPendingBattle( t, false, BattleType.NORMAL );
        if( lastBattle instanceof MustFightBattle ) {
          lastBattle.fight( m_bridge );
        }
      }
    }

  }

}
