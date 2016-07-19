package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

/**
 * RelationshipChange this creates a change in relationshipType between two players, for example from Neutral to War.
 */
class RelationshipChange extends Change {
  private static final long serialVersionUID = 2694339584633196289L;
  private final String m_player1;
  private final String m_player2;
  private final String m_OldRelation;
  private final String m_NewRelation;

  RelationshipChange(final PlayerID player1, final PlayerID player2, final RelationshipType oldRelation,
      final RelationshipType newRelation) {
    m_player1 = player1.getName();
    m_player2 = player2.getName();
    m_OldRelation = oldRelation.getName();
    m_NewRelation = newRelation.getName();
  }

  private RelationshipChange(final String player1, final String player2, final String oldRelation,
      final String newRelation) {
    m_player1 = player1;
    m_player2 = player2;
    m_OldRelation = oldRelation;
    m_NewRelation = newRelation;
  }

  @Override
  public Change invert() {
    return new RelationshipChange(m_player1, m_player2, m_NewRelation, m_OldRelation);
  }

  @Override
  protected void perform(final GameData data) {
    data.getRelationshipTracker().setRelationship(data.getPlayerList().getPlayerID(m_player1),
        data.getPlayerList().getPlayerID(m_player2), data.getRelationshipTypeList().getRelationshipType(m_NewRelation));
    // now redraw territories in case of new hostility
    if (Matches.RelationshipTypeIsAtWar.match(data.getRelationshipTypeList().getRelationshipType(m_NewRelation))) {
      for (final Territory t : Match.getMatches(data.getMap().getTerritories(),
          new CompositeMatchAnd<>(
              Matches.territoryHasUnitsOwnedBy(data.getPlayerList().getPlayerID(m_player1)),
              Matches.territoryHasUnitsOwnedBy(data.getPlayerList().getPlayerID(m_player2))))) {
        t.notifyChanged();
      }
    }
  }

  @Override
  public String toString() {
    return "Add relation change. " + m_player1 + " and " + m_player2 + " change from " + m_OldRelation + " to "
        + m_NewRelation;
  }
}
