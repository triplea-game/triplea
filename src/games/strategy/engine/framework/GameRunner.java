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

import java.io.File;
import java.io.Serializable;

import java.awt.Image;
import java.awt.MediaTracker;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import games.strategy.debug.Console;
import games.strategy.engine.framework.ui.LauncherFrame;

/**
 *
 * @author  Sean Bridges
 *
 * This class starts and runs the game.
 */
public class GameRunner
{
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

  private static boolean isMac()
  {
    return   System.getProperties().getProperty("os.name").toLowerCase().indexOf("mac") != -1;
  }


  /**
     Get version number of Java VM.
     Allow versions of 1.4 to run the game.
     Show warning for 1.5 version due to instability with
     serialized classes but don't prevent from game play,
     as it seems to affect cross version play only.
     
     Show error + URL and kill program.
     @author NeKromancer
  */
  private static void checkJavaVersion()
  {
     String strV = System.getProperties().getProperty("java.version");
     int v14 = strV.indexOf("1.4");
     int v15 = strV.indexOf("1.5");

     System.out.println("v14 = "+v14);
     System.out.println("v15 = "+v15);
     System.out.println("strV = "+strV);

     if(v14 == -1 && v15 == -1) {
          if(!isMac()) {
               JOptionPane.showMessageDialog(null, "You need java version 1.4.x\nPlease download a newer version of java from http://java.sun.com/", "ERROR", JOptionPane.ERROR_MESSAGE);
               System.exit( -1);
          }
          else if(isMac()) {
               JOptionPane.showMessageDialog(null, "You need java version 1.4.x\nPlease download a newer version of java from http://www.apple.com/java/", "ERROR", JOptionPane.ERROR_MESSAGE);
               System.exit( -1);
	   }
      }
      else if(v15 != -1) {
               JOptionPane.showMessageDialog(null, "Running TripleA with Java v1.5.x will cause instability\n with serialized classes (bug #1027674)\nYou will get errors when playing other who use a differnt version!!\nWe highly recomend using Java v1.4.x", "WARNING", JOptionPane.WARNING_MESSAGE);
      }

  }//end checkJavaVersion()

  public static void main(String[] args)
  {

    checkJavaVersion();
    
    Console.getConsole().displayStandardError();
    Console.getConsole().displayStandardOutput();
 
    try
    {
      //macs are already beautiful
        if(!isMac())
        {
          com.jgoodies.plaf.plastic.Plastic3DLookAndFeel.setTabStyle(com.jgoodies.plaf.plastic.PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
          //com.jgoodies.plaf.plastic.PlasticXPLookAndFeel.setTabStyle(com.jgoodies.plaf.plastic.PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
          UIManager.setLookAndFeel(new com.jgoodies.plaf.plastic.PlasticXPLookAndFeel());
        }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }


    LauncherFrame frame = new LauncherFrame();
    frame.setIconImage(getGameIcon(frame));
    frame.pack();

    games.strategy.ui.Util.center(frame);
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

