package games.strategy.engine.data;

import static games.strategy.test.Assertions.assertNotThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public final class DecapitalizedDynamicallyModifiableTest {
  private final DynamicallyModifiable object = new FakeDynamicallyModifiableObject();

  @Test
  public void getProperty_ShouldPerformLookupUsingDecapitalizedPropertyName() {
    final Optional<MutableProperty<?>> propertyFromDecapitalizedName = object.getProperty("name");
    final Optional<MutableProperty<?>> propertyFromCapitalizedName = object.getProperty("Name");

    assertThat(propertyFromDecapitalizedName.isPresent(), is(true));
    assertThat(propertyFromCapitalizedName.isPresent(), is(true));
    assertThat(propertyFromDecapitalizedName, is(propertyFromCapitalizedName));
  }

  @Test
  public void getPropertyOrThrow_ShouldPerformLookupUsingDecapitalizedPropertyName() {
    final AtomicReference<MutableProperty<?>> propertyFromDecapitalizedNameRef = new AtomicReference<>();
    final AtomicReference<MutableProperty<?>> propertyFromCapitalizedNameRef = new AtomicReference<>();

    assertNotThrows(() -> propertyFromDecapitalizedNameRef.set(object.getPropertyOrThrow("name")));
    assertNotThrows(() -> propertyFromCapitalizedNameRef.set(object.getPropertyOrThrow("Name")));

    assertThat(propertyFromDecapitalizedNameRef.get(), is(propertyFromCapitalizedNameRef.get()));
  }

  private static final class FakeDynamicallyModifiableObject implements DecapitalizedDynamicallyModifiable {
    private final MutableProperty<?> nameProperty = MutableProperty.of(Object.class, () -> null);

    @Override
    public Map<String, MutableProperty<?>> getPropertyMap() {
      return Collections.singletonMap("name", nameProperty);
    }
  }
}
