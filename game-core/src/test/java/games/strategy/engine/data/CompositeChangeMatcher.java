package games.strategy.engine.data;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

@AllArgsConstructor
public class CompositeChangeMatcher extends ChangeMatcher<CompositeChange> {

  private final List<ChangeMatcher<?>> changeMatchers;

  @Override
  protected boolean matchesSafely(final CompositeChange compositeChange) {
    final List<Change> changes = compositeChange.getChanges();

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

  @Override
  public void describeTo(final Description description) {
    description.appendText("CompositeChange <[");
    changeMatchers.forEach(change -> change.describeTo(description));
    description.appendText("]>");
  }

  public static Matcher<CompositeChange> compositeChangeContains(
      final ChangeMatcher<?>... changes) {
    return new CompositeChangeMatcher(Arrays.asList(changes));
  }
}
