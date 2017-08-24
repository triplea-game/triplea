package games.strategy.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;

import com.google.common.reflect.TypeToken;

/**
 * A service for obtaining an {@link EqualityComparator} for a specific type that can be used to compare two instances
 * of that type for equality independent of {@link Object#equals(Object)}.
 */
@Immutable
public final class EqualityComparatorRegistry {
  private static final EqualityComparator DEFAULT_EQUALITY_COMPARATOR =
      EqualityComparator.newInstance(Object.class, (context, o1, o2) -> o1.equals(o2));

  private final Map<Class<?>, EqualityComparator> equalityComparatorsByType;

  private EqualityComparatorRegistry(final Map<Class<?>, EqualityComparator> equalityComparatorsByType) {
    this.equalityComparatorsByType = Collections.unmodifiableMap(equalityComparatorsByType);
  }

  /**
   * Gets an equality comparator for the specified type.
   *
   * <p>
   * The registry attempts to look up an equality comparator for the specified type in the following order:
   * </p>
   *
   * <ol>
   * <li>If an equality comparator for the exact type has been registered, use it.</li>
   * <li>If an equality comparator for any interface implemented by the type has been registered, use it.</li>
   * <li>Otherwise, use the default equality comparator that delegates to {@link Object#equals(Object)}.
   * </ol>
   *
   * @param type The type for which an equality comparator is desired.
   *
   * @return An equality comparator for the specified type.
   */
  public EqualityComparator getEqualityComparatorFor(final Class<?> type) {
    checkNotNull(type);

    EqualityComparator equalityComparator = equalityComparatorsByType.get(type);
    if (equalityComparator != null) {
      return equalityComparator;
    }

    for (final TypeToken<?> typeToken : TypeToken.of(type).getTypes().interfaces()) {
      equalityComparator = equalityComparatorsByType.get(typeToken.getRawType());
      if (equalityComparator != null) {
        return equalityComparator;
      }
    }

    return DEFAULT_EQUALITY_COMPARATOR;
  }

  public static EqualityComparatorRegistry newInstance(final EqualityComparator... equalityComparators) {
    return newInstance(Arrays.asList(equalityComparators));
  }

  public static EqualityComparatorRegistry newInstance(final Collection<EqualityComparator> equalityComparators) {
    return new EqualityComparatorRegistry(
        equalityComparators.stream().collect(Collectors.toMap(EqualityComparator::getType, Function.identity())));
  }
}
