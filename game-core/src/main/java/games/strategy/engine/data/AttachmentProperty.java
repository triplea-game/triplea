package games.strategy.engine.data;

import java.util.function.Supplier;

public interface AttachmentProperty<T> {

  void setValue(String string) throws GameParseException;

  void setValue(T value) throws GameParseException;

  T getValue();

  void resetValue();

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

  static AttachmentProperty<String> of(
      final GameParsingConsumer<String> setter,
      final Supplier<String> getter,
      final Runnable resetter) {
    return of(setter, setter, getter, resetter);
  }

  static <T> AttachmentProperty<T> of(final Supplier<T> getter) {
    return of(t -> throwIllegalStateException("No Setter"),
        t -> throwIllegalStateException("No Setter"),
        getter,
        () -> throwIllegalStateException("No Resetter"));
  }

  @FunctionalInterface
  static interface GameParsingConsumer<T> {
    void accept(T object) throws GameParseException;
  }

  static void throwIllegalStateException(final String text) {
    throw new IllegalStateException(text);
  }
}
