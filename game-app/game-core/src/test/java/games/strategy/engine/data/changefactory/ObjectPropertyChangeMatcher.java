package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.mockito.ArgumentMatcher;

/**
 * Matches {@link ObjectPropertyChange} objects with the requested property, newValue, and oldValue,
 * used with Mockito's {@code argThat} verification.
 *
 * <p>Example usage: verify(bridge).addChange(argThat(propertyChange(property, newValue,
 * oldValue)));
 */
@AllArgsConstructor
public class ObjectPropertyChangeMatcher implements ArgumentMatcher<Change> {

  private final String property;

  private final Object newValue;

  private final Object oldValue;

  @Override
  public boolean matches(final Change change) {
    if (!(change instanceof ObjectPropertyChange)) {
      return false;
    }

    final ObjectPropertyChange objectPropertyChange = (ObjectPropertyChange) change;
    return Objects.equals(objectPropertyChange.getProperty(), property)
        && Objects.equals(objectPropertyChange.getNewValue(), newValue)
        && Objects.equals(objectPropertyChange.getOldValue(), oldValue);
  }

  public static ArgumentMatcher<Change> propertyChange(
      final String property, final Object newValue, final Object oldValue) {
    return new ObjectPropertyChangeMatcher(property, newValue, oldValue);
  }
}
