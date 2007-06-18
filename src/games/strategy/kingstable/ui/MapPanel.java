package games.strategy.kingstable.ui;

import games.strategy.common.image.UnitImageFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.kingstable.attachments.TerritoryAttachment;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class MapPanel extends JComponent implements MouseListener
{
    private MapData m_mapData;
    private GameData m_gameData;
    
    private Territory m_clickedAt = null;
    private Territory m_releasedAt = null;
    
    private Map<Territory, Image> m_images;
    
    public MapPanel(MapData mapData)
    {
        m_mapData = mapData;
        m_gameData = m_mapData.getGameData();
        
        Dimension mapDimension = m_mapData.getMapDimensions();
        
        this.setMinimumSize(mapDimension);
        this.setPreferredSize(mapDimension);
        this.setSize(mapDimension);

        m_images = new HashMap<Territory, Image>();
        for (Territory at : m_mapData.getPolygons().keySet())
        {
            updateImage(at);
        }
        
        this.addMouseListener(this);
        
        this.setOpaque(true);
    }
    
    
    /**
     * Get the size of the map.
     * 
     * @return the size of the map
     */
    public Dimension getMapDimensions()
    {
        return m_mapData.getMapDimensions();
    }
    
    
    /**
     * Update the user interface based on a game play.
     * 
     * @param start <code>Territory</code> where the moving piece began
     * @param end <code>Territory</code> where the moving piece ended
     * @param captured <code>Collection</code> of <code>Territory</code>s whose pieces were captured during the play
     */
    protected void performPlay(Territory start, Territory end, Collection<Territory> captured)
    {   
        updateImage(start);
        updateImage(end);
        
        for (Territory at : captured)
            updateImage(at);
        
        // Ask Swing to repaint this panel when it's convenient
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                repaint();
            }
        });
    }
    
    /**
     * Update the image for a <code>Territory</code> based on the contents of that <code>Territory</code>.
     * 
     * @param at the <code>Territory</code> to update
     */
    private void updateImage(Territory at)
    {
        if (at != null)
        {
            if (at.getUnits().size() == 1)
            {  
                UnitImageFactory f = new UnitImageFactory();
                // Get image for exactly one unit
                Unit u = (Unit) at.getUnits().getUnits().toArray()[0];
                Image image = f.getImage(u.getType(), u.getOwner(), m_gameData);
                m_images.put(at, image); 
            } else
            {
                m_images.remove(at);
            }
        }
    }
    
    
    protected void paintComponent(Graphics g) 
    {   
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());

        for (Map.Entry<Territory,Polygon> entry : m_mapData.getPolygons().entrySet())
        {   
            Polygon p = entry.getValue();
            Territory at = entry.getKey();
            Color backgroundColor = Color.WHITE;

            TerritoryAttachment ta = (TerritoryAttachment) at.getAttachment("territoryAttachment");
            if (ta!=null)
            {   
                if (ta.isKingsExit())
                    backgroundColor = new Color(225,225,255);
                else if (ta.isKingsSquare())
                    backgroundColor = new Color(235,235,235);

                g.setColor(backgroundColor);
                g.fillPolygon(p);
            }

            g.setColor(Color.black);
            Image image = m_images.get(at);

            if (image != null)
            {
                Rectangle square = p.getBounds(); 
                g.drawImage(image, square.x, square.y, square.width, square.height, backgroundColor, null);
            }

            g.drawPolygon(p);                

        }
    }
    

    public void mouseClicked(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    
    public void mousePressed(MouseEvent e) 
    { 
        int x = e.getX();
        int y = e.getY();
        m_clickedAt = m_mapData.getTerritoryAt(x, y);
    }
    
    public void mouseReleased(MouseEvent e) 
    { 
        int x = e.getX();
        int y = e.getY();
        m_releasedAt = m_mapData.getTerritoryAt(x, y);
    }
    
    public PlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge) 
    {
        Territory clickedAt = null;
        Territory releasedAt = null;
        
        while (clickedAt == releasedAt)
        {
            while (m_clickedAt == null) {}
            clickedAt = m_clickedAt;

            while (m_releasedAt == null) {}
            releasedAt = m_releasedAt;

            m_clickedAt = null;
            m_releasedAt = null;
        }
        
        return new PlayData(clickedAt, releasedAt);
    }

}
