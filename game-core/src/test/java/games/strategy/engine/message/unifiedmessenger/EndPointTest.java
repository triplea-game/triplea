package games.strategy.engine.message.unifiedmessenger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteMethodCallResults;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndPointTest {

  interface TestInterface {
    @RemoteActionCode(0)
    @SuppressWarnings("unused")
    int dummy();
  }

  @Test
  void testEndPoint() throws Exception {
    final EndPoint endPoint = new EndPoint("", TestInterface.class, false);
    endPoint.addImplementor((TestInterface) () -> 2);
    final RemoteMethodCall call =
        new RemoteMethodCall("", TestInterface.class.getMethod("dummy"), new Object[] {});
    final List<RemoteMethodCallResults> results =
        endPoint.invokeLocal(call, endPoint.takeANumber(), null);
    assertEquals(1, results.size());
    assertEquals(2, results.iterator().next().getRVal());
  }
}
