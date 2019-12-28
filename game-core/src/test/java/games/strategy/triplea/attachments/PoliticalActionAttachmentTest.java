package games.strategy.triplea.attachments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class PoliticalActionAttachmentTest {
  @Nested
  final class GetRelationshipChangesTest {
    private final GameData gameData = new GameData();
    private final GamePlayer player1 = new GamePlayer("player1", gameData);
    private final GamePlayer player2 = new GamePlayer("player2", gameData);
    private final GamePlayer player3 = new GamePlayer("player3", gameData);
    private final RelationshipType relationshipType1 =
        new RelationshipType("relationshipType1", gameData);
    private final RelationshipType relationshipType2 =
        new RelationshipType("relationshipType2", gameData);
    private final PoliticalActionAttachment politicalActionAttachment =
        new PoliticalActionAttachment("politicalActionAttachment", null, gameData);

    private String join(final String... values) {
      return String.join(":", values);
    }

    @BeforeEach
    void setUpGameData() {
      gameData.getPlayerList().addPlayerId(player1);
      gameData.getPlayerList().addPlayerId(player2);
      gameData.getPlayerList().addPlayerId(player3);
      gameData.getRelationshipTypeList().addRelationshipType(relationshipType1);
      gameData.getRelationshipTypeList().addRelationshipType(relationshipType2);
    }

    @Test
    void shouldReturnEmptyListWhenZeroRelationshipChangesExist() {
      assertThat(politicalActionAttachment.getRelationshipChanges(), hasSize(0));
    }

    @Test
    void shouldReturnListOfSizeOneWhenOneRelationshipChangeExists() throws Exception {
      politicalActionAttachment.setRelationshipChange(
          join(player1.getName(), player2.getName(), relationshipType1.getName()));

      assertThat(
          politicalActionAttachment.getRelationshipChanges(),
          contains(
              new PoliticalActionAttachment.RelationshipChange(
                  player1, player2, relationshipType1)));
    }

    @Test
    void shouldReturnListOfSizeTwoWhenTwoRelationshipChangesExists() throws Exception {
      politicalActionAttachment.setRelationshipChange(
          join(player1.getName(), player2.getName(), relationshipType1.getName()));
      politicalActionAttachment.setRelationshipChange(
          join(player1.getName(), player3.getName(), relationshipType2.getName()));

      assertThat(
          politicalActionAttachment.getRelationshipChanges(),
          contains(
              new PoliticalActionAttachment.RelationshipChange(player1, player2, relationshipType1),
              new PoliticalActionAttachment.RelationshipChange(
                  player1, player3, relationshipType2)));
    }
  }

  @Nested
  final class RelationshipChangeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      final GameData gameData = new GameData();
      EqualsVerifier.forClass(PoliticalActionAttachment.RelationshipChange.class)
          .withPrefabValues(
              GamePlayer.class,
              new GamePlayer("redPlayerId", gameData),
              new GamePlayer("blackPlayerId", gameData))
          .withPrefabValues(
              RelationshipType.class,
              new RelationshipType("redRelationshipType", gameData),
              new RelationshipType("blackRelationshipType", gameData))
          .verify();
    }
  }
}
