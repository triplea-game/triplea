package games.strategy.triplea.ai;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;

/** The random numbers AI decisions draw on. A tool replaying a game reproducibly replaces them. */
@UtilityClass
public final class AiRandom {
  private static volatile Supplier<Random> source = ThreadLocalRandom::current;

  public static Random get() {
    return source.get();
  }

  /** Draws AI random numbers from {@code newSource}; null restores the default. */
  public static void setSource(final @Nullable Supplier<Random> newSource) {
    source = newSource == null ? ThreadLocalRandom::current : newSource;
  }
}
