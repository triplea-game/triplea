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
@RemoteActionCode(9)
  boolean getEditMode();

@RemoteActionCode(12)
  void setEditMode(boolean editMode);

@RemoteActionCode(11)
  String removeUnits(Territory t, Collection<Unit> units);

@RemoteActionCode(2)
  String addUnits(Territory t, Collection<Unit> units);

@RemoteActionCode(6)
  String changeTerritoryOwner(Territory t, GamePlayer player);

@RemoteActionCode(3)
  String changePUs(GamePlayer player, int pus);

@RemoteActionCode(5)
  String changeTechTokens(GamePlayer player, int tokens);

@RemoteActionCode(1)
  String addTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

@RemoteActionCode(10)
  String removeTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

@RemoteActionCode(8)
  String changeUnitHitDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

@RemoteActionCode(7)
  String changeUnitBombingDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

@RemoteActionCode(0)
  String addComment(String message);

  String changePoliticalRelationships(
      Collection<Triple<GamePlayer, GamePlayer, RelationshipType>> relationshipChanges);
}
