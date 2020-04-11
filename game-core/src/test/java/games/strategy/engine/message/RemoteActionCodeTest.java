package games.strategy.engine.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.stream.IntStream;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.provider.CsvFileSource;

public class RemoteActionCodeTest {

  @ParameterizedTest
  @CsvFileSource(resources = "/required-op-codes.txt")
  void verifyCorrectOpCode(
      final Class<?> clazz,
      final int opCode,
      final String methodName,
      @AggregateWith(ClassAggregator.class) final Class<?>[] parameterTypes)
      throws Exception {
    var method = clazz.getMethod(methodName, parameterTypes);
    var remoteActionCode = method.getAnnotation(RemoteActionCode.class);

    assumeTrue(remoteActionCode != null);

    assertThat(remoteActionCode.value(), is(opCode));
  }

  static class ClassAggregator implements ArgumentsAggregator {
    @Override
    public Class<?>[] aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
      return IntStream.range(context.getIndex(), arguments.size())
          .mapToObj(i -> arguments.get(i, Class.class))
          .toArray(Class<?>[]::new);
    }
  }
}
