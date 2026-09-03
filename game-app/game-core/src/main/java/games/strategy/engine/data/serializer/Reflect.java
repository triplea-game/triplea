package games.strategy.engine.data.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Minimal reflective field and constructor access for {@link TextSaver}s whose target type keeps
 * its state in {@code private final} fields with private constructors — notably the {@code Change}
 * subclasses. It lets the explicit text contract be added <b>without modifying those classes</b>.
 *
 * <p>Coupling to field names is acceptable here: the project's save-game-compatibility rule already
 * forbids renaming or removing those fields, so the names are stable by policy. Each saver still
 * declares exactly which fields it persists, so the contract is explicit — reflection is only the
 * access mechanism.
 */
public final class Reflect {

  private Reflect() {}

  /** Reads the value of {@code fieldName} from {@code target} (searching superclasses). */
  public static Object get(final Object target, final String fieldName) {
    try {
      final Field field = findField(target.getClass(), fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException(
          "Failed to read field '" + fieldName + "' on " + target.getClass().getName(), e);
    }
  }

  /** Constructs a {@code T} via the declared constructor matching {@code paramTypes}. */
  public static <T> T construct(
      final Class<T> type, final Class<?>[] paramTypes, final Object... args) {
    try {
      final Constructor<T> constructor = type.getDeclaredConstructor(paramTypes);
      constructor.setAccessible(true);
      return constructor.newInstance(args);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to construct " + type.getName(), e);
    }
  }

  private static Field findField(final Class<?> type, final String name)
      throws NoSuchFieldException {
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      try {
        return c.getDeclaredField(name);
      } catch (final NoSuchFieldException ignored) {
        // try the superclass
      }
    }
    throw new NoSuchFieldException(name);
  }
}
