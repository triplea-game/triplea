package games.strategy.engine.data;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(relatedPlayers1.equals(relatedPlayers2)).isTrue();
      }

      @Test
      void shouldHaveSameHashCodeAsOtherWithOppositePlayers() {
        final RelatedPlayers relatedPlayers1 = new RelatedPlayers(player1, player2);
        final RelatedPlayers relatedPlayers2 = new RelatedPlayers(player2, player1);

        assertThat(relatedPlayers1.hashCode()).isEqualTo(relatedPlayers2.hashCode());
      }
    }
  }
}
