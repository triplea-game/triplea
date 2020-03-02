package games.strategy.engine.message.unifiedmessenger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteMethodCallResults;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndPointTest {

  @Test
  void testEndPoint() throws Exception {
    final EndPoint endPoint = new EndPoint("", Comparator.class, false);
    endPoint.addImplementor((Comparator<Object>) (o1, o2) -> 2);
    final RemoteMethodCall call =
        new RemoteMethodCall(
            "",
            Comparator.class.getMethod("compare", Object.class, Object.class),
            new Object[] {"", ""});
    final List<RemoteMethodCallResults> results =
        endPoint.invokeLocal(call, endPoint.takeANumber(), null);
    assertEquals(1, results.size());
    assertEquals(2, results.iterator().next().getRVal());
  }
}
