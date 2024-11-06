package games.strategy.engine.data.changefactory;

import static org.hamcrest.Matchers.equalTo;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeMatcher;
import lombok.AllArgsConstructor;
import org.hamcrest.Description;

/**
 * Matches {@link ObjectPropertyChange} objects with the requested property, newValue, and oldValue
 *
 * <p>Example usage: assertThat(change, propertyChange(property, newValue, oldValue));
 */
@AllArgsConstructor
public class ObjectPropertyChangeMatcher extends ChangeMatcher<Change> {

  private final String property;

  private final Object newValue;

  private final Object oldValue;

  @Override
  protected boolean matchesSafely(final Change change) {
    if (!(change instanceof ObjectPropertyChange)) {
      return false;
    }

    final ObjectPropertyChange objectPropertyChange = (ObjectPropertyChange) change;
    return equalTo(objectPropertyChange.getProperty()).matches(property)
        && equalTo(objectPropertyChange.getNewValue()).matches(newValue)
        && equalTo(objectPropertyChange.getOldValue()).matches(oldValue);
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("property: " + property);
    description.appendText(" newValue: " + newValue);
    description.appendText(" oldValue: " + oldValue);
  }

  public static ChangeMatcher<Change> propertyChange(
      final String property, final Object newValue, final Object oldValue) {
    return new ObjectPropertyChangeMatcher(property, newValue, oldValue);
  }
}
