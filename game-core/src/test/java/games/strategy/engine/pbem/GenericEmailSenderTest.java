package games.strategy.engine.pbem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class GenericEmailSenderTest {
  @Nested
  final class IsSameTypeTest {
    private final GenericEmailSender reference = new ConcreteEmailSender();

    @Test
    void shouldReturnTrueWhenOtherHasSameClass() {
      assertThat(reference.isSameType(new ConcreteEmailSender()), is(true));
    }

    @Test
    void shouldReturnFalseWhenOtherHasDifferentClass() {
      assertThat(reference.isSameType(mock(GenericEmailSender.class)), is(false));
    }

    @Test
    void shouldReturnFalseWhenOtherIsNull() {
      assertThat(reference.isSameType(null), is(false));
    }

    private final class ConcreteEmailSender extends GenericEmailSender {
      private static final long serialVersionUID = 9167992793666878829L;
    }
  }
}
