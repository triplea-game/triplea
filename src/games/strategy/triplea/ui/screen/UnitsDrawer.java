package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.ui.*;
import games.strategy.util.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

public class UnitsDrawer implements IDrawable
{
    private final int m_count;
    private final String m_unitType;
    private final String m_playerName;
    private final Point m_placementPoint;
    private final boolean m_damaged;
    private final boolean m_overflow;
    private final String m_territoryName;
    private final UIContext m_uiContext;
    
    public UnitsDrawer(final int count, final String unitType, final String playerName, final Point placementPoint, final boolean damaged, boolean overflow, String territoryName, UIContext uiContext)
    {
        m_count = count;
        m_unitType = unitType;
        m_playerName = playerName;
        m_placementPoint = placementPoint;
        m_damaged = damaged;
        m_overflow = overflow;
        m_territoryName = territoryName;
        m_uiContext = uiContext;
    }

    public void prepare() {}
    
    public Point getPlacementPoint()
    {
        return m_placementPoint;
    }
    
    public String getPlayer()
    {
        return m_playerName;
    }
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {
        if(m_overflow)
        {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(m_placementPoint.x - bounds.x -2, m_placementPoint.y - bounds.y + m_uiContext.getUnitImageFactory().getUnitImageHeight() , m_uiContext.getUnitImageFactory().getUnitImageWidth() + 2,  3 );
        }
        
        UnitType type = data.getUnitTypeList().getUnitType(m_unitType);
        if (type == null)
            throw new IllegalStateException("Type not found:" + m_unitType);
        PlayerID owner = data.getPlayerList().getPlayerID(m_playerName);

        Image img =  m_uiContext.getUnitImageFactory().getImage(type, owner, data, m_damaged);
        graphics.drawImage(img, m_placementPoint.x - bounds.x, m_placementPoint.y - bounds.y, null);

        if (m_count != 1)
        {
            graphics.setColor(Color.white);
            graphics.setFont(MapImage.MAP_FONT);
            graphics.drawString(String.valueOf(m_count), m_placementPoint.x - bounds.x + (m_uiContext.getUnitImageFactory().getUnitImageWidth() / 4),
                    m_placementPoint.y - bounds.y + m_uiContext.getUnitImageFactory().getUnitImageHeight());
        }
        
        //Display Factory Damage
        if(type.getName().equals("factory"))
        {
        	displayFactoryDamage(bounds, data, graphics, type);
        }
    }

	private void displayFactoryDamage(Rectangle bounds, GameData data,
			Graphics2D graphics, UnitType type) {

		graphics.setColor(Color.black);
		graphics.setFont(MapImage.MAP_FONT);

		TerritoryAttachment ta = TerritoryAttachment.get(data.getMap().getTerritory(m_territoryName));       
		int damageCount = ta.getProduction() - ta.getUnitProduction();

		if(damageCount>0)
		{
			graphics.drawString(String.valueOf(damageCount), m_placementPoint.x - bounds.x + (m_uiContext.getUnitImageFactory().getUnitImageWidth() / 4),
					m_placementPoint.y - bounds.y + m_uiContext.getUnitImageFactory().getUnitImageHeight()/4);
		}
        
	}
    

    public Tuple<Territory,List<Unit>> getUnits(GameData data)
    {
        //note - it may be the case where the territory is being changed as a result
        //to a mouse click, and the map units haven't updated yet, so the unit count
        //from the territory wont match the units in m_count
        
        Territory t = data.getMap().getTerritory(m_territoryName);
        UnitType type = data.getUnitTypeList().getUnitType(m_unitType);
        
        CompositeMatch<Unit> selectedUnits = new CompositeMatchAnd<Unit>();
        selectedUnits.add(Matches.unitIsOfType(type));
        selectedUnits.add(Matches.unitIsOwnedBy(data.getPlayerList().getPlayerID(m_playerName) ));
        if(m_damaged)
            selectedUnits.add(Matches.UnitIsDamaged);
        else
            selectedUnits.add(Matches.UnitIsNotDamaged);
        
        List<Unit> rVal = t.getUnits().getMatches(selectedUnits);

        return new Tuple<Territory,List<Unit>>(t,rVal);
    }
    
    public int getLevel()
    {
        return UNITS_LEVEL;
    }
    
    public String toString()
    {
        return "UnitsDrawer for " + m_count + " " + MyFormatter.pluralize(m_unitType) + " in  " + m_territoryName; 
    }

}
