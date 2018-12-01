package games.strategy.engine.pbem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class AbstractForumPosterTest {
  @Nested
  final class IsSameTypeTest {
    private final AbstractForumPoster reference = new ConcreteForumPoster();

    @Test
    void shouldReturnTrueWhenOtherHasSameClass() {
      assertThat(reference.isSameType(new ConcreteForumPoster()), is(true));
    }

    @Test
    void shouldReturnFalseWhenOtherHasDifferentClass() {
      assertThat(reference.isSameType(mock(AbstractForumPoster.class)), is(false));
    }

    @Test
    void shouldReturnFalseWhenOtherIsNull() {
      assertThat(reference.isSameType(null), is(false));
    }

    private final class ConcreteForumPoster extends AbstractForumPoster {
      private static final long serialVersionUID = -6649241125894154214L;

      @Override
      public boolean postTurnSummary(final String summary, final String subject) {
        return false;
      }

      @Override
      public IForumPoster doClone() {
        return null;
      }

      @Override
      public boolean supportsSaveGame() {
        return false;
      }

      @Override
      public String getDisplayName() {
        return null;
      }

      @Override
      public void viewPosted() {}

      @Override
      public String getTestMessage() {
        return null;
      }

      @Override
      public String getHelpText() {
        return null;
      }
    }
  }
}
