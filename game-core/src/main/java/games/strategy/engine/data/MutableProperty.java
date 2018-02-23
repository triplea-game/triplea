package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Supplier;

/**
 * A wrapper interface to Bundle setters, getters and resetters of the same field.
 *
 * @param <T> The type of the field to set, get and reset.
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
   * @param value The new property value. If a {@link String}, the value will be set using {@link #stringSetter};
   *        otherwise the value will be set using {@link #setter}.
   *
   * @throws InvalidValueException If the new property value is invalid.
   */
  @SuppressWarnings("unchecked")
  public void setValue(final Object value) throws InvalidValueException {
    if (value instanceof String) {
      setStringValue((String) value);
    } else {
      setTypedValue((T) value);
    }
  }

  public T getValue() {
    return getter.get();
  }

  public void resetValue() {
    resetter.run();
  }

  /**
   * Convenience method to create an instance of this interface.
   */
  public static <T> MutableProperty<T> of(
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<T> getter,
      final Runnable resetter) {
    return new MutableProperty<>(setter, stringSetter, getter, resetter);
  }

  /**
   * Convenience method to create an instance of this interface with no resetter.
   */
  public static <T> MutableProperty<T> of(
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<T> getter) {
    return of(setter, stringSetter, getter, noResetter());
  }

  /**
   * Convenience method to create an instance of this interface that only gets.
   */
  public static <T> MutableProperty<T> of(final Supplier<T> getter) {
    return of(noSetter(), noStringSetter(), getter, noResetter());
  }

  /**
   * Convenience method to create an instance of this interface that only sets, but doesn't reset.
   */
  public static <T> MutableProperty<T> of(
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter) {
    return of(setter, stringSetter, noGetter(), noResetter());
  }

  /**
   * Convenience method to create a generic String instance of this interface.
   */
  public static MutableProperty<String> ofString(
      final ThrowingConsumer<String, Exception> setter,
      final Supplier<String> getter,
      final Runnable resetter) {
    return of(setter, setter, getter, resetter);
  }

  /**
   * Convenience method to create an instance of this interface that just contains a direct
   * setter and getter. And no support for Strings as secondary setter.
   */
  public static <T> MutableProperty<T> ofSimple(
      final ThrowingConsumer<T, Exception> setter,
      final Supplier<T> getter) {
    return of(setter, noStringSetter(), getter, noResetter());
  }

  /**
   * Convenience method to create an instance of this interface that just contains a
   * getter. And no support for setters of any kind.
   */
  public static <T> MutableProperty<T> ofSimple(final Supplier<T> getter) {
    return ofSimple(noSetter(), getter);
  }

  /**
   * Convenience method to create an instance of this interface that only sets via the string value.
   */
  public static <T> MutableProperty<T> ofWriteOnlyString(final ThrowingConsumer<String, Exception> stringSetter) {
    return of(noSetter(), stringSetter, noGetter(), noResetter());
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
