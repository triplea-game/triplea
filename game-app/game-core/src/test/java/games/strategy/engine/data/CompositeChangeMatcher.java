package games.strategy.engine.data;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import org.mockito.ArgumentMatcher;

/**
 * Matches all changes of a {@link CompositeChange} object, used with Mockito's {@code argThat}
 * verification.
 *
 * <p>Each of the changes are matched against the requested matchers in the same order and will fail
 * if the changes are not the same size or in the same order.
 *
 * <p>Example usage:
 * verify(bridge).addChange(argThat(compositeChangeContains(propertyChange(property, newValue,
 * oldValue))));
 */
@AllArgsConstructor
public class CompositeChangeMatcher implements ArgumentMatcher<Change> {

  private final List<ArgumentMatcher<Change>> changeMatchers;

  @Override
  public boolean matches(final Change change) {
    if (!(change instanceof CompositeChange)) {
      return false;
    }

    final List<Change> changes = ((CompositeChange) change).getChanges();
    if (changes.size() != changeMatchers.size()) {
      return false;
    }

    for (int i = 0; i < changeMatchers.size(); i++) {
      if (!changeMatchers.get(i).matches(changes.get(i))) {
        return false;
      }
    }

    return true;
  }

  @SafeVarargs
  public static ArgumentMatcher<Change> compositeChangeContains(
      final ArgumentMatcher<Change>... changes) {
    return new CompositeChangeMatcher(Arrays.asList(changes));
  }
}
