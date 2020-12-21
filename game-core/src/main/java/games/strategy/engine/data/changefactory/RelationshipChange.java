package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import org.triplea.java.collections.CollectionUtils;

/**
 * RelationshipChange this creates a change in relationshipType between two players, for example
 * from Neutral to War.
 */
class RelationshipChange extends Change {
  private static final long serialVersionUID = 2694339584633196289L;

  private final String player1Name;
  private final String player2Name;
  private final String oldRelationshipTypeName;
  private final String newRelationshipTypeName;

  RelationshipChange(
      final GamePlayer player1,
      final GamePlayer player2,
      final RelationshipType oldRelationshipType,
      final RelationshipType newRelationshipType) {
    player1Name = player1.getName();
    player2Name = player2.getName();
    oldRelationshipTypeName = oldRelationshipType.getName();
    newRelationshipTypeName = newRelationshipType.getName();
  }

  private RelationshipChange(
      final String player1Name,
      final String player2Name,
      final String oldRelationshipTypeName,
      final String newRelationshipTypeName) {
    this.player1Name = player1Name;
    this.player2Name = player2Name;
    this.oldRelationshipTypeName = oldRelationshipTypeName;
    this.newRelationshipTypeName = newRelationshipTypeName;
  }

  @Override
  public Change invert() {
    return new RelationshipChange(
        player1Name, player2Name, newRelationshipTypeName, oldRelationshipTypeName);
  }

  @Override
  protected void perform(final GameDataInjections data) {
    data.getRelationshipTracker()
        .setRelationship(
            data.getPlayerList().getPlayerId(player1Name),
            data.getPlayerList().getPlayerId(player2Name),
            data.getRelationshipTypeList().getRelationshipType(newRelationshipTypeName));
    // now redraw territories in case of new hostility
    if (Matches.relationshipTypeIsAtWar()
        .test(data.getRelationshipTypeList().getRelationshipType(newRelationshipTypeName))) {
      for (final Territory t :
          CollectionUtils.getMatches(
              data.getMap().getTerritories(),
              Matches.territoryHasUnitsOwnedBy(data.getPlayerList().getPlayerId(player1Name))
                  .and(
                      Matches.territoryHasUnitsOwnedBy(
                          data.getPlayerList().getPlayerId(player2Name))))) {
        t.notifyChanged();
      }
    }
  }

  @Override
  public String toString() {
    return "Add relation change. "
        + player1Name
        + " and "
        + player2Name
        + " change from "
        + oldRelationshipTypeName
        + " to "
        + newRelationshipTypeName;
  }
}
