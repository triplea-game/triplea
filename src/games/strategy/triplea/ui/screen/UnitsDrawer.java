package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.MapData;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Tuple;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.naming.CompositeName;

public class UnitsDrawer implements IDrawable
{
    private final int m_count;
    private final String m_unitType;
    private final String m_playerName;
    private final Point m_placementPoint;
    private final boolean m_damaged;
    private final boolean m_overflow;
    private final String m_territoryName;
    
    public UnitsDrawer(final int count, final String unitType, final String playerName, final Point placementPoint, final boolean damaged, boolean overflow, String territoryName)
    {
        m_count = count;
        m_unitType = unitType;
        m_playerName = playerName;
        m_placementPoint = placementPoint;
        m_damaged = damaged;
        m_overflow = overflow;
        m_territoryName = territoryName;
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
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        if(m_overflow)
        {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(m_placementPoint.x - bounds.x -2, m_placementPoint.y - bounds.y + UnitImageFactory.UNIT_ICON_HEIGHT , UnitImageFactory.UNIT_ICON_WIDTH + 2,  3 );
        }
        
        UnitType type = data.getUnitTypeList().getUnitType(m_unitType);
        if (type == null)
            throw new IllegalStateException("Type not found:" + m_unitType);
        PlayerID owner = data.getPlayerList().getPlayerID(m_playerName);

        Image img = UnitImageFactory.instance().getImage(type, owner, data, m_damaged);
        graphics.drawImage(img, m_placementPoint.x - bounds.x, m_placementPoint.y - bounds.y, null);

        if (m_count != 1)
        {
            graphics.setColor(Color.white);
            graphics.setFont(MapImage.MAP_FONT);
            graphics.drawString(String.valueOf(m_count), m_placementPoint.x - bounds.x + (UnitImageFactory.instance().getUnitImageWidth() / 4),
                    m_placementPoint.y - bounds.y + UnitImageFactory.instance().getUnitImageHeight());
        }
    }
    

    public Tuple<Territory,List<Unit>> getUnits(GameData data)
    {
        //note - it may be the case where the territory is being changed as a result
        //to a mouse click, and the map units havent updated yet, so the unit count
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
        
        if(rVal.size() != m_count)
            throw new IllegalStateException("Wrong units expected, expecting :" + m_count + " but got " + rVal);
        
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
