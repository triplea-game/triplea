package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.ChangeMatcher;
import lombok.AllArgsConstructor;
import org.hamcrest.Description;

@AllArgsConstructor
public class ObjectPropertyChangeMatcher extends ChangeMatcher<ObjectPropertyChange> {

  private final String property;

  private final Object newValue;

  private final Object oldValue;

  @Override
  protected boolean matchesSafely(final ObjectPropertyChange item) {
    return item.getProperty().equals(property)
        && item.getNewValue().equals(newValue)
        && item.getOldValue().equals(oldValue);
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("property:" + property);
    description.appendText(" newValue:" + newValue);
    description.appendText(" oldValue:" + oldValue);
  }

  public static ChangeMatcher<ObjectPropertyChange> propertyChange(
      final String property, final Object newValue, final Object oldValue) {
    return new ObjectPropertyChangeMatcher(property, newValue, oldValue);
  }
}
