package games.strategy.engine.data;

import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * A wrapper interface to Bundle setters, getters and resetters of the same field.
 *
 * @param <T> The type of the field to set, get and reset.
 */
public class MutableProperty<T> {

  private final ThrowingConsumer<T, Exception> setter;
  private final ThrowingConsumer<String, Exception> stringSetter;
  private final Supplier<T> getter;
  private final Runnable resetter;

  private MutableProperty(
      @Nullable final ThrowingConsumer<T, Exception> setter,
      @Nullable final ThrowingConsumer<String, Exception> stringSetter,
      @Nullable final Supplier<T> getter,
      @Nullable final Runnable resetter) {
    this.setter = setter != null ? setter : o -> {
      throw new UnsupportedOperationException("No Setter has been defined!");
    };
    this.stringSetter = stringSetter != null ? stringSetter : s -> {
      throw new UnsupportedOperationException("No String Setter has been defined!");
    };
    this.getter = getter != null ? getter : () -> {
      throw new UnsupportedOperationException("No Getter has been defined!");
    };
    this.resetter = resetter != null ? resetter : () -> {
      throw new UnsupportedOperationException("No Resetter has been defined!");
    };
  }

  public void setValue(final String string) throws Exception {
    stringSetter.accept(string);
  }

  public void setValue(final T value) throws Exception {
    setter.accept(value);
  }

  /**
   * Calls the appropriate {@link MutableProperty#setValue} method.
   */
  @SuppressWarnings("unchecked")
  public void setObjectValue(final Object o) {
    try {
      if (o instanceof String) {
        setValue((String) o);
      } else {
        setValue((T) o);
      }
    } catch (final Exception e) {
      throw new IllegalStateException(e);
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
    return of(setter, stringSetter, getter, null);
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
   * Convenience method to create an instance of this interface that only gets.
   */
  public static <T, E extends Throwable> MutableProperty<T> of(final Supplier<T> getter) {
    return of(null, null, getter, null);
  }

  /**
   * Convenience method to create an instance of this interface that only sets, but doesn't reset.
   */
  public static <T> MutableProperty<T> of(
      final ThrowingConsumer<T, Exception> setter,
      final ThrowingConsumer<String, Exception> stringSetter) {
    return of(setter, stringSetter, null, null);
  }


  /**
   * Convenience method to create an instance of this interface that just contains a direct
   * setter and getter. And no support for Strings as secondary setter.
   */
  public static <T> MutableProperty<T> ofSimple(
      final ThrowingConsumer<T, Exception> setter,
      final Supplier<T> getter) {
    return of(setter, null, getter, null);
  }


  /**
   * Convenience method to create an instance of this interface that just contains a
   * getter. And no support for setters of any kind.
   */
  public static <T> MutableProperty<T> ofSimple(final Supplier<T> getter) {
    return ofSimple(null, getter);
  }

  /**
   * Convenience method to create an instance of this interface that only sets via the string value.
   */
  public static <T> MutableProperty<T> ofWriteOnlyString(final ThrowingConsumer<String, Exception> stringSetter) {
    return of(
        t -> throwIllegalStateException("No Setter"),
        stringSetter,
        () -> throwIllegalStateException("No Getter"),
        () -> throwIllegalStateException("No Resetter"));
  }

  /**
   * A Consumer capable of throwing a GameParseException.
   *
   * @param <T> The type of Object to consume.
   * @param <E> The type of Throwable to throw.
   */
  @FunctionalInterface
  public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T object) throws E;
  }
}
