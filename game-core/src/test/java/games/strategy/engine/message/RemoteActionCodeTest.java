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

public class RemoteActionCodeTest {

  @ParameterizedTest
  @CsvFileSource(resources = "/required-op-codes.txt")
  void verifyCorrectOpCode(
      final int opCode, @AggregateWith(MethodAggregator.class) final Method method) {
    final var remoteActionCode = method.getAnnotation(RemoteActionCode.class);

    assertThat("No annotation present for " + method, remoteActionCode, is(notNullValue()));

    assertThat("Invalid value for " + method, remoteActionCode.value(), is(opCode));
  }

  static class MethodAggregator implements ArgumentsAggregator {
    @Override
    public Method aggregateArguments(
        final ArgumentsAccessor arguments, final ParameterContext context)
        throws ArgumentsAggregationException {
      final int offset = context.getIndex();
      final Class<?> clazz = arguments.get(offset, Class.class);
      final String methodName = arguments.getString(offset + 1);
      final Class<?>[] args =
          IntStream.range(offset + 2, arguments.size())
              .mapToObj(i -> arguments.get(i, Class.class))
              .toArray(Class<?>[]::new);
      try {
        return clazz.getMethod(methodName, args);
      } catch (final NoSuchMethodException e) {
        throw new ArgumentsAggregationException("Invalid method specified", e);
      }
    }
  }
}
