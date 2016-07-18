package games.strategy.engine.message.unifiedmessenger;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.List;

import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteMethodCallResults;
import games.strategy.engine.message.unifiedmessenger.EndPoint;
import org.junit.Test;

import games.strategy.test.TestUtil;

public class EndPointTest {

  @Test
  public void testEndPoint() {
    final EndPoint endPoint = new EndPoint("", Comparator.class, false);
    endPoint.addImplementor((Comparator<Object>) (o1, o2) -> 2);
    final RemoteMethodCall call = new RemoteMethodCall("", "compare", new Object[] {"", ""},
        TestUtil.getClassArrayFrom(Object.class, Object.class), Comparator.class);
    final List<RemoteMethodCallResults> results = endPoint.invokeLocal(call, endPoint.takeANumber(), null);
    assertEquals(results.size(), 1);
    assertEquals(2, (results.iterator().next()).getRVal());
  }
}
