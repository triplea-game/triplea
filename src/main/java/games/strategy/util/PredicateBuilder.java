package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Predicate;;

public class PredicateBuilder<T> {

  private Predicate<T> predicate;

  private PredicateBuilder(final Predicate<T> predicate) {
    this.predicate = checkNotNull(predicate);
  }

  public static <T> PredicateBuilder<T> of(final Predicate<T> predicate) {
    return new PredicateBuilder<>(predicate);
  }

  public PredicateBuilder<T> and(final Predicate<T> predicate) {
    this.predicate = this.predicate.and(predicate);
    return this;
  }

  public PredicateBuilder<T> andIf(final boolean condition, final Predicate<T> predicate) {
    return condition ? and(predicate) : this;
  }

  public PredicateBuilder<T> or(final Predicate<T> predicate) {
    this.predicate = this.predicate.or(predicate);
    return this;
  }

  public PredicateBuilder<T> orIf(final boolean condition, final Predicate<T> predicate) {
    return condition ? or(predicate) : this;
  }

  public Predicate<T> build() {
    return predicate;
  }
}
