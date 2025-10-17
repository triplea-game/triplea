package games.strategy.engine.data;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

public interface IPropertyEnum<A> {
  String getValue();

  Function<A, MutableProperty<?>> getPropertyAccessor();

  default MutableProperty<?> getMutableProperty(A attachment) {
    return getPropertyAccessor().apply(attachment);
  }

  static <E extends Enum<E> & IPropertyEnum<?>> Optional<E> parseFromString(
      Class<E> enumClass, String propertyName) {
    return Arrays.stream(enumClass.getEnumConstants())
        .filter(e -> e.getValue().equals(propertyName))
        .findAny();
  }

  static <E extends Enum<E> & IPropertyEnum<A>, A> Optional<MutableProperty<?>> getPropertyOrEmpty(
      Class<E> enumClass, String propertyName, A attachment) {
    return parseFromString(enumClass, propertyName).map(p -> p.getMutableProperty(attachment));
  }
}
