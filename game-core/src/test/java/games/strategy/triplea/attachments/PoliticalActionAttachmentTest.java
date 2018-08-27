package games.strategy.triplea.attachments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.base.Joiner;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;

final class PoliticalActionAttachmentTest {
  @Nested
  final class ParseRelationshipChangeTest {
    private final GameData gameData = new GameData();
    private final PlayerID player1 = new PlayerID("player1Name", gameData);
    private final PlayerID player2 = new PlayerID("player2Name", gameData);
    private final RelationshipType relationshipType = new RelationshipType("relationshipTypeName", gameData);
    private final PoliticalActionAttachment politicalActionAttachment =
        new PoliticalActionAttachment("politicalActionAttachmentName", null, gameData);

    private String join(final String... values) {
      return Joiner.on(':').join(values);
    }

    @BeforeEach
    void setUpGameData() {
      gameData.getPlayerList().addPlayerId(player1);
      gameData.getPlayerList().addPlayerId(player2);
      gameData.getRelationshipTypeList().addRelationshipType(relationshipType);
    }

    @Test
    void shouldParseRelationshipChangeWhenTokenCountEqualsThree() {
      final PoliticalActionAttachment.RelationshipChange relationshipChange = politicalActionAttachment
          .parseRelationshipChange(join(player1.getName(), player2.getName(), relationshipType.getName()));

      assertThat(relationshipChange.player1, is(player1));
      assertThat(relationshipChange.player2, is(player2));
      assertThat(relationshipChange.relationshipType, is(relationshipType));
    }

    @Test
    void shouldThrowExceptionWhenTokenCountNotEqualsThree() {
      assertThrows(IllegalArgumentException.class, () -> politicalActionAttachment.parseRelationshipChange(
          join(player1.getName(), player2.getName())));
      assertThrows(IllegalArgumentException.class, () -> politicalActionAttachment.parseRelationshipChange(
          join(player1.getName(), player2.getName(), relationshipType.getName(), "other")));
    }
  }
}
