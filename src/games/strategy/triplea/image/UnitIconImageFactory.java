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

  TechTracker m_techTracker = new TechTracker();
  private TechTracker getTechTracker(GameData data)
  {
    return m_techTracker;
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

  /** Creates new IconImageFactory */
  private UnitIconImageFactory()
  {

  }

  public Image getImage(UnitType type, PlayerID player, GameData data, boolean damaged)
  {
      String baseName = getBaseImageName(type, player, data, damaged);
      String fullName = baseName + player.getName();
      if(m_images.containsKey(fullName))
      {
          return (Image) m_images.get(fullName);
      }

      Image baseImage = getBaseImage(baseName, player, damaged);

      m_images.put(fullName, baseImage);
      return baseImage;


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

  public ImageIcon getIcon(UnitType type, PlayerID player, GameData data, boolean damaged)
  {
      String baseName = getBaseImageName(type, player, data, damaged);
      String fullName = baseName + player.getName();
      if(m_icons.containsKey(fullName))
      {
          return (ImageIcon) m_icons.get(fullName);
      }

      Image img = getImage(type, player, data, damaged);
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
          if (getTechTracker(data).hasLongRangeAir(id))
          {
              name.append("_lr");
          }
          if (getTechTracker(data).hasJetFighter(id))
          {
              name.append("_jp");
          }
      }

      if (type.getName().equals(Constants.BOMBER_TYPE))
      {
          if (getTechTracker(data).hasLongRangeAir(id))
          {
              name.append("_lr");
          }

          if (getTechTracker(data).hasHeavyBomber(id))
          {
              name.append("_hb");
          }
      }

      if (type.getName().equals(Constants.SUBMARINE_TYPE))
      {
          if (getTechTracker(data).hasSuperSubs(id))
          {
              name.append("_ss");
          }
          if (getTechTracker(data).hasRocket(id))
          {}
      }

      if (type.getName().equals(Constants.FACTORY_TYPE))
      {

          if (getTechTracker(data).hasIndustrialTechnology(id))
          {
              name.append("_it");
          }
      }

      if(damaged)
          name.append("_hit");

      return name.toString();
  }


}
