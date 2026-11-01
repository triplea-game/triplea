package org.triplea.ai.benchmark.tabulator;

import java.util.Collection;
import javax.annotation.Nullable;
import org.apache.commons.math3.distribution.TDistribution;

/** Means with two-sided 95% Student-t confidence intervals. */
final class Stats {
  private Stats() {}

  /**
   * A sample's size, mean, and CI half-width. {@code mean} is null for an empty sample and {@code
   * ci} is null below two values.
   */
  record Summary(int n, @Nullable Double mean, @Nullable Double ci) {}

  /**
   * A difference of two means with its Welch CI half-width; {@code ci} is null unless both samples
   * have two or more values.
   */
  record Difference(@Nullable Double mean, @Nullable Double ci) {
    boolean excludesZero() {
      return mean != null && ci != null && Math.abs(mean) > ci;
    }
  }

  static Summary summarize(final Collection<Double> values) {
    final int n = values.size();
    if (n == 0) {
      return new Summary(0, null, null);
    }
    final double mean = mean(values);
    if (n == 1) {
      return new Summary(1, mean, null);
    }
    return new Summary(n, mean, tCritical(n - 1) * Math.sqrt(variance(values, mean) / n));
  }

  /** {@code b} minus {@code a}. */
  static Difference difference(final Collection<Double> a, final Collection<Double> b) {
    if (a.isEmpty() || b.isEmpty()) {
      return new Difference(null, null);
    }
    final double diff = mean(b) - mean(a);
    if (a.size() < 2 || b.size() < 2) {
      return new Difference(diff, null);
    }
    final double va = variance(a, mean(a)) / a.size();
    final double vb = variance(b, mean(b)) / b.size();
    final double se = Math.sqrt(va + vb);
    if (se == 0) {
      return new Difference(diff, 0.0);
    }
    final double df = (va + vb) * (va + vb) / (va * va / (a.size() - 1) + vb * vb / (b.size() - 1));
    return new Difference(diff, tCritical(df) * se);
  }

  private static double tCritical(final double degreesOfFreedom) {
    return new TDistribution(degreesOfFreedom).inverseCumulativeProbability(0.975);
  }

  private static double mean(final Collection<Double> values) {
    return values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
  }

  private static double variance(final Collection<Double> values, final double mean) {
    return values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum() / (values.size() - 1);
  }
}
