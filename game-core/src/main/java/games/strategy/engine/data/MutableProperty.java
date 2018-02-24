package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Supplier;

/**
 * A wrapper interface to Bundle setters, getters and resetters of the same field.
 *
 * @param <T> The type of the field to set, get and reset.
 */
public final class MutableProperty<T> {
  private final Class<T> type;
  private final ThrowingConsumer<T, Exception> setter;
  private final ThrowingConsumer<String, Exception> stringSetter;
  private final Supplier<T> getter;
  private final Runnable resetter;

  private MutableProperty(
      final Class<T> type,
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<T> getter,
      final Runnable resetter) {
    this.type = checkNotNull(type);
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
    try {
      return type.cast(value);
    } catch (final ClassCastException e) {
      throw new InvalidValueException(
          String.format("expected value of type '%s' but was '%s'", type, value.getClass()),
          e);
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
      final Class<T> type,
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<T> getter,
      final Runnable resetter) {
    return new MutableProperty<>(type, setter, stringSetter, getter, resetter);
  }

  /**
   * Convenience method to create an instance of this interface with no resetter.
   */
  public static <T> MutableProperty<T> of(
      final Class<T> type,
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<T> getter) {
    return of(type, setter, stringSetter, getter, noResetter());
  }

  /**
   * Convenience method to create an instance of this interface that only gets.
   */
  public static <T> MutableProperty<T> of(final Class<T> type, final Supplier<T> getter) {
    return of(type, noSetter(), noStringSetter(), getter, noResetter());
  }

  /**
   * Convenience method to create an instance of this interface that only sets, but doesn't reset.
   */
  public static <T> MutableProperty<T> of(
      final Class<T> type,
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter) {
    return of(type, setter, stringSetter, noGetter(), noResetter());
  }

  /**
   * Convenience method to create a generic read-write Boolean instance of this interface.
   */
  public static MutableProperty<Boolean> ofBoolean(
      final ThrowingConsumer<Boolean, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<Boolean> getter,
      final Runnable resetter) {
    return of(Boolean.class, setter, stringSetter, getter, resetter);
  }

  /**
   * Convenience method to create a generic read-only Boolean instance of this interface.
   */
  public static MutableProperty<Boolean> ofReadOnlyBoolean(final Supplier<Boolean> getter) {
    return of(Boolean.class, noSetter(), noStringSetter(), getter, noResetter());
  }

  /**
   * Convenience method to create a generic write-only Boolean instance of this interface.
   */
  public static MutableProperty<Boolean> ofWriteOnlyBoolean(
      final ThrowingConsumer<Boolean, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter) {
    return of(Boolean.class, setter, stringSetter, noGetter(), noResetter());
  }

  /**
   * Convenience method to create a generic read-write Integer instance of this interface.
   */
  public static MutableProperty<Integer> ofInteger(
      final ThrowingConsumer<Integer, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter,
      final Supplier<Integer> getter,
      final Runnable resetter) {
    return of(Integer.class, setter, stringSetter, getter, resetter);
  }

  /**
   * Convenience method to create a generic read-only Integer instance of this interface.
   */
  public static MutableProperty<Integer> ofReadOnlyInteger(final Supplier<Integer> getter) {
    return of(Integer.class, noSetter(), noStringSetter(), getter, noResetter());
  }

  /**
   * Convenience method to create a generic String instance of this interface.
   */
  public static MutableProperty<String> ofString(
      final ThrowingConsumer<String, Exception> setter,
      final Supplier<String> getter,
      final Runnable resetter) {
    return of(String.class, setter, setter, getter, resetter);
  }

  /**
   * Convenience method to create an instance of this interface that just contains a direct
   * setter and getter. And no support for Strings as secondary setter.
   */
  public static <T> MutableProperty<T> ofSimple(
      final Class<T> type,
      final ThrowingConsumer<T, Exception> setter,
      final Supplier<T> getter) {
    return of(type, setter, noStringSetter(), getter, noResetter());
  }

  /**
   * Convenience method to create an instance of this interface that just contains a
   * getter. And no support for setters of any kind.
   */
  public static <T> MutableProperty<T> ofSimple(final Class<T> type, final Supplier<T> getter) {
    return ofSimple(type, noSetter(), getter);
  }

  /**
   * Convenience method to create an instance of this interface that just contains a direct
   * setter and getter for a Boolean. And no support for Strings as secondary setter.
   */
  public static MutableProperty<Boolean> ofSimpleBoolean(
      final ThrowingConsumer<Boolean, Exception> setter,
      final Supplier<Boolean> getter) {
    return of(Boolean.class, setter, noStringSetter(), getter, noResetter());
  }

  /**
   * Convenience method to create an instance of this interface that just contains a direct
   * setter and getter for an Integer. And no support for Strings as secondary setter.
   */
  public static MutableProperty<Integer> ofSimpleInteger(
      final ThrowingConsumer<Integer, Exception> setter,
      final Supplier<Integer> getter) {
    return of(Integer.class, setter, noStringSetter(), getter, noResetter());
  }

  /**
   * Convenience method to create an instance of this interface that only sets via the string value.
   */
  public static MutableProperty<String> ofWriteOnlyString(final ThrowingConsumer<String, Exception> stringSetter) {
    return of(String.class, noSetter(), stringSetter, noGetter(), noResetter());
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
