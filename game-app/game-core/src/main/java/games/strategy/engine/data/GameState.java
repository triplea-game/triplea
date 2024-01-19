package games.strategy.engine.data;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.TechnologyDelegate;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import java.util.Map;

public interface GameState {
  GameMap getMap();

  /** Returns a collection of all units in the game. */
  UnitsList getUnits();

  /** Returns list of Players in the game. */
  PlayerList getPlayerList();

  /** Returns list of resources available in the game. */
  ResourceList getResourceList();

  /** Returns list of production Frontiers for this game. */
  ProductionFrontierList getProductionFrontierList();

  /** Returns list of Production Rules for the game. */
  ProductionRuleList getProductionRuleList();

  /** Returns the Technology Frontier for this game. */
  TechnologyFrontier getTechnologyFrontier();

  /** Returns the list of production Frontiers for this game. */
  RepairFrontierList getRepairFrontierList();

  /** Returns the list of Production Rules for the game. */
  RepairRules getRepairRules();

  /** Returns the Alliance Tracker for the game. */
  AllianceTracker getAllianceTracker();

  TechTracker getTechTracker();

  GameSequence getSequence();

  UnitTypeList getUnitTypeList();

  UnitHolder getUnitHolder(String name, String type);

  GameProperties getProperties();

  String getGameName();

  String getMapName();

  /**
   * Returns all relationshipTypes that are valid in this game, default there is the NullRelation
   * (relation with the Null player / Neutral) and the SelfRelation (Relation with yourself) all
   * other relations are map designer defined.
   */
  RelationshipTypeList getRelationshipTypeList();

  /** Returns a tracker which tracks all current relationships that exist between all players. */
  RelationshipTracker getRelationshipTracker();

  Map<String, TerritoryEffect> getTerritoryEffectList();

  BattleRecordsList getBattleRecordsList();

  BattleDelegate getBattleDelegate();

  PoliticsDelegate getPoliticsDelegate();

  AbstractMoveDelegate getMoveDelegate();

  TechnologyDelegate getTechDelegate();
}
