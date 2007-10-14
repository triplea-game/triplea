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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;



/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class Util
{
    //all we have is static methods
    private Util()
    {
    }

    public static void ensureImageLoaded(Image anImage, Component comp) throws InterruptedException
    {
        MediaTracker tracker = new MediaTracker(comp);
        tracker.addImage(anImage, 1);
        tracker.waitForAll();
        tracker.removeImage(anImage);
    }

    public static Image copyImage(BufferedImage img, boolean needAlpha)
    {
        BufferedImage copy = createImage(img.getWidth(), img.getHeight(), needAlpha);
        Graphics2D g = (Graphics2D) copy.getGraphics();
        g.drawImage(img, 0, 0 ,null);
        g.dispose();
        return copy;
    }

//    public static Image createVolatileImage(int width, int height)
//    {
//
//        GraphicsConfiguration localGraphicSystem = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
//                .getDefaultConfiguration();
//        return localGraphicSystem.createCompatibleVolatileImage(width, height);
//
//    }

    /**
     * Previously used to use TYPE_INT_BGR and TYPE_INT_ABGR but caused memory
     * problems. Fix is to use 3Byte rather than INT.
     */
    public static BufferedImage createImage(int width, int height, boolean needAlpha)
    {

        //

                if (needAlpha)
                {
                    return new BufferedImage(width, height,
         BufferedImage.TYPE_4BYTE_ABGR);
                } else
                {
                    return new BufferedImage(width, height,
         BufferedImage.TYPE_3BYTE_BGR);
                }

        //the code below should be the correct way to get graphics, but it is
        // makes the ui quite
        //unresponsive when drawing the map (as seen when updating the map for
        // different routes
        //in combat move phase)
        //For jdk1.3 on linux and windows, and jdk1.4 on linux there is a very
        //noticeable difference
        //jdk1.4 on windows doesnt have a difference

        // local graphic system is used to create compatible bitmaps
//        GraphicsConfiguration localGraphicSystem = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
//                .getDefaultConfiguration();
//
//        // Create a buffered image in the most optimal format, which allows a
//        // fast blit to the screen.
//        BufferedImage workImage = localGraphicSystem.createCompatibleImage(width, height, needAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
//
//        return workImage;

    }

    public static Dimension getDimension(Image anImage, ImageObserver obs)
    {
        return new Dimension(anImage.getWidth(obs), anImage.getHeight(obs));
    }

    public static void center(Window w)
    {
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

        int windowWidth = w.getWidth();
        int windowHeight = w.getHeight();

        if (windowHeight > screenHeight)
            return;
        if (windowWidth > screenWidth)
            return;

        int x = (screenWidth - windowWidth) / 2;
        int y = (screenHeight - windowHeight) / 2;

        w.setLocation(x, y);

    }

    
    //code stolen from swingx
    //swingx is lgpl, so no problems with copyright
    public static Image getBanner(String text) 
    {
        int w = 400;
        int h = 60;
        float loginStringX = w * .05f;
        float loginStringY = h * .75f;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        Font font = new Font("Arial Bold", Font.PLAIN, 36);
        g2.setFont(font);
        Graphics2D originalGraphics = g2;
        

        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        //draw a big square
        g2.setColor(Color.GRAY);
        g2.fillRect(0, 0, w, h);

        //create the curve shape
        GeneralPath curveShape = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        curveShape.moveTo(0, h * .6f);
        curveShape.curveTo(w * .167f, h * 1.2f, w * .667f, h * -.5f, w, h * .75f);
        curveShape.lineTo(w, h);
        curveShape.lineTo(0, h);
        curveShape.lineTo(0, h * .8f);
        curveShape.closePath();

        //draw into the buffer a gradient (bottom to top), and the text "Login"
        GradientPaint gp = new GradientPaint(0, h, Color.GRAY,
                0, 0, Color.LIGHT_GRAY);
        g2.setPaint(gp);
        g2.fill(curveShape);

        //g2.setPaint(Color.white);
        originalGraphics.setColor(Color.WHITE);
        originalGraphics.drawString(text, loginStringX, loginStringY);
        return img;
    }
}