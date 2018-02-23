package games.strategy.util;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.base.MoreObjects;

public class Triple<F, S, T> implements Serializable {
  private static final long serialVersionUID = -8188046743232005918L;
  private final Tuple<F, S> tuple;
  private final T third;

  /**
   * Static creation method to create a new instance of a triple with the parameters provided.
   *
   * <p>
   * This method allows for nicer triple creation syntax, namely:
   * </p>
   *
   * <pre>
   * Triple&lt;String, Integer, String> myTriple = Triple.of("abc", 123, "xyz");
   * </pre>
   *
   * <p>
   * Instead of:
   * </p>
   *
   * <pre>
   * Triple&lt;String, Integer, String> myTriple = new Triple&lt;String, Integer, String>("abc", 123, "xyz");
   * </pre>
   */
  public static <F, S, T> Triple<F, S, T> of(final F first, final S second, final T third) {
    return new Triple<>(first, second, third);
  }

  private Triple(final F first, final S second, final T third) {
    tuple = Tuple.of(first, second);
    this.third = third;
  }

  public F getFirst() {
    return tuple.getFirst();
  }

  public S getSecond() {
    return tuple.getSecond();
  }

  public T getThird() {
    return third;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("first", getFirst())
        .add("second", getSecond())
        .add("third", getThird())
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(tuple, third);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }

    final Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
    return Objects.equals(tuple, other.tuple)
        && Objects.equals(getThird(), other.getThird());
  }
}
