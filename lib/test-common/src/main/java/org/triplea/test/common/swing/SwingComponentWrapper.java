package org.triplea.test.common.swing;

import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.Value;

/** A decorator or wrapper for a javax.swing Component that adds methods useful for testing. */
@Value(staticConstructor = "of")
public class SwingComponentWrapper {

  @Nonnull private final Component component;

  /**
   * Recursively searches the wrapped component for a swing component whose name matches the
   * parameter.
   *
   * @param <T> The expected type of the component that we are trying to find.
   * @param childName Component name to find, searching current component or any of its children.
   * @param classType The component type to return.
   * @throws ClassCastException Thrown if the matched component has a type that is different from
   *     the expected type.
   * @throws AssertionError Thrown when failing to match any components by name.
   */
  <T> void findChildByName(final String childName, final Class<T> classType) {
    findChildByNameRecursive(childName, classType)
        .orElseThrow(
            () -> new AssertionError("Expected to find a component with name: " + childName));
  }

  private <T> Optional<T> findChildByNameRecursive(
      final String childName, final Class<T> classType) {
    return childName.equals(component.getName())
        ? Optional.of(classType.cast(component))
        : (component instanceof Container container)
            ? Arrays.stream(container.getComponents())
                .map(
                    childComponent ->
                        SwingComponentWrapper.of(childComponent)
                            .findChildByNameRecursive(childName, classType))
                .filter(Optional::isPresent)
                .findAny()
                .orElse(Optional.empty())
            : Optional.empty();
  }

  public void assertHasComponentByName(final String childName) {
    findChildByName(childName, Object.class);
  }
}
