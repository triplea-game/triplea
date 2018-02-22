package games.strategy.engine.data;

import java.util.function.Supplier;

/**
 * A wrapper interface to Bundle setters, getters and resetters of the same field.
 *
 * @param <T> The type of the field to set, get and reset.
 */
public interface ModifiableProperty<T> {

  void setValue(String string) throws Exception;

  void setValue(T value) throws Exception;

  /**
   * Calls the appropriate {@link ModifiableProperty#setValue} method.
   */
  @SuppressWarnings("unchecked")
  default void setObjectValue(final Object o) {
    try {
      if (o instanceof String) {
        setValue((String) o);
      } else {
        setValue((T) o);
      }
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to set Attachment property", e);
    }
  }

  T getValue();

  void resetValue();

  /**
   * Convenience method to create an instance of this interface.
   */
  static <T> ModifiableProperty<T> of(
      final ExceptionConsumer<T> setter,
      final ExceptionConsumer<String> stringSetter,
      final Supplier<T> getter,
      final Runnable resetter) {
    return new ModifiableProperty<T>() {

      @Override
      public void setValue(String string) throws Exception {
        stringSetter.accept(string);
      }

      @Override
      public void setValue(T value) throws Exception {
        setter.accept(value);
      }

      @Override
      public T getValue() {
        return getter.get();
      }

      @Override
      public void resetValue() {
        resetter.run();
      }
    };
  }

  /**
   * Convenience method to create an instance of this interface with no resetter.
   */
  static <T> ModifiableProperty<T> of(
      final ExceptionConsumer<T> setter,
      final ExceptionConsumer<String> stringSetter,
      final Supplier<T> getter) {
    return of(setter, stringSetter, getter, () -> throwIllegalStateException("No Resetter"));
  }

  /**
   * Convenience method to create a generic String instance of this interface.
   */
  static ModifiableProperty<String> of(
      final ExceptionConsumer<String> setter,
      final Supplier<String> getter,
      final Runnable resetter) {
    return of(setter, setter, getter, resetter);
  }

  /**
   * Convenience method to create an instance of this interface that only gets.
   */
  static <T> ModifiableProperty<T> of(final Supplier<T> getter) {
    return of(
        t -> throwIllegalStateException("No Setter"),
        t -> throwIllegalStateException("No String Setter"),
        getter,
        () -> throwIllegalStateException("No Resetter"));
  }

  /**
   * Convenience method to create an instance of this interface that only sets, but doesn't reset.
   */
  static <T> ModifiableProperty<T> of(
      final ExceptionConsumer<T> setter,
      final ExceptionConsumer<String> stringSetter) {
    return of(
        setter,
        stringSetter,
        () -> throwIllegalStateException("No Getter"),
        () -> throwIllegalStateException("No Resetter"));
  }


  /**
   * Convenience method to create an instance of this interface that just contains a direct
   * setter and getter. And no support for Strings as secondary setter.
   */
  static <T> ModifiableProperty<T> ofSimple(
      final ExceptionConsumer<T> setter,
      final Supplier<T> getter) {
    return of(setter,
        o -> throwIllegalStateException("No String Setter"),
        getter,
        () -> throwIllegalStateException("No Resetter"));
  }


  /**
   * Convenience method to create an instance of this interface that just contains a
   * getter. And no support for setters of any kind.
   */
  static <T> ModifiableProperty<T> ofSimple(final Supplier<T> getter) {
    return ofSimple(e -> throwIllegalStateException("No Setter"), getter);
  }

  static <T> T throwIllegalStateException(final String text) {
    throw new IllegalStateException(text);
  }

  /**
   * Convenience method to create an instance of this interface that only sets via the string value.
   */
  static <T> ModifiableProperty<T> ofWriteOnlyString(final ExceptionConsumer<String> stringSetter) {
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
   */
  @FunctionalInterface
  static interface ExceptionConsumer<T> {
    void accept(T object) throws Exception;
  }
}
