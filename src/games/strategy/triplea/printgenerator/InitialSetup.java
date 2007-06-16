package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.history.HistoryNode;
import games.strategy.triplea.attatchments.UnitAttachment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Gansito Frito
 *
 */
public class InitialSetup
{
    private Iterator<UnitType> m_unitTypeIterator;
    private Iterator<PlayerID> m_playerIterator;
    private Map<UnitType, UnitAttachment> m_unitInfoMap=new HashMap<UnitType, UnitAttachment>();
    private GameData m_data;


    protected InitialSetup()
    {

    }

    /**
     * @param GameData data
     * @param boolean useOriginalState
     */
    protected void run(GameData data, boolean useOriginalState)
    {
        m_data = data;
        PrintGenerationData.setData(m_data);
        
        if (useOriginalState)
        {
            HistoryNode root = (HistoryNode) m_data.getHistory().getRoot();
            m_data.getHistory().gotoNode(root);
        }

        m_unitTypeIterator = m_data.getUnitTypeList().iterator();
        while (m_unitTypeIterator.hasNext())
        {
            UnitType currentType = m_unitTypeIterator.next();
            UnitAttachment currentTypeUnitAttachment=UnitAttachment.get(currentType);
            m_unitInfoMap.put(currentType, currentTypeUnitAttachment);
        }
        
        new UnitInformation().saveToFile(m_unitInfoMap);
        
        m_playerIterator=m_data.getPlayerList().iterator();
        while(m_playerIterator.hasNext())
        {
            PlayerID currentPlayer=m_playerIterator.next();
            CountryChart.saveToFile(currentPlayer, m_data);
            
        }
        IPCInfo.saveToFile(m_data);
        try
        {
            PlayerOrder.saveToFile(m_data);
            IPCChart chart=new IPCChart();
            chart.saveToFile();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
    }
}
