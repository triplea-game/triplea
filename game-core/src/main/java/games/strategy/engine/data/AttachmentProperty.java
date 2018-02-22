package games.strategy.engine.data;

import java.util.function.Supplier;

/**
 * A wrapper interface to Bundle setters, getters and resetters of the same field.
 *
 * @param <T> The type of the field to set, get and reset.
 */
public interface AttachmentProperty<T> {

  void setValue(String string) throws GameParseException;

  void setValue(T value) throws GameParseException;

  @SuppressWarnings("unchecked")
  default void setObjectValue(final Object o) {
    try {
      if (o instanceof String) {
        setValue((String) o);
      } else {
        setValue((T) o);
      }
    } catch (GameParseException e) {
      throw new IllegalStateException("Failed to set Attachment property", e);
    }
  }

  T getValue();

  void resetValue();

  /**
   * Convenience method to create an instance of this interface.
   */
  static <T> AttachmentProperty<T> of(
      final GameParsingConsumer<T> setter,
      final GameParsingConsumer<String> stringSetter,
      final Supplier<T> getter,
      final Runnable resetter) {
    return new AttachmentProperty<T>() {

      @Override
      public void setValue(String string) throws GameParseException {
        stringSetter.accept(string);
      }

      @Override
      public void setValue(T value) throws GameParseException {
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
  static <T> AttachmentProperty<T> of(
      final GameParsingConsumer<T> setter,
      final GameParsingConsumer<String> stringSetter,
      final Supplier<T> getter) {
    return of(setter, stringSetter, getter, () -> throwIllegalStateException("No Resetter"));
  }

  /**
   * Convenience method to create a generic String instance of this interface.
   */
  static AttachmentProperty<String> of(
      final GameParsingConsumer<String> setter,
      final Supplier<String> getter,
      final Runnable resetter) {
    return of(setter, setter, getter, resetter);
  }

  /**
   * Convenience method to create an instance of this interface that only gets.
   */
  static <T> AttachmentProperty<T> of(final Supplier<T> getter) {
    return of(
        t -> throwIllegalStateException("No Setter"),
        t -> throwIllegalStateException("No Setter"),
        getter,
        () -> throwIllegalStateException("No Resetter"));
  }

  /**
   * Convenience method to create an instance of this interface that only sets, but doesn't reset.
   */
  static <T> AttachmentProperty<T> of(
      final GameParsingConsumer<T> setter,
      final GameParsingConsumer<String> stringSetter) {
    return of(
        setter,
        stringSetter,
        () -> throwIllegalStateException("No Getter"),
        () -> throwIllegalStateException("No Resetter"));
  }

  /**
   * Convenience method to create an instance of this interface that only sets via the string value.
   */
  static <T> AttachmentProperty<T> ofWriteOnlyString(final GameParsingConsumer<String> stringSetter) {
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
  static interface GameParsingConsumer<T> {
    void accept(T object) throws GameParseException;
  }

  static <T> T throwIllegalStateException(final String text) {
    throw new IllegalStateException(text);
  }
}
