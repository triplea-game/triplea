package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.data.properties.*;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;

import java.io.*;
import java.util.List;

import junit.framework.TestCase;

public class DiceRollTest extends TestCase
{
    
    private GameData m_data;

    @Override
    protected void setUp() throws Exception
    {
        File gameRoot  = GameRunner.getRootFolder();
        File gamesFolder = new File(gameRoot, "games");
        File lhtr = new File(gamesFolder, "revised.xml");
        
        if(!lhtr.exists())
            throw new IllegalStateException("revised does not exist");
        
        InputStream input = new BufferedInputStream(new FileInputStream(lhtr));
        
        try
        {
            m_data = (new GameParser()).parse(input);
        }
        finally
        {
            input.close();    
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        m_data = null;
    }

    
    
    public void testSimple()
    {
        Territory westRussia = m_data.getMap().getTerritory("West Russia");
        MockBattle battle = new MockBattle(westRussia);
        PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
        
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, russians);
     
        
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        List<Unit> infantry = infantryType.create(1, russians);
        
        //infantry defends and hits at 1 (0 based)
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
        DiceRoll roll = DiceRoll.rollDice( infantry, true, russians, bridge, m_data, battle);
        assertEquals(1, roll.getHits());
        
        //infantry does not hit at 2 (0 based)
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {2}));
        DiceRoll roll2 = DiceRoll.rollDice( infantry, true, russians, bridge, m_data, battle);
        assertEquals(0, roll2.getHits());
        
        
        //infantry attacks and hits at 0 (0 based)
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {0}));
        DiceRoll roll3 = DiceRoll.rollDice( infantry, false, russians, bridge, m_data, battle);
        assertEquals(1, roll3.getHits());
        
        //infantry attack does not hit at 1 (0 based)
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
        DiceRoll roll4 = DiceRoll.rollDice( infantry, false, russians, bridge, m_data, battle);
        assertEquals(0, roll4.getHits());
        
    }
    
    
    public void testArtillerySupport()
    {
        Territory westRussia = m_data.getMap().getTerritory("West Russia");
        MockBattle battle = new MockBattle(westRussia);
        PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
        
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, russians);
     
        
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        List<Unit> units = infantryType.create(1, russians);
        
        UnitType artillery = m_data.getUnitTypeList().getUnitType("artillery");
        units.addAll(artillery.create(1, russians));
        
        //artileery supported infantry and art attack at 1 (0 based)
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {1,1}));
        DiceRoll roll = DiceRoll.rollDice( units, false, russians, bridge, m_data, battle);
        assertEquals(2, roll.getHits());
    }
    
    public void testLowLuck()
    {
        for(IEditableProperty property : m_data.getProperties().getEditableProperties())
        {
            if(property.getName().equals(Constants.LOW_LUCK))
            {
                 ((BooleanProperty)  property).setValue(true);
            }
        }
        
        Territory westRussia = m_data.getMap().getTerritory("West Russia");
        MockBattle battle = new MockBattle(westRussia);
        PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
        
        TestDelegateBridge bridge = new TestDelegateBridge(m_data, russians);
     
        
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        List<Unit> units = infantryType.create(3, russians);
        
       
        //3 infantry on defense should produce exactly one hit, without rolling the dice
        bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
        
        DiceRoll roll = DiceRoll.rollDice( units, true, russians, bridge, m_data, battle);
        assertEquals(1, roll.getHits());
    }
    
}
