package games.strategy.util;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.base.MoreObjects;

public class Tuple<T, S> implements Serializable {
  private static final long serialVersionUID = -5091545494950868125L;
  private final T first;
  private final S second;


  /**
   * Static creation method to create a new instance of a tuple with the parameters provided.
   *
   * <p>
   * This method allows for nicer tuple creation syntax, namely:
   * </p>
   *
   * <pre>
   * Tuple&lt;String, Integer> myTuple = Tuple.of("abc", 123);
   * </pre>
   *
   * <p>
   * Instead of:
   * </p>
   *
   * <pre>
   * Tuple&lt;String, Integer> myTuple = new Tuple&lt;String, Integer>("abc", 123);
   * </pre>
   */
  public static <T, S> Tuple<T, S> of(final T first, final S second) {
    return new Tuple<>(first, second);
  }

  private Tuple(final T first, final S second) {
    this.first = first;
    this.second = second;
  }

  public T getFirst() {
    return first;
  }

  public S getSecond() {
    return second;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("first", getFirst())
        .add("second", getSecond())
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, second);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }

    // ignore parameterization, just perform equals-check on components
    final Tuple<?, ?> other = (Tuple<?, ?>) obj;
    return Objects.equals(getFirst(), other.getFirst())
        && Objects.equals(getSecond(), other.getSecond());
  }
}
