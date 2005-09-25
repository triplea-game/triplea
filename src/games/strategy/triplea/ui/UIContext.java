package games.strategy.triplea.ui;

import games.strategy.triplea.image.*;

/**
 * A place to find images and map data for a ui.
 * 
 * @author sgb
 */
public class UIContext
{
    private MapData m_mapData;
    private TileImageFactory m_tileImageFactory = new TileImageFactory();
    private String m_mapDir;
    private UnitImageFactory m_unitImageFactory;
    private MapImage m_mapImage ;
    private FlagIconImageFactory m_flagIconImageFactory = new FlagIconImageFactory();
    private DiceImageFactory m_diceImageFactory = new DiceImageFactory();

    public UIContext()
    {
        m_mapImage = new MapImage();
    }

    public void setMapDir(String dir)
    {
        m_mapData = new MapData(dir);
        m_tileImageFactory.setMapDir(dir);

        m_mapImage.loadMaps(dir); // load map data
        
        //only create once
        if(m_mapDir == null)
        {
            m_unitImageFactory = new UnitImageFactory(m_mapData.getDefaultUnitScale());
        }

        m_mapDir = dir;
    }
    
    public MapData getMapData()
    {
        return m_mapData;
    }

    public String getMapDir()
    {
        return m_mapDir;
    }
    
    public TileImageFactory getTileImageFactory()
    {
        return m_tileImageFactory;
    }
    
    public UnitImageFactory getUnitImageFactory()
    {
        return m_unitImageFactory;
    }
    
    public MapImage getMapImage()
    {
        return m_mapImage;
    }
    
    public FlagIconImageFactory getFlagImageFactory()
    {
        return m_flagIconImageFactory;
    }
    
    public DiceImageFactory getDiceImageFactory()
    {
        return m_diceImageFactory;
    }
    
}
