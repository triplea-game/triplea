package games.strategy.triplea.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.IntegerMap;

public class AbstractEndTurnDelegateTest extends DelegateTest {

  @Test
  public void testFindEstimatedIncome() throws Exception {
    final GameData global40Data = TestMapGameData.GLOBAL1940.getGameData();
    final PlayerID germans = GameDataTestUtil.germans(global40Data);
    final IntegerMap<Resource> results = AbstractEndTurnDelegate.findEstimatedIncome(germans, global40Data);
    final int pus = results.getInt(new Resource(Constants.PUS, global40Data));
    assertEquals(40, pus);
  }

}
