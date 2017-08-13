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
   * @param expected The expected {@link GameData} value.
   *
   * @return A new matcher.
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
      return (expected.getDiceSides() == actual.getDiceSides())
          && areBothNullOrBothNotNull(expected.getGameLoader(), actual.getGameLoader())
          && Objects.equals(expected.getGameName(), actual.getGameName())
          && Objects.equals(expected.getGameVersion(), actual.getGameVersion());
      // TODO: include remaining fields
    }

    private static boolean areBothNullOrBothNotNull(final Object expected, final Object actual) {
      return (expected == null) == (actual == null);
    }
  }

  /**
   * Creates a matcher that matches when the examined {@link Resource} is logically equal to the specified
   * {@link Resource}.
   *
   * @param expected The expected {@link Resource} value.
   *
   * @return A new matcher.
   */
  public static Matcher<Resource> equalToResource(final Resource expected) {
    checkNotNull(expected);

    return new IsResourceEqual(expected);
  }

  private static final class IsResourceEqual extends TypeSafeMatcher<Resource> {
    private final Resource expected;

    IsResourceEqual(final Resource expected) {
      assert expected != null;

      this.expected = expected;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendValue(expected);
    }

    @Override
    protected boolean matchesSafely(final Resource actual) {
      return Objects.equals(expected.getAttachments(), actual.getAttachments())
          && equalToGameData(expected.getData()).matches(actual.getData())
          && Objects.equals(expected.getName(), actual.getName());
    }
  }

  /**
   * Creates a matcher that matches when the examined {@link ResourceCollection} is logically equal to the specified
   * {@link ResourceCollection}.
   *
   * @param expected The expected {@link ResourceCollection} value.
   *
   * @return A new matcher.
   */
  public static Matcher<ResourceCollection> equalToResourceCollection(final ResourceCollection expected) {
    checkNotNull(expected);

    return new IsResourceCollectionEqual(expected);
  }

  private static final class IsResourceCollectionEqual extends TypeSafeMatcher<ResourceCollection> {
    private final ResourceCollection expected;

    IsResourceCollectionEqual(final ResourceCollection expected) {
      assert expected != null;

      this.expected = expected;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendValue(expected);
    }

    @Override
    protected boolean matchesSafely(final ResourceCollection actual) {
      return Objects.equals(expected.getResourcesCopy(), actual.getResourcesCopy());
    }
  }
}
