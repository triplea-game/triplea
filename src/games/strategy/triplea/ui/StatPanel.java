/*
 * StatPanel.java
 *
 * Created on January 6, 2002, 4:07 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.util.*;
import javax.swing.*;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameDataChangeListener;

import games.strategy.util.IntegerMap;

import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.Constants;

/**
 *
 * @author  Sean Bridges
 */
public class StatPanel extends JPanel
{
	private final GameData m_data;
	private final StatsCalculator m_calc;
	private JTextArea m_text;
	private Collection m_playerStats = new ArrayList();
	
	/** Creates a new instance of InfoPanel */
    public StatPanel(GameData data) 
	{
		data.addDataChangeListener(m_dataChangeListener);
	
		m_text = new JTextArea();
		m_text.setEditable(false);
		m_text.setBackground(getBackground());
		
		JScrollPane scroll = new JScrollPane(m_text);
		
		setLayout(new BorderLayout());
		add(scroll, BorderLayout.CENTER);
		
		m_data = data;
		m_calc = new StatsCalculator(data);
		
		initStats();
		updateStats();
    }
	
	private void initStats()
	{
		Iterator iter = m_data.getPlayerList().getPlayers().iterator();
		while(iter.hasNext())
		{
			m_playerStats.add( new PlayerStats( (PlayerID) iter.next(), m_calc));
		}
	}
	
	private void updateStats()
	{
		m_text.setText("");
		Iterator iter = m_playerStats.iterator();
		while(iter.hasNext())
		{
			PlayerStats stats = (PlayerStats) iter.next();
			m_text.append(stats.getStats());
		}
	}
	
	private GameDataChangeListener m_dataChangeListener = new GameDataChangeListener()
	{
		public void gameDataChanged()
		{
			updateStats();
		}
	};
}

class PlayerStats 
{
	private PlayerID m_id;
	private StatsCalculator m_calc;
	
	PlayerStats(PlayerID id, StatsCalculator calc)
	{
		m_id = id;
		m_calc = calc;
	}
	
	public String getStats()
	{
		StringBuffer buf = new StringBuffer();
		buf.append(m_id.getName());
		buf.append("\n");
		buf.append("IPCS");
		buf.append(String.valueOf( m_calc.getIPCSinHand(m_id)));
		buf.append("\n");
		buf.append("Production:");
		buf.append(String.valueOf(m_calc.getProduction(m_id)));
		buf.append("\n");
		buf.append("Units:");
		buf.append(String.valueOf(m_calc.getUnitsPlaced(m_id).totalValues() + m_calc.getUnitsNotPlaced(m_id).totalValues()));
		buf.append("\n");
		
		String advances = m_calc.getAdvances(m_id);
		if(advances != null)
		{
			buf.append("Tech advances:").append("\n");
			buf.append(advances);
			buf.append("\n");
		}
		
		
		buf.append("\n");
		return buf.toString();
	}	
}

class StatsCalculator 
{
	private final GameData m_data;
	
	StatsCalculator(GameData data)
	{
		m_data = data;
	}

	/**
	 * @ retrun null if no advances.  
	 */
	public String getAdvances(PlayerID id)
	{
		Map advanceProperty =  (Map) m_data.getProperties().get(Constants.TECH_PROPERTY);
		if(advanceProperty == null)
			return null;
		
		Collection advances = (Collection) advanceProperty.get(id);
		
		if(advances == null || advances.size() == 0)
			return null;
		
		StringBuffer advanceString = new StringBuffer();
		Iterator iter = advances.iterator();
		while(iter.hasNext())
		{
			advanceString.append(iter.next());
			if(iter.hasNext())
				advanceString.append("\n");
		}
		
		return advanceString.toString();	
	}
	
	public int getProduction(PlayerID id)
	{
		int sum = 0;
		Iterator territories = m_data.getMap().iterator();
		while(territories.hasNext())
		{
			Territory current = (Territory) territories.next();
			if(current.getOwner().equals(id))
			{
				TerritoryAttatchment ta = TerritoryAttatchment.get(current);
				sum += ta.getProduction();
			}
		}
		return sum;
	}
	
	public IntegerMap getUnitsPlaced(PlayerID id)
	{
		IntegerMap placed = new IntegerMap();
		Iterator territories = m_data.getMap().iterator();
		while(territories.hasNext())
		{
			Territory current = (Territory) territories.next();
			if(current.getOwner().equals(id))
			{
				placed.add( current.getUnits().getUnitsByType(id));
			}
		}
		return placed;
	}
	
	public IntegerMap getUnitsNotPlaced(PlayerID id)
	{
		return id.getUnits().getUnitsByType();
	}
	
	public int getIPCSinHand(PlayerID id)
	{
		return id.getResources().getQuantity(Constants.IPCS);
	}	
}