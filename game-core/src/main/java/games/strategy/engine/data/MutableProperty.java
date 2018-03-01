package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Supplier;

import com.google.common.reflect.TypeToken;

/**
 * A wrapper interface to Bundle setters, getters and resetters of the same field.
 *
 * @param <T> The type of the property value.
 */
public final class MutableProperty<T> {
  private final TypeToken<T> typeToken;
  private final ThrowingConsumer<T, Exception> setter;
  private final ThrowingConsumer<String, Exception> stringSetter;
  private final Supplier<T> getter;
  private final Runnable resetter;

  private MutableProperty(
      final TypeToken<T> typeToken,
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<T> getter,
      final Runnable resetter) {
    this.typeToken = checkNotNull(typeToken);
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
   * @param value The new property value. If a {@link String}, the value will be set using {@link #stringSetter};
   *        otherwise the value will be set using {@link #setter}.
   *
   * @throws InvalidValueException If the new property value is invalid.
   */
  public void setValue(final Object value) throws InvalidValueException {
    // TODO: do we need to allow null values? if so, document it; if not, add precondition check

    if (value instanceof String) {
      setStringValue((String) value);
    } else {
      setTypedValue(cast(value));
    }
  }

  private T cast(final Object value) throws InvalidValueException {
    @SuppressWarnings("unchecked")
    final Class<T> type = (Class<T>) typeToken.getRawType();
    try {
      return type.cast(value);
    } catch (final ClassCastException e) {
      throw new InvalidValueException(
          String.format("expected value of type '%s' but was '%s'", typeToken, value.getClass().getName()),
          e);
    }
  }

  public T getValue() {
    return getter.get();
  }

  public void resetValue() {
    resetter.run();
  }

  public static <T> MutableProperty<T> of(
      final TypeToken<T> typeToken,
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<T> getter,
      final Runnable resetter) {
    return new MutableProperty<>(typeToken, setter, stringSetter, getter, resetter);
  }

  public static <T> MutableProperty<T> ofReadOnly(
      final TypeToken<T> typeToken,
      final Supplier<T> getter) {
    return of(typeToken, noSetter(), noStringSetter(), getter, noResetter());
  }

  public static <T> MutableProperty<T> ofWriteOnly(
      final TypeToken<T> typeToken,
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter) {
    return of(typeToken, setter, stringSetter, noGetter(), noResetter());
  }

  public static <T> MutableProperty<T> ofSimple(
      final TypeToken<T> typeToken,
      final ThrowingConsumer<T, Exception> setter,
      final Supplier<T> getter) {
    return of(typeToken, setter, noStringSetter(), getter, noResetter());
  }

  public static <T> MutableProperty<T> ofReadOnlySimple(final TypeToken<T> typeToken, final Supplier<T> getter) {
    return ofSimple(typeToken, noSetter(), getter);
  }

  public static MutableProperty<String> ofString(
      final ThrowingConsumer<String, Exception> setter,
      final Supplier<String> getter,
      final Runnable resetter) {
    return of(TypeToken.of(String.class), setter, setter, getter, resetter);
  }

  public static MutableProperty<String> ofWriteOnlyString(final ThrowingConsumer<String, Exception> stringSetter) {
    return ofString(stringSetter, noGetter(), noResetter());
  }

  /**
   * A Consumer capable of throwing an exception.
   *
   * @param <T> The type of Object to consume.
   * @param <E> The type of Throwable to throw.
   */
  @FunctionalInterface
  public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T object) throws E;
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
