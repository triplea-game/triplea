package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.RelationshipTracker.RelatedPlayers;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class RelationshipTrackerTest {
  @Nested
  final class RelatedPlayersTest {
    @Nested
    final class EqualsAndHashCodeTest {
      private final GameData gameData = new GameData();
      private final GamePlayer player1 = new GamePlayer("red", gameData);
      private final GamePlayer player2 = new GamePlayer("black", gameData);

      @Test
      void shouldBeEquatableAndHashable() {
        EqualsVerifier.forClass(RelatedPlayers.class)
            .withPrefabValues(GamePlayer.class, player1, player2)
            .suppress(Warning.NULL_FIELDS)
            .verify();
      }

      @Test
      void shouldBeEqualToOtherWithOppositePlayers() {
        final RelatedPlayers relatedPlayers1 = new RelatedPlayers(player1, player2);
        final RelatedPlayers relatedPlayers2 = new RelatedPlayers(player2, player1);

        assertThat(relatedPlayers1.equals(relatedPlayers2), is(true));
      }

      @Test
      void shouldHaveSameHashCodeAsOtherWithOppositePlayers() {
        final RelatedPlayers relatedPlayers1 = new RelatedPlayers(player1, player2);
        final RelatedPlayers relatedPlayers2 = new RelatedPlayers(player2, player1);

        assertThat(relatedPlayers1.hashCode(), is(relatedPlayers2.hashCode()));
      }
    }
  }
}
