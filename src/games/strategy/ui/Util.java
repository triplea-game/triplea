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
 * Util.java
 *
 * Created on October 30, 2001, 6:29 PM
 */

package games.strategy.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.JComponent;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class Util
{
	//all we have is static methods
	private Util() {}



	public static void ensureImageLoaded(Image anImage, Component comp) throws InterruptedException
	{
		MediaTracker tracker = new MediaTracker(comp);
		tracker.addImage(anImage, 1);
		tracker.waitForAll();
	}

	public static Image copyImage(Image img, JComponent comp)
	{
		Image copy = createImage(img.getWidth(comp), img.getHeight(comp), false);
		copy.getGraphics().drawImage(img, 0,0, comp);
		return copy;
	}

        
	/**
	   Change by Beagle.
	   
	   Previously used to use TYPE_INT_BGR and TYPE_INT_ABGR but caused memory
	   problems. Fix is to use 3Byte rather than INT.
	*/
	public static BufferedImage createImage(int width, int height, boolean needAlpha)
	{
            if(needAlpha)
	    {
                return new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            }
	    else
	    {
                return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
	    }
	    
    //the code below should be the correct way to get graphics, but it is makes the ui quite
    //unresponsive when drawing the map (as seen when updating the map for different routes
    //in combat move phase)
    //For jdk1.3 on linux and windows, and jdk1.4 on linux there is a very
    //noticeable difference
    //jdk1.4 on windows doesnt have a difference

//      // local graphic system is used to create compatible bitmaps
//      GraphicsConfiguration localGraphicSystem = GraphicsEnvironment.getLocalGraphicsEnvironment()
//          .getDefaultScreenDevice()
//          .getDefaultConfiguration();
//
//      // Create a buffered image in the most optimal format, which allows a
//      //    fast blit to the screen.
//      BufferedImage workImage = localGraphicSystem.createCompatibleImage(width,
//          height,
//          Transparency.BITMASK);
//
//      return workImage;

	}

	public static Dimension getDimension(Image anImage, ImageObserver obs)
	{
		return new Dimension(anImage.getWidth(obs), anImage.getHeight(obs) );
	}

	public static final WindowListener EXIT_ON_CLOSE_WINDOW_LISTENER = new WindowAdapter()
	{
		public void windowClosing(WindowEvent e)
		{
			System.exit(0);
		}
	};

  public static void center(Window w)
  {
    int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
    int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

    int windowWidth = w.getWidth();
    int windowHeight = w.getHeight();

    if(windowHeight > screenHeight)
      return;
    if(windowWidth > screenWidth)
      return;

    int x = (screenWidth - windowWidth ) / 2;
    int y = (screenHeight - windowHeight) /2;

    w.setLocation(x,y);

  }

}
