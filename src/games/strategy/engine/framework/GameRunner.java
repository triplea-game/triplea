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

import games.strategy.debug.Console;
import games.strategy.engine.framework.ui.LauncherFrame;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.net.URL;
import java.util.logging.LogManager;

import javax.swing.*;

/**
 * 
 * @author Sean Bridges
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
        } catch (Exception ex)
        {
            System.out.println("icon not loaded");
        }
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(img, 0);
        try
        {
            tracker.waitForAll();
        } catch (InterruptedException ex)
        {
            ex.printStackTrace();
        }
        return img;
    }

    private static boolean isMac()
    {
        return System.getProperties().getProperty("os.name").toLowerCase().indexOf("mac") != -1;
    }

    /**
     * Get version number of Java VM. Allow versions of 1.4 to run the game.
     * Show warning for 1.5 version due to instability with serialized classes
     * but don't prevent from game play, as it seems to affect cross version
     * play only.
     * 
     * Show error + URL and kill program.
     * 
     * @author NeKromancer
     */
    private static void checkJavaVersion()
    {
        String strV = System.getProperties().getProperty("java.version");
        boolean v14 = strV.indexOf("1.4") != -1;
        boolean v15 = strV.indexOf("1.5") != -1;

        if (v14 && v15)
        {
            if (!isMac())
            {
                JOptionPane.showMessageDialog(null, "You need java version 1.4.x\nPlease download a newer version of java from http://java.sun.com/",
                        "ERROR", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            } else if (isMac())
            {
                JOptionPane.showMessageDialog(null,
                        "You need java version 1.4.x\nPlease download a newer version of java from http://www.apple.com/java/", "ERROR",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        }

    }//end checkJavaVersion()

    public static void main(String[] args)
    {
        checkJavaVersion();

        setupLogging();
        
        Console.getConsole().displayStandardError();
        Console.getConsole().displayStandardOutput();

        try
        {
            //macs are already beautiful
            if (!isMac())
            {
                com.jgoodies.looks.plastic.PlasticLookAndFeel.setTabStyle(com.jgoodies.looks.plastic.PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
                //com.jgoodies.plaf.plastic.PlasticXPLookAndFeel.setTabStyle(com.jgoodies.plaf.plastic.PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
                UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.PlasticXPLookAndFeel());
                com.jgoodies.looks.Options.setPopupDropShadowEnabled(true);

            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        LauncherFrame frame = new LauncherFrame();
        frame.setIconImage(getGameIcon(frame));
        frame.pack();

        games.strategy.ui.Util.center(frame);
        frame.setVisible(true);

    }

    private static void setupLogging()
    {
        //setup logging to read our logging.properties
        try
        {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Get the root folder for the application
     */
    public static File getRootFolder()
    {
        //this will fail if we are in a jar
        //what we are doing is looking up the url to GameRunner.class
        //we can find it because we are it
        //we know that the class file is in a directory one above the games root folder
        //so navigate up from the class file, and we have root.
        
        //find the url of our class
        URL url = GameRunner.class.getResource("GameRunner.class");
        
        //we want to move up 1 directory for each  
        //package
        int moveUpCount = GameRunner.class.getName().split("\\.").length + 1;
        
        String fileName = url.getFile();
        try
        {
            //deal with spaces in the file name which would be url encoded
            fileName  = URLDecoder.decode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        File f = new File(fileName);
        
        
        for(int i = 0; i < moveUpCount; i++)
        {
            f = f.getParentFile();
        }
        
        if(!f.exists())
        {
            System.err.println("Could not find root folder, does  not exist:" + f);
            return new File(System.getProperties().getProperty("user.dir"));
        }
        
        return f;
    }

}

class ClientReady implements Serializable
{

}
