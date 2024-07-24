package games.strategy.engine.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.lang.reflect.Method;
import java.util.stream.IntStream;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.provider.CsvFileSource;

/**
 * Test class that verifies that remote operation codes didn't change unexpectedly due to some
 * seemingly unrelated.
 *
 * <p>Please update {@code /required-op-codes.csv} accordingly in case the remote interfaces are
 * ever altered. All lines in {@code /required-op-codes.csv} must follow the scheme:
 *
 * <p>{@code op-code,interface-name,method-name,method-param-type-0,...,method-param-type-n}
 */
public class RemoteActionCodeTest {

  @ParameterizedTest
  @CsvFileSource(resources = "/required-op-codes.csv")
  void verifyCorrectOpCode(
      final int opCode, @AggregateWith(MethodAggregator.class) final Method method) {
    final RemoteActionCode remoteActionCode = method.getAnnotation(RemoteActionCode.class);

    assertThat(
        "Expected @RemoteActionCode annotation to be present for " + method,
        remoteActionCode,
        is(notNullValue()));

    assertThat("Invalid value for " + method, remoteActionCode.value(), is(opCode));
  }

  /**
   * Helper class that aggregates the fields found in the csv of test data to a {@link Method}. In
   * order for this to work as expected, the CSVs last n + 2 columns must follow the format:
   *
   * <p>{@code interface-name,method-name,method-param-type-0,...,method-param-type-n}
   *
   * <p>Note that due to the variable length of the type arguments
   * {@code @AggregateWith(MethodAggregator.class)} can only ever be used on the last parameter of a
   * test method to work properly, otherwise it might pickup the wrong arguments.
   */
  static class MethodAggregator implements ArgumentsAggregator {
    @Override
    public Method aggregateArguments(
        final ArgumentsAccessor arguments, final ParameterContext context)
        throws ArgumentsAggregationException {
      // Ignore CSV columns that are already used by different parameters
      final int offset = context.getIndex();
      final Class<?> remoteInterface = arguments.get(offset, Class.class);
      final String methodName = arguments.getString(offset + 1);
      final Class<?>[] argumentTypes =
          IntStream.range(offset + 2, arguments.size())
              .mapToObj(i -> arguments.get(i, Class.class))
              .toArray(Class<?>[]::new);
      try {
        return remoteInterface.getMethod(methodName, argumentTypes);
      } catch (final NoSuchMethodException e) {
        throw new ArgumentsAggregationException("Invalid method specified: " + methodName, e);
      }
    }
  }
}
