package games.strategy.engine.pbem;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

final class AbstractForumPosterTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(AbstractForumPoster.class)
          .usingGetClass()
          .withIgnoredFields(
              "alsoPostAfterCombatMove",
              "credentialsProtected",
              "credentialsSaved",
              "includeSaveGame",
              "password",
              "topicId",
              "username")
          .verify();
    }
  }
}
