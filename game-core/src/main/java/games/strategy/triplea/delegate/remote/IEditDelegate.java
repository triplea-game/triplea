package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.TechAdvance;
import java.util.Collection;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;

/** Remote interface for EditDelegate. */
public interface IEditDelegate extends IRemote, IPersistentDelegate {
  boolean getEditMode();

  void setEditMode(boolean editMode);

  String removeUnits(Territory t, Collection<Unit> units);

  String addUnits(Territory t, Collection<Unit> units);

  String changeTerritoryOwner(Territory t, GamePlayer player);

  String changePUs(GamePlayer player, int pus);

  String changeTechTokens(GamePlayer player, int tokens);

  String addTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

  String removeTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

  String changeUnitHitDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  String changeUnitBombingDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  String addComment(String message);

  String changePoliticalRelationships(
      Collection<Triple<GamePlayer, GamePlayer, RelationshipType>> relationshipChanges);
}
