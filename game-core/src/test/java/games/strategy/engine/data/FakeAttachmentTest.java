package games.strategy.engine.data;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;
import org.triplea.test.common.TestType;

@Integration(type = TestType.TEST_CODE_VERIFICATION)
final class FakeAttachmentTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(FakeAttachment.class).verify();
    }
  }
}
