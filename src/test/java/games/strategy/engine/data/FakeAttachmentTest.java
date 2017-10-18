package games.strategy.engine.data;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public final class FakeAttachmentTest {
  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(FakeAttachment.class).verify();
  }
}
