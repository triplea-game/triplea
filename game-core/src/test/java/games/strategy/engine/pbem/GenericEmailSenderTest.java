package games.strategy.engine.pbem;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

final class GenericEmailSenderTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(GenericEmailSender.class)
          .usingGetClass()
          .withIgnoredFields(
              "alsoPostAfterCombatMove",
              "credentialsProtected",
              "credentialsSaved",
              "encryption",
              "host",
              "password",
              "port",
              "subjectPrefix",
              "timeout",
              "toAddress",
              "username")
          .verify();
    }
  }
}
