package games.strategy.triplea.attachments;

import static games.strategy.triplea.attachments.PoliticalActionAttachment.parseRelationshipChange;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.base.Joiner;

import nl.jqno.equalsverifier.EqualsVerifier;

final class PoliticalActionAttachmentTest {
  @Nested
  final class ParseRelationshipChangeTest {
    private static final String PLAYER_1_NAME = "player1Name";
    private static final String PLAYER_2_NAME = "player2Name";
    private static final String RELATIONSHIP_TYPE_NAME = "relationshipTypeName";

    private String join(final String... values) {
      return Joiner.on(':').join(values);
    }

    @Test
    void shouldParseRelationshipChangeWhenTokenCountEqualsThree() {
      assertThat(
          parseRelationshipChange(join(PLAYER_1_NAME, PLAYER_2_NAME, RELATIONSHIP_TYPE_NAME)),
          is(new PoliticalActionAttachment.RelationshipChange(PLAYER_1_NAME, PLAYER_2_NAME, RELATIONSHIP_TYPE_NAME)));
    }

    @Test
    void shouldThrowExceptionWhenTokenCountNotEqualsThree() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parseRelationshipChange(join(PLAYER_1_NAME, PLAYER_2_NAME)));
      assertThrows(
          IllegalArgumentException.class,
          () -> parseRelationshipChange(join(PLAYER_1_NAME, PLAYER_2_NAME, RELATIONSHIP_TYPE_NAME, "other")));
    }
  }

  @Nested
  final class RelationshipChangeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(PoliticalActionAttachment.RelationshipChange.class).verify();
    }
  }
}
