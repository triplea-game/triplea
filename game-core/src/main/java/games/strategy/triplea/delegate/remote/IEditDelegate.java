package games.strategy.triplea.delegate.remote;

import java.util.Collection;

import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.TechAdvance;

/**
 * Remote interface for EditDelegate.
 */
public interface IEditDelegate extends IRemote, IPersistentDelegate {
  boolean getEditMode();

  void setEditMode(boolean editMode);

  String removeUnits(Territory t, Collection<Unit> units);

  String addUnits(Territory t, Collection<Unit> units);

  String changeTerritoryOwner(Territory t, PlayerId player);

  String changePUs(PlayerId player, int pus);

  String changeTechTokens(PlayerId player, int tokens);

  String addTechAdvance(PlayerId player, Collection<TechAdvance> advance);

  String removeTechAdvance(PlayerId player, Collection<TechAdvance> advance);

  String changeUnitHitDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  String changeUnitBombingDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  String addComment(String message);

  String changePoliticalRelationships(
      Collection<Triple<PlayerId, PlayerId, RelationshipType>> relationshipChanges);
}
