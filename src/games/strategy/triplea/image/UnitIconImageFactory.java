/*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
* UnitIconImageFactory.java
*
* Created on November 25, 2001, 8:27 PM
 */

package games.strategy.triplea.image;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import games.strategy.triplea.Constants;
import javax.swing.ImageIcon;

import games.strategy.util.*;
import games.strategy.ui.Util;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.PlayerID;

import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.engine.data.GameData;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class UnitIconImageFactory
{

  private static UnitIconImageFactory s_instance = new UnitIconImageFactory();

  public static UnitIconImageFactory instance()
  {
    return s_instance;
  }

  private TechTracker getTechTracker(GameData data)
  {
    return DelegateFinder.techDelegate(data).getTechTracker();
  }

  boolean m_loaded = false;
  /**
   * Width of all icons.
   */
  public static final int UNIT_ICON_WIDTH = 30;
  /**
   * Height of all icons.
   **/
  public static final int UNIT_ICON_HEIGHT = 28;



  private static final String FILE_NAME = "images/units2.gif";

  //maps Point -> image
  private final Map m_images = new HashMap();
  //maps Point -> ICon
  private final Map m_icons = new HashMap();

  /** Creates new IconImageFactory */
  private UnitIconImageFactory()
  {

  }

  private void copyImage(int row, int column, Component comp, Image source)
  {
    Image image = Util.createImage(UNIT_ICON_WIDTH, UNIT_ICON_HEIGHT);
    Graphics g = image.getGraphics();
    int sx = column * UNIT_ICON_WIDTH;
    int sy = row * UNIT_ICON_HEIGHT;
    g.drawImage(source, 0,0, UNIT_ICON_WIDTH, UNIT_ICON_HEIGHT, sx, sy, sx + UNIT_ICON_WIDTH, sy + UNIT_ICON_HEIGHT, comp);
    m_images.put(new Point(row, column), image);

  }

  /**
   * Loads the images, does not return till all images
   * have finished loading.
   */
  public synchronized void load(Component observer) throws IOException
  {
    if(m_loaded)
      throw new IllegalStateException("Already loaded");

    Image image = Toolkit.getDefaultToolkit().getImage( this.getClass().getResource(FILE_NAME));
    //wait for the image to load
    MediaTracker tracker = new MediaTracker(observer);
    tracker.addImage(image, 1);
    try
    {
      tracker.waitForAll();
      } catch(InterruptedException e)
      {
        e.printStackTrace(System.out);
        System.out.println("try again");
        load(observer);
      }


      for (int column = 0; column <= 12; column++)
      {
          copyImage(0, column, observer, image);
      }

      m_loaded = true;
  }

  private void ensurePlayerLoaded(PlayerID player)
  {
    int row = getPlayerRow(player);
    if(m_images.get(new Point(row, 0)) == null)
    {
      Point sourcePoint = new Point(0, 0);
      Point destPoint = new Point(row, 0);
      Image src = (Image) m_images.get(sourcePoint);
      javax.swing.JComponent obs = new javax.swing.JLabel();
      while(src != null)
      {
          Image image = Util.createImage(UNIT_ICON_WIDTH, UNIT_ICON_HEIGHT);
          Graphics g = image.getGraphics();
          g.drawImage(src,0,0,  obs);
          g.drawImage(FlagIconImageFactory.instance().getSmallFlag(player),0,0, obs);
          m_images.put(new Point(destPoint), image);

          destPoint.y++;

          sourcePoint.y++;
          src = (Image) m_images.get(sourcePoint);
      }
    }
  }

  public Image getImage(UnitType type, PlayerID player, GameData data)
  {
    if(m_loaded == false)
      throw new IllegalArgumentException("Images not loaded");

    ensurePlayerLoaded(player);

    Image img = (Image) m_images.get(getImageLocation(type, player, data));
    if(img == null)
      throw new IllegalArgumentException("Image not found. Type:" + type.getName() + " Player:" + player + " point:" + getImageLocation(type, player, data));
    return img;
  }

  public ImageIcon getIcon(UnitType type, PlayerID player, GameData data)
  {
    if(m_loaded == false)
      throw new IllegalArgumentException("Images not loaded");

    Point location =getImageLocation(type, player, data);
    ImageIcon icon = (ImageIcon) m_icons.get(location);

    if(icon == null)
    {
      Image img = getImage(type, player, data);
      icon = new ImageIcon(img);
      m_icons.put(location, icon);
    }

    return icon;
  }

  public Point getImageLocation(UnitType type, PlayerID id, GameData data)
  {
    int row = 0;
    int column = 0;

    row = getPlayerRow(id);

    //find the type
    if(type.getName().equals(Constants.INFANTRY_TYPE))
    {
      column = 1;
    }
    else if(type.getName().equals(Constants.ARMOUR_TYPE))
    {
      column = 0;

    }
    else if(type.getName().equals(Constants.FIGHTER_TYPE))
    {
      column = 2;


      boolean longRange = getTechTracker(data).hasLongRangeAir(id);
      boolean jetFIghter = getTechTracker(data).hasJetFighter(id);

      if(jetFIghter)
        column = 10;
    }
    else if(type.getName().equals(Constants.BOMBER_TYPE))
    {
      column = 3;

      boolean longRange = getTechTracker(data).hasLongRangeAir(id);
      boolean heavyBomber = getTechTracker(data).hasHeavyBomber(id);

      if(longRange)
        column = 9;


    }
    else if(type.getName().equals(Constants.TRANSPORT_TYPE))
    {
      column = 5;
    }
    else if(type.getName().equals(Constants.SUBMARINE_TYPE))
    {
      column = 8;
    }
    else if(type.getName().equals(Constants.CARRIER_TYPE))
    {
      column = 4;
    }
    else if(type.getName().equals(Constants.BATTLESHIP_TYPE))
    {
      column = 6;
    }
    else if(type.getName().equals(Constants.AAGUN_TYPE))
    {
      column = 7;

      if(getTechTracker(data).hasRocket(id))
      {}
    }
    else if(type.getName().equals(Constants.FACTORY_TYPE))
    {
      column = 11;

      if(getTechTracker(data).hasIndustrialTechnology(id))
      {}
    }
    else
      throw new IllegalArgumentException("unrecognized type:" + type);

    return new Point(row, column);
  }

private int getPlayerRow(PlayerID id) throws IllegalArgumentException
{
    int row;
    //find the player
    if(id.getName().equals(Constants.RUSSIANS))
      row = 1;
    else if(id.getName().equals(Constants.GERMANS))
      row = 2;
    else if(id.getName().equals(Constants.BRITISH))
      row = 3;
    else if(id.getName().equals(Constants.JAPANESE))
      row = 4;
    else if(id.getName().equals(Constants.AMERICANS))
      row = 5;
    else if(id == PlayerID.NULL_PLAYERID)
      row = 0;
    else
      throw new IllegalArgumentException("player not recognized:" + id);
    return row;
}

}