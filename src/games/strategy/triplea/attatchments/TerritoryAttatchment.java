/*
 * TerritoryAttatchment.java
 *
 * Created on November 8, 2001, 3:08 PM
 */

package games.strategy.triplea.attatchments;

import games.strategy.engine.data.DefaultAttatchment;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TerritoryAttatchment extends DefaultAttatchment
{
	
	/**
	 * Conveniente method.
	 */
	public static TerritoryAttatchment get(Territory t)
	{
		return (TerritoryAttatchment) t.getAttatchment(Constants.TERRITORY_ATTATCHMENT_NAME);
	}
	
	private String m_capital = null;
	private boolean m_originalFactory = false;
	private int m_production = 2;

	/** Creates new TerritoryAttatchment */
    public TerritoryAttatchment() 
	{
    }
	
	public void setCapital(String value)
	{
		m_capital = value;
	}
	
	public boolean isCapital()
	{
		return m_capital != null;
	}
	
	public String getCapital()
	{
		return m_capital;
	}
	
	public void setOriginalFactory(String value)
	{
		m_originalFactory = getBool(value);
	}
	
	public boolean isOriginalFactory()
	{
		return m_originalFactory;
	}
	
	public void setProduction(String value)
	{
		m_production = getInt(value);
	}
	
	public int getProduction()
	{
		return m_production;
	}
}