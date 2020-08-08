package org.triplea.util;

import java.io.Serializable;
import lombok.Value;

/**
 * A heterogeneous container of three values.
 *
 * @param <F> The type of the first value.
 * @param <S> The type of the second value.
 * @param <T> The type of the third value.
 */
@Value(staticConstructor = "of")
public final class Triple<F, S, T> implements Serializable {
  private static final long serialVersionUID = -8188046743232005918L;
  private final F first;
  private final S second;
  private final T third;
}
