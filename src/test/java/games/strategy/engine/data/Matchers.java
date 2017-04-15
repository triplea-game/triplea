package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A collection of matchers for types in the {@link games.strategy.engine.data} package.
 */
public final class Matchers {
  private Matchers() {}

  /**
   * Creates a matcher that matches when the examined {@link GameData} is logically equal to the specified
   * {@link GameData}.
   *
   * @param expected The expected {@link GameData} value; must not be {@code null}.
   *
   * @return A new matcher; never {@code null}.
   */
  public static Matcher<GameData> equalToGameData(final GameData expected) {
    checkNotNull(expected);

    return new IsGameDataEqual(expected);
  }

  private static final class IsGameDataEqual extends TypeSafeMatcher<GameData> {
    private final GameData expected;

    IsGameDataEqual(final GameData expected) {
      assert expected != null;

      this.expected = expected;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendValue(expected);
    }

    @Override
    protected boolean matchesSafely(final GameData actual) {
      return Objects.equals(expected.getGameName(), actual.getGameName())
          && Objects.equals(expected.getGameVersion(), actual.getGameVersion());
      // TODO: include remaining fields
    }
  }
}
