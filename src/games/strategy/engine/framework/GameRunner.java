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
 * GameRunner.java
 *
 * Created on December 14, 2001, 12:05 PM
 */

package games.strategy.engine.framework;

import java.util.*;
import java.net.*;
import java.io.*;
import org.xml.sax.SAXException;
import java.awt.*;
import javax.swing.*;

import games.strategy.util.Util;
import games.strategy.net.*;
import games.strategy.ui.*;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.gamePlayer.GamePlayer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ui.*;

import games.strategy.engine.chat.*;
import games.strategy.engine.transcript.*;

import games.strategy.debug.Console;

/**
 *
 * @author  Sean Bridges
 *
 * This class starts and runs the game.
 */
public class GameRunner
{
  private GameData m_data;
  public final static int PORT = 3300;

  public static Image getGameIcon(JFrame frame)
  {
    Image img = null;
    try
    {
      img = frame.getToolkit().getImage(GameRunner.class.getResource("ta_icon.png"));
    }
    catch (Exception ex)
    {
      System.out.println("icon not loaded");
    }
    MediaTracker tracker = new MediaTracker(frame);
    tracker.addImage(img, 0);
    try
    {
      tracker.waitForAll();
    }
    catch (InterruptedException ex)
    {
      ex.printStackTrace();
    }
    return img;
  }

  private static void installSecurityProvider()
  {
//    java.security.Security.addProvider( new com.sun.crypto.provider.SunJCE());
  }

  public static void main(String[] args)
  {
    try
    {
      UIManager.setLookAndFeel(new com.incors.plaf.kunststoff.KunststoffLookAndFeel());
    }
    catch (UnsupportedLookAndFeelException ex)
    {
      ex.printStackTrace();
    }

    installSecurityProvider();

    //Console c = Console.getConsole();
    //c.displayStandardError();
    //c.displayStandardOutput();
    //c.show();

    LauncherFrame frame = new LauncherFrame();
    frame.setIconImage(getGameIcon(frame));
    frame.pack();
    frame.show();
  }

  /**
   * Get the root folder for the application
   */
  public static File getRootFolder()
  {
    //TODO this is a bit hokey, we assume that we are running
    //from the bin directory.
      return new File("..");
  }


}

class ClientReady implements Serializable
{

}

