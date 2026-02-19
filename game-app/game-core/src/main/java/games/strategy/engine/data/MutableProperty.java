package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.triplea.java.function.ThrowingConsumer;
import org.triplea.java.function.ThrowingFunction;

/**
 * A wrapper interface to Bundle setters, getters and resetters of the same field.
 *
 * @param <T> The type of the property value.
 */
public final class MutableProperty<T> {
  private final ThrowingConsumer<T, Exception> setter;
  private final ThrowingConsumer<String, Exception> stringSetter;
  private final Supplier<T> getter;
  private final Runnable resetter;

  private MutableProperty(
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<T> getter,
      final Runnable resetter) {
    this.setter = checkNotNull(setter);
    this.stringSetter = checkNotNull(stringSetter);
    this.getter = checkNotNull(getter);
    this.resetter = checkNotNull(resetter);
  }

  private static <T> ThrowingConsumer<T, Exception> noSetter() {
    return value -> {
      throw new UnsupportedOperationException("No Setter has been defined!");
    };
  }

  private static ThrowingConsumer<String, Exception> noStringSetter() {
    return value -> {
      throw new UnsupportedOperationException("No String Setter has been defined!");
    };
  }

  private static <T> Supplier<T> noGetter() {
    return () -> {
      throw new UnsupportedOperationException("No Getter has been defined!");
    };
  }

  private static Runnable noResetter() {
    return () -> {
      throw new UnsupportedOperationException("No Resetter has been defined!");
    };
  }

  private void setStringValue(final String value) throws InvalidValueException {
    try {
      stringSetter.accept(value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new InvalidValueException("failed to set string property value to '" + value + "'", e);
    }
  }

  private void setTypedValue(final T value) throws InvalidValueException {
    try {
      setter.accept(value);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new InvalidValueException("failed to set typed property value to '" + value + "'", e);
    }
  }

  /**
   * Sets the property value.
   *
   * @param value The new property value. If a {@link String}, the value will be set using {@link
   *     #stringSetter}; otherwise the value will be set using {@link #setter}.
   * @throws InvalidValueException If the new property value is invalid.
   */
  public void setValue(final @Nullable Object value) throws InvalidValueException {
    if (value instanceof String stringValue) {
      setStringValue(stringValue);
    } else {
      try {
        setTypedValue(cast(value));
      } catch (final ClassCastException e) {
        // NB: this will also catch ClassCastExceptions thrown by setTypedValue that may have
        // nothing to do with "value"
        throw new InvalidValueException("value has wrong type", e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private T cast(final Object value) {
    return (T) value;
  }

  public T getValue() {
    return getter.get();
  }

  public void resetValue() {
    resetter.run();
  }

  public static <T> MutableProperty<T> of(
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<T> getter,
      final Runnable resetter) {
    return new MutableProperty<>(setter, stringSetter, getter, resetter);
  }

  public static <T> MutableProperty<T> ofReadOnly(final Supplier<T> getter) {
    return of(noSetter(), noStringSetter(), getter, noResetter());
  }

  public static <T> MutableProperty<T> ofWriteOnly(
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter) {
    return of(setter, stringSetter, noGetter(), noResetter());
  }

  public static <T> MutableProperty<T> ofSimple(
      final ThrowingConsumer<T, Exception> setter, final Supplier<T> getter) {
    return of(setter, noStringSetter(), getter, noResetter());
  }

  public static <T> MutableProperty<T> ofReadOnlySimple(final Supplier<T> getter) {
    return ofSimple(noSetter(), getter);
  }

  public static MutableProperty<String> ofString(
      final ThrowingConsumer<String, Exception> setter,
      final Supplier<String> getter,
      final Runnable resetter) {
    return of(setter, setter, getter, resetter);
  }

  public static MutableProperty<String> ofWriteOnlyString(
      final ThrowingConsumer<String, Exception> stringSetter) {
    return ofString(stringSetter, noGetter(), noResetter());
  }

  /**
   * A special convenience method trying to keep everything slightly more functional. Instead of
   * specifying 2 setters, one getter and a resetter with this method only one setter, one getter, a
   * function mapping a String to the setters type and a Supplier supplying the default value that's
   * getting fed in again using the setter are required. This keeps stuff more readable and more
   * testable.
   */
  public static <T> MutableProperty<T> ofMapper(
      final ThrowingFunction<String, T, Exception> mapper,
      final ThrowingConsumer<T, Exception> setter,
      final Supplier<T> getter,
      final Supplier<T> defaultValue) {
    return new MutableProperty<>(
        setter,
        o -> setter.accept(mapper.apply(o)),
        getter,
        () -> {
          try {
            setter.accept(defaultValue.get());
          } catch (final RuntimeException e) {
            throw e;
          } catch (final Exception e) {
            throw new IllegalStateException("Unexpected Error while resetting value", e);
          }
        });
  }

  /**
   * A checked exception that indicates an attempt was made to set a property to an invalid value.
   */
  public static final class InvalidValueException extends Exception {
    private static final long serialVersionUID = 7634850287487589543L;

    private InvalidValueException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
