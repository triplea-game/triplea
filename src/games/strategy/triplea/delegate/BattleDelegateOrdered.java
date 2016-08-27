package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.Map;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.IBattle.WhoWon;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

public class BattleDelegateOrdered extends BattleDelegate {

  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
    // we may start multiple times due to loading after saving
    // only initialize once

    
    IBattle battle;
    for( final Territory t : m_battleTracker.getPendingBattleSites(true) ) {   // loop throught bombing/air raids
      battle = m_battleTracker.getPendingBattle(t, true, null);
      if( battle == null ) {
        try {
          throw new Exception("Air/Bombing Raid gone missing in BattleDelegate");
        } catch( Exception e ) {  // Will crash below
        }
      }
      battle.fight(m_bridge);
      battle = m_battleTracker.getPendingBattle(t, true, BattleType.BOMBING_RAID );  // check to see if there's still a bombing raid for the territory - i.e. previous battle was an air raid
      if( battle != null ) {
        battle.fight(m_bridge);
      }
    }

    int otherBattleCount = 0;
    int amphibCount = 0;
    IBattle lastAmphib = null;
    for( final Territory t : m_battleTracker.getPendingBattleSites(false) ) {  // Loop through normal combats i.e. not bombing or air raid
      battle = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
      // we only care about battles where we must fight
      // this check is really to avoid implementing getAttackingFrom() in other battle subclasses
      if (!(battle instanceof MustFightBattle) ) {
        continue;
      }
      if( !t.isWater() ) {
        final Map<Territory, Collection<Unit>> attackingFromMap = ((MustFightBattle) battle).getAttackingFromMap();
        for( final Territory neighbor : ((MustFightBattle) battle).getAttackingFrom() ) {
          if (!neighbor.isWater() || Match.allMatch(attackingFromMap.get(neighbor), Matches.UnitIsAir) ) {
            continue;
          }
          amphibCount++;
          lastAmphib = battle;
          break;
        }
      }
      if( lastAmphib != battle ) {
        otherBattleCount++;
      }
    }

    if( amphibCount > 1 ) {
      return;
    }

    // Fight amphibious assault if there is one. Fight naval battles in random order if prerequisites first.
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
        battle = m_battleTracker.getPendingBattle( t, false, BattleType.NORMAL );
        if( battle instanceof MustFightBattle ) {
          battle.fight( m_bridge );
        }
      }
    }

  }

}
