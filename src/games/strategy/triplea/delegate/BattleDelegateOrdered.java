package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    
    Territory lastAmphib = null;
    Territory lastNonAmphib = null;
    Territory t;
    IBattle battle;
    Iterator<Territory> battleTerritories = m_battleTracker.getPendingBattleSites(true).iterator(); // get bombing/air raids
    while( battleTerritories.hasNext() ) {
      t = battleTerritories.next();
      battle = m_battleTracker.getPendingBattle(t, true, null);
      if( battle == null ) {
        try {
          throw new Exception("Air/Bombing Raid gone missing in BattleDelegate");
        } catch( Exception e ) {
        }
      }
      battle.fight(m_bridge);
      battle = m_battleTracker.getPendingBattle(t, true, BattleType.BOMBING_RAID );  // check to see if there's still a bombing raid for the territory - i.e. previous battle was an air raid
      if( battle != null ) {
        battle.fight(m_bridge);
      }
    }

    battleTerritories = m_battleTracker.getPendingBattleSites(false).iterator();      // Get normal combats
    int landBattleCount = 0;
    int amphibCount = 0;
    while (battleTerritories.hasNext()) {
      t = battleTerritories.next();
      battle = m_battleTracker.getPendingBattle(t, false, BattleType.NORMAL);
      // we only care about battles where we must fight
      // this check is really to avoid implementing getAttackingFrom() in other battle subclasses
      if (!(battle instanceof MustFightBattle) || t.isWater() ) {
        continue;
      }
      landBattleCount++;
      final Map<Territory, Collection<Unit>> attackingFromMap = ((MustFightBattle) battle).getAttackingFromMap();
      final Iterator<Territory> bombardingTerritories = ((MustFightBattle) battle).getAttackingFrom().iterator();
      while (bombardingTerritories.hasNext()) {
        final Territory neighbor = bombardingTerritories.next();
        if (!neighbor.isWater() || Match.allMatch(attackingFromMap.get(neighbor), Matches.UnitIsAir) ) continue;
        amphibCount++;
        lastAmphib = t;
      }
      if( lastAmphib != t ) {
        lastNonAmphib = t;  // If we didn't find amphibious then it was the last non-amphib, obviously. Did this need a comment?
      }
    }

    if( amphibCount > 1 ) return;

    // Fight amphibious assault if there is one. Fight naval battles in random order if prerequisites first.
    while( amphibCount > 0
        && (battle = m_battleTracker.getPendingBattle( lastAmphib, false, BattleType.NORMAL )) != null
        && battle instanceof MustFightBattle ) {
      if( m_currentBattle != null && m_currentBattle != battle ) {
        m_currentBattle.fight(m_bridge);
        continue;
      }
      
      // are there battles that must occur first
      final Collection<IBattle> allMustPrecede = m_battleTracker.getDependentOn(battle);
      if (!allMustPrecede.isEmpty()) {
        final Iterator<IBattle> seaBattles = allMustPrecede.iterator();
        do {
          battle = seaBattles.next();
        } while( ! (battle instanceof MustFightBattle ) && seaBattles.hasNext() );
      }
      if( battle != null ) {
        battle.fight(m_bridge);
        if( battle.getTerritory() == lastAmphib ) landBattleCount--;            // Reduce count of battles only if we've found the actual amphibious assault, not a dependent sea battle
      }
    }

    if( landBattleCount == 1 ) {  // If there is only one remaining normal combat, fight it here rather than requiring the user to click it.
      battle = m_battleTracker.getPendingBattle( lastNonAmphib, false, BattleType.NORMAL );
      if( battle != null ) {
        battle.fight(m_bridge);
      }
      else {
        try {
          throw new Exception( "Non amphib battle not found" );
        } catch( Exception e ) {
        }
      }
    }

  }

}
