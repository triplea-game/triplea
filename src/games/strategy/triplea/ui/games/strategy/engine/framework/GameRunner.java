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

import java.awt.*;
import java.io.*;
import java.net.*;

import javax.swing.JOptionPane;

/**
 * 
 * @author Sean Bridges
 * 
 * This class starts and runs the game.<p>
 * 
 * This class is compiled to run under older jdks (1.3 at least), and should not 
 * do anything more than check the java version number, and then delegate to GameRunner2<p>
 */
public class GameRunner
{
    public final static int PORT = 3300;

    public static Image getGameIcon(Window frame)
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

    public static boolean isWindows()
    {
        return System.getProperties().getProperty("os.name").toLowerCase().indexOf("windows") != -1;
    }    
    
    public static boolean isMac()
    {
        return System.getProperties().getProperty("os.name").toLowerCase().indexOf("mac") != -1;
    }

    /**
     * Get version number of Java VM.
     * 
     * @author NeKromancer
     */
    private static void checkJavaVersion()
    {
        //note - this method should not use any new language features (this includes string concatention using +
        //since this method must run on older vms.
        
        String version = System.getProperties().getProperty("java.version");
        boolean v12 = version.indexOf("1.2") != -1;
        boolean v13 = version.indexOf("1.3") != -1;
        boolean v14 = version.indexOf("1.4") != -1;

        if (v14 || v13 || v12)
        {
            if (!isMac())
            {
                JOptionPane.showMessageDialog(null, "TripleA requires a java runtime greater than or equal to 5.0.\nPlease download a newer version of java from http://java.sun.com/",
                        "ERROR", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            } else if (isMac())
            {
                JOptionPane.showMessageDialog(null,
                        "TripleA requires a java runtime greater than or equal to 5.0 (Note, this requires Mac OS X >= 10.4)\nPlease download a newer version of java from http://www.apple.com/java/", "ERROR",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        }

    }//end checkJavaVersion()

    public static void main(String[] args)
    {
        //we want this class to be executable in older jvm's
        //since we require jdk 1.5, this class delegates to GameRunner2
        //and all we do is check the java version
        checkJavaVersion();

        //do the other interesting stuff here
        GameRunner2.main(args);
    }

 

    /**
     * Get the root folder for the application
     */
    public static File getRootFolder()
    {
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

	//we are in a jar file
	if(fileName.indexOf("triplea.jar!") != -1)
	{
		String subString = fileName.substring("file:/".length()  - ( isWindows() ? 0 : 1)  , fileName.indexOf("triplea.jar!") -1);
        
         
		File f = new File(subString).getParentFile();
		
		if(!f.exists())
		{
			throw new IllegalStateException("File not found:" + f);
		}
		return f;
	}
	else
	{
				
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
}
