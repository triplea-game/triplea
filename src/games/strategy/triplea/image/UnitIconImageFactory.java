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
import games.strategy.triplea.Constants;
import javax.swing.ImageIcon;

import games.strategy.ui.Util;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.PlayerID;

import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.engine.data.GameData;
import java.net.*;

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


  /**
   * Width of all icons.
   */
  public static final int UNIT_ICON_WIDTH = 48;

  /**
   * Height of all icons.
   **/
  public static final int UNIT_ICON_HEIGHT = 48;

  private static final String FILE_NAME_BASE = "images/units/";

  //maps Point -> image
  private final Map m_images = new HashMap();
  //maps Point -> Icon
  private final Map m_icons = new HashMap();
  // Scaling factor for unit images
  private double m_scaleFactor = 1;

  /** Creates new IconImageFactory */
  private UnitIconImageFactory()
  {

  }

  /**
   * Set the unitScaling factor
   */
  public void setScaleFactor(double scaleFactor) {
    if (m_scaleFactor != scaleFactor) {
      m_scaleFactor = scaleFactor;
      clearImageCache();
    }
  }

  /**
   * Return the unit scaling factor
   */
  public double getScaleFactor() {
    return m_scaleFactor;
  }

  /**
   * Return the width of scaled units
   */
  public int getUnitImageWidth() {
    return (int)(m_scaleFactor * UNIT_ICON_WIDTH);
  }

  /**
   * Return the height of scaled units
   */
  public int getUnitImageHeight() {
    return (int)(m_scaleFactor * UNIT_ICON_HEIGHT);
  }

  // Clear the image and icon cache
  private void clearImageCache()
  {
    m_images.clear();
    m_icons.clear();
  }

  /**
   * Return the appropriate unit image.
   */
  public Image getImage(UnitType type, PlayerID player, GameData data, boolean damaged)
  {
    String baseName = getBaseImageName(type, player, data, damaged);
    String fullName = baseName + player.getName();
    if(m_images.containsKey(fullName))
    {
      return (Image) m_images.get(fullName);
    }

    Image baseImage = getBaseImage(baseName, player, damaged);

    // We want to scale units according to the given scale factor.
    // We use smooth scaling since the images are cached to allow
    // to take our time in doing the scaling.
    // Image observer is null, since the image should have been
    // guaranteed to be loaded.
    int width = (int) (baseImage.getWidth(null) * m_scaleFactor);
    int height = (int) (baseImage.getHeight(null) * m_scaleFactor);
    Image scaledImage = baseImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

    // Ensure the scaling is completed.
    try
    {
      Util.ensureImageLoaded(scaledImage, new java.awt.Label());
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
    }

    m_images.put(fullName, scaledImage);
    return scaledImage;
  }

  private Image getBaseImage(String baseImageName, PlayerID id, boolean damaged)
  {
    String fileName = FILE_NAME_BASE + id.getName() + "/"  + baseImageName  + ".png";
    URL url = this.getClass().getResource(fileName);
    if(url == null)
      throw new IllegalStateException("Cant load :"+ baseImageName + " looking in:" + fileName);

    Image image = Toolkit.getDefaultToolkit().getImage(url);
    try
    {
      Util.ensureImageLoaded(image, new java.awt.Label());
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
    }

    return image;
  }

  /**
   * Return a icon image for a unit.
   */
  public ImageIcon getIcon(UnitType type, PlayerID player, GameData data, boolean damaged)
  {
    String baseName = getBaseImageName(type, player, data, damaged);
    String fullName = baseName + player.getName();
    if(m_icons.containsKey(fullName))
    {
      return (ImageIcon) m_icons.get(fullName);
    }

    Image img = getBaseImage(baseName, player, damaged);
    ImageIcon icon = new ImageIcon(img);
    m_icons.put(fullName, icon);

    return icon;
  }

  public String getBaseImageName(UnitType type, PlayerID id, GameData data, boolean damaged)
  {
    StringBuffer name = new StringBuffer(32);
    name.append(type.getName());

    if (type.getName().equals(Constants.FIGHTER_TYPE))
    {
      if (TechTracker.hasLongRangeAir(id))
      {
	name.append("_lr");
      }
      if (TechTracker.hasJetFighter(id))
      {
	name.append("_jp");
      }
    }

    if (type.getName().equals(Constants.BOMBER_TYPE))
    {
      if (TechTracker.hasLongRangeAir(id))
      {
	name.append("_lr");
      }

      if (TechTracker.hasHeavyBomber(id))
      {
	name.append("_hb");
      }
    }

    if (type.getName().equals(Constants.SUBMARINE_TYPE))
    {
      if (TechTracker.hasSuperSubs(id))
      {
	name.append("_ss");
      }
      if (TechTracker.hasRocket(id))
      {}
    }

    if (type.getName().equals(Constants.FACTORY_TYPE))
    {

      if (TechTracker.hasIndustrialTechnology(id))
      {
	name.append("_it");
      }
    }

    if(damaged)
      name.append("_hit");

    return name.toString();
  }


}
