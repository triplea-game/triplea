package games.strategy.triplea.ui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.beans.*;
import games.strategy.ui.*;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: </p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class MapPanelSmallView extends ImageScrollerSmallView
{
    //the image with the current map
    private final Image m_mapImage;

    public  MapPanelSmallView(Image img)
    {
        super(Util.createImage(img.getWidth(null), img.getHeight(null)));
        m_mapImage = img;
    }

    public Image getMapImage()
    {
        return m_mapImage;
    }

    public void resetOffScreen()
    {
        getOffScreenImage().getGraphics().drawImage(m_mapImage, 0,0, this);
    }

}
