package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameRunner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class OddsCalculatorTest extends TestCase
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
    
    
    public void testUnbalancedFight()
    {
        Territory germany = m_data.getMap().getTerritory("Germany");
        List<Unit> defendingUnits = new ArrayList<Unit>(germany.getUnits().getUnits());
        PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        List<Unit> attackingUnits = m_data.getUnitTypeList().getUnitType("infantry").create(100, russians);
        List<Unit> bombardingUnits = Collections.emptyList();
        
        OddsCalculator calculator = new OddsCalculator();
        AggregateResults results = calculator.calculate(m_data, russians, germans, germany, attackingUnits, defendingUnits, bombardingUnits, 5000);
       
        assertTrue(results.getAttackerWinPercent() > 0.99);        
        assertTrue(results.getDefenderWinPercent() < 0.1);
        assertTrue(results.getDrawPercent() < 0.1 );
    }
    
    
    public void testBalancedFight()
    {
        //1 british tank in eastern canada, defending one german tank
        //odds for win/loss/tie are all equal
        
        Territory eastCanada = m_data.getMap().getTerritory("Eastern Canada");
        List<Unit> defendingUnits = new ArrayList<Unit>(eastCanada.getUnits().getUnits());
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        List<Unit> attackingUnits = m_data.getUnitTypeList().getUnitType("armour").create(1,germans, false);
        List<Unit> bombardingUnits = Collections.emptyList();
        
        OddsCalculator calculator = new OddsCalculator();
        AggregateResults results = calculator.calculate(m_data, germans, british, eastCanada, attackingUnits, defendingUnits, bombardingUnits, 10000);
       
        
        assertEquals(0.33, results.getAttackerWinPercent(), 0.05);
        assertEquals(0.33, results.getDefenderWinPercent(), 0.05);
        assertEquals(0.33, results.getDrawPercent(), 0.05);
        
    }
    
    
    public void testNoUnitsInTerritory()
    {
        //fight 1 tank against 1 tank,
        //where none of the defending units are in the territry
        //and we ignore some units that are in the territory
        
        Territory uk = m_data.getMap().getTerritory("United Kingdom");
        
        
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        List<Unit> attackingUnits = m_data.getUnitTypeList().getUnitType("armour").create(1,germans);
        List<Unit> bombardingUnits = Collections.emptyList();
        
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        List<Unit> defendingUnits = m_data.getUnitTypeList().getUnitType("armour").create(1,british);
        
        OddsCalculator calculator = new OddsCalculator();
        AggregateResults results = calculator.calculate(m_data, germans, british, uk, attackingUnits, defendingUnits, bombardingUnits, 5000);
       
        
        assertEquals(0.33, results.getAttackerWinPercent(), 0.05);
        assertEquals(0.33, results.getDefenderWinPercent(), 0.05);
        assertEquals(0.33, results.getDrawPercent(), 0.05);
    }


    
    
    

    
}
