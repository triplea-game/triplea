package games.strategy.triplea.delegate.remote;

import java.util.Collection;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.util.IntegerMap;
import games.strategy.util.Triple;

/**
 * Remote interface for EditDelegate.
 */
public interface IEditDelegate extends IRemote, IPersistentDelegate {
  boolean getEditMode();

  String setEditMode(boolean editMode);

  String removeUnits(Territory t, Collection<Unit> units);

  String addUnits(Territory t, Collection<Unit> units);

  String changeTerritoryOwner(Territory t, PlayerId player);

  String changePUs(PlayerId player, int pus);

  String changeTechTokens(PlayerId player, int tokens);

  String addTechAdvance(PlayerId player, Collection<TechAdvance> advance);

  String removeTechAdvance(PlayerId player, Collection<TechAdvance> advance);

  String changeUnitHitDamage(final IntegerMap<Unit> unitDamageMap, final Territory territory);

  String changeUnitBombingDamage(final IntegerMap<Unit> unitDamageMap, final Territory territory);

  String addComment(String message);

  String changePoliticalRelationships(
      Collection<Triple<PlayerId, PlayerId, RelationshipType>> relationshipChanges);
}
