package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.triplea.delegate.TechAdvance;
import java.util.Collection;
import javax.annotation.Nullable;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;

/** Remote interface for EditDelegate. */
public interface IEditDelegate extends IRemote, IPersistentDelegate {
  @RemoteActionCode(9)
  boolean getEditMode();

  @RemoteActionCode(12)
  void setEditMode(boolean editMode);

  @RemoteActionCode(11)
  @Nullable
  String removeUnits(Territory t, Collection<Unit> units);

  @RemoteActionCode(2)
  @Nullable
  String addUnits(Territory t, Collection<Unit> units);

  @RemoteActionCode(6)
  @Nullable
  String changeTerritoryOwner(Territory t, GamePlayer player);

  @RemoteActionCode(13)
  @Nullable
  String changeResource(GamePlayer player, String resourceName, int newTotal);

  @RemoteActionCode(1)
  @Nullable
  String addTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

  @RemoteActionCode(10)
  @Nullable
  String removeTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

  @RemoteActionCode(8)
  @Nullable
  String changeUnitHitDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  @RemoteActionCode(7)
  @Nullable
  String changeUnitBombingDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  @RemoteActionCode(0)
  @Nullable
  String addComment(String message);

  @RemoteActionCode(4)
  @Nullable
  String changePoliticalRelationships(
      Collection<Triple<GamePlayer, GamePlayer, RelationshipType>> relationshipChanges);
}
