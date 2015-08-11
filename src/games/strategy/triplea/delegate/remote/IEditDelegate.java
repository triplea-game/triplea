package games.strategy.triplea.delegate.remote;

import java.util.Collection;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.util.IntegerMap;
import games.strategy.util.Triple;

/**
 * Remote interface for EditDelegate
 */
public interface IEditDelegate extends IRemote, IPersistentDelegate {
  public boolean getEditMode();

  public String setEditMode(boolean editMode);

  public String removeUnits(Territory t, Collection<Unit> units);

  public String addUnits(Territory t, Collection<Unit> units);

  public String changeTerritoryOwner(Territory t, PlayerID player);

  public String changePUs(PlayerID player, int PUs);

  public String changeTechTokens(PlayerID player, int tokens);

  public String addTechAdvance(PlayerID player, Collection<TechAdvance> advance);

  public String removeTechAdvance(PlayerID player, Collection<TechAdvance> advance);

  public String changeUnitHitDamage(final IntegerMap<Unit> unitDamageMap, final Territory territory);

  public String changeUnitBombingDamage(final IntegerMap<Unit> unitDamageMap, final Territory territory);

  public String addComment(String message);

  public String changePoliticalRelationships(
      Collection<Triple<PlayerID, PlayerID, RelationshipType>> relationshipChanges);
}
