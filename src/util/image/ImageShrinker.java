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
package util.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.sun.image.codec.jpeg.*;

/**
 * 
 * Takes an image and shrinks it.  Used for making small images.
 *
 * @author Sean Bridges
 */
public class ImageShrinker
{
    
    
    public static void main(String[] args) throws Exception
    {
        File mapFile = new FileOpen("Select The Large Image").getFile();
        
        if(!mapFile.exists())
            throw new IllegalStateException(mapFile + "File does not exist");
        
        String input = JOptionPane.showInputDialog(null, "Select scale");
        float scale = Float.parseFloat(input);
        
        Image baseImg =  ImageIO.read(mapFile);
        int thumbWidth = (int) (baseImg.getWidth(null) * scale);
        int thumbHeight = (int) (baseImg.getHeight(null) * scale);
     
        //based on code from 
        //http://www.geocities.com/marcoschmidt.geo/java-save-jpeg-thumbnail.html
            
        // draw original image to thumbnail image object and
        // scale it to the new size on-the-fly
        BufferedImage thumbImage = new BufferedImage(thumbWidth, 
          thumbHeight, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D graphics2D = thumbImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(baseImg, 0, 0, thumbWidth, thumbHeight, null);
        // save thumbnail image to OUTFILE
        BufferedOutputStream out = new BufferedOutputStream(new
          FileOutputStream("smallMap.jpeg"));
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam param = encoder.
          getDefaultJPEGEncodeParam(thumbImage);
        
        param.setQuality(1, false);
        encoder.setJPEGEncodeParam(param);
        encoder.encode(thumbImage);
        out.close(); 
        System.out.println("Done.");
        System.exit(0);

        
        
    }
     
    
    
}
