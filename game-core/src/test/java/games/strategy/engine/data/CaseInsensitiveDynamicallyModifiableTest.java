package games.strategy.engine.data;

import static games.strategy.test.Assertions.assertNotThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public final class CaseInsensitiveDynamicallyModifiableTest {
  private final DynamicallyModifiable object = new FakeDynamicallyModifiableObject();

  @Test
  public void getProperty_ShouldPerformLookupUsingCaseInsensitivePropertyName() {
    final Optional<MutableProperty<?>> propertyFromLowerCaseName = object.getProperty("name");
    final Optional<MutableProperty<?>> propertyFromUpperCaseName = object.getProperty("NAME");

    assertThat(propertyFromLowerCaseName.isPresent(), is(true));
    assertThat(propertyFromUpperCaseName.isPresent(), is(true));
    assertThat(propertyFromLowerCaseName, is(propertyFromUpperCaseName));
  }

  @Test
  public void getPropertyOrThrow_ShouldPerformLookupUsingCaseInsensitivePropertyName() {
    final AtomicReference<MutableProperty<?>> propertyFromLowerCaseNameRef = new AtomicReference<>();
    final AtomicReference<MutableProperty<?>> propertyFromUpperCaseNameRef = new AtomicReference<>();

    assertNotThrows(() -> propertyFromLowerCaseNameRef.set(object.getPropertyOrThrow("name")));
    assertNotThrows(() -> propertyFromUpperCaseNameRef.set(object.getPropertyOrThrow("NAME")));

    assertThat(propertyFromLowerCaseNameRef.get(), is(propertyFromUpperCaseNameRef.get()));
  }

  private static final class FakeDynamicallyModifiableObject implements CaseInsensitiveDynamicallyModifiable {
    private final MutableProperty<?> nameProperty = MutableProperty.of(Object.class, () -> null);

    @Override
    public Map<String, MutableProperty<?>> getPropertyMap() {
      return Collections.singletonMap("name", nameProperty);
    }
  }
}
