package games.strategy.util;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.base.MoreObjects;

public class Tuple<T, S> implements Serializable {
  private static final long serialVersionUID = -5091545494950868125L;
  private final T m_first;
  private final S m_second;


  /**
   * Static creation method to create a new instance of a tuple with the parameters provided.
   *
   * This method allows for nicer tuple creation syntax, namely:
   * Tuple<String,Integer> myTuple = Tuple.of("abc",123);
   * Instead of:
   * Tuple<String,Integer> myTuple = Tuple.of("abc",123);
   */
  public static <T, S> Tuple<T, S> of(final T first, final S second) {
    return new Tuple(first, second);
  }

  protected Tuple(final T first, final S second) {
    this.m_first = first;
    this.m_second = second;
  }

  public T getFirst() {
    return m_first;
  }

  public S getSecond() {
    return m_second;
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
    return Objects.hash(m_first, m_second);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final Tuple other = (Tuple) obj;
    return Objects.equals(getFirst(), other.getFirst())
        && Objects.equals(getSecond(), other.getSecond());
  }
}
