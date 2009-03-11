package games.strategy.triplea.ui;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class MouseDetails
{
    private final MouseEvent m_mouseEvent;
    //the x position of the event on the map
    //this is in absolute pixels of the unscaled map
    private final double m_x;
    //the x position of the event on the map
    //this is in absolute pixels of the unscaled map

    private final double m_y;

    public MouseDetails(final MouseEvent mouseEvent, final double x, final double y)
    {
        super();
        m_mouseEvent = mouseEvent;
        m_x = x;
        m_y = y;
    }

    public MouseEvent getMouseEvent()
    {
        return m_mouseEvent;
    }

    public double getX()
    {
        return m_x;
    }

    public double getY()
    {
        return m_y;
    }


    public boolean isRightButton()
    {
        return (m_mouseEvent.getModifiers() & InputEvent.BUTTON3_MASK )!= 0;
    }

    public boolean isControlDown()
    {
        return m_mouseEvent.isControlDown();
    }

    public boolean isShiftDown()
    {
       return m_mouseEvent.isShiftDown();
    }
    
    public boolean isAltDown()
    {
       return m_mouseEvent.isAltDown();
    }

    /**
     * 
     * @return this point is in the map co-ordinates, unscaled
     */
    public Point getMapPoint()
    {
        return new Point((int)m_x,(int) m_y);
    }

    public int getButton()
    {
        return m_mouseEvent.getButton();
    }

    
    
    

}
