package org.triplea.util;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A heterogeneous container of two values.
 *
 * @param <T> The type of the first value.
 * @param <S> The type of the second value.
 * @deprecated Instead use a simple value object, eg:
 *     <pre>{@code
 * @Value
 * class ValueObject {
 *   String first;
 *   String second;
 * }
 * }</pre>
 */
@Getter
@EqualsAndHashCode
@Deprecated
public final class Tuple<T, S> implements Serializable {
  private static final long serialVersionUID = -5091545494950868125L;
  private final T first;
  private final S second;

  private Tuple(final T first, final S second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Static creation method to create a new instance of a tuple with the parameters provided.
   *
   * <p>This method allows for nicer tuple creation syntax, namely:
   *
   * <pre>
   * Tuple&lt;String, Integer> myTuple = Tuple.of("abc", 123);
   * </pre>
   *
   * <p>Instead of:
   *
   * <pre>
   * Tuple&lt;String, Integer> myTuple = new Tuple&lt;String, Integer>("abc", 123);
   * </pre>
   */
  public static <T, S> Tuple<T, S> of(final T first, final S second) {
    return new Tuple<>(first, second);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("first", getFirst())
        .add("second", getSecond())
        .toString();
  }
}
