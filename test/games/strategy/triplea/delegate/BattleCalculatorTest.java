package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.*;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.LoadGameUtil;

import java.util.Collection;

import junit.framework.TestCase;

public class BattleCalculatorTest extends TestCase 
{
	
	private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {        
        m_data = LoadGameUtil.loadGame("revised", "revised.xml");
    }
	
	public void testAACasualtiesLowLuck() 
	{
		makeGameLowLuck(m_data);
		DiceRoll roll = new DiceRoll(new int[] {1}, 2, 1, false);
		Collection<Unit> planes = bomber(m_data).create(5, british(m_data));
		Collection<Unit> casualties = BattleCalculator.getAACasualties(planes, roll, null, null, null, m_data, null, null);
		assertEquals(casualties.size(), 2);
	}

}
