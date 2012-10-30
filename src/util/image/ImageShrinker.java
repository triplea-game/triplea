/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package util.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * 
 * Takes an image and shrinks it. Used for making small images.
 * 
 * @author Sean Bridges
 */
public class ImageShrinker
{
	private static File s_mapFolderLocation = null;
	private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
	
	public static void main(final String[] args) throws Exception
	{
		handleCommandLineArgs(args);
		JOptionPane.showMessageDialog(null, new JLabel("<html>"
					+ "This is the ImageShrinker, it will create a smallMap.jpeg file for you. "
					+ "<br>Put in your base map or relief map, and it will spit out a small scaled copy of it."
					+ "<br>Please note that the quality of the image will be worse than if you use a real painting program."
					+ "<br>So we suggest you instead shrink the image with paint.net or photoshop or gimp, etc, then clean it up before saving."
					+ "</html>"));
		final File mapFile = new FileOpen("Select The Large Image", s_mapFolderLocation, ".gif", ".png").getFile();
		if (!mapFile.exists())
			throw new IllegalStateException(mapFile + "File does not exist");
		final String input = JOptionPane.showInputDialog(null, "Select scale");
		final float scale = Float.parseFloat(input);
		final Image baseImg = ImageIO.read(mapFile);
		final int thumbWidth = (int) (baseImg.getWidth(null) * scale);
		final int thumbHeight = (int) (baseImg.getHeight(null) * scale);
		// based on code from
		// http://www.geocities.com/marcoschmidt.geo/java-save-jpeg-thumbnail.html
		// draw original image to thumbnail image object and
		// scale it to the new size on-the-fly
		final BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics2D = thumbImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(baseImg, 0, 0, thumbWidth, thumbHeight, null);
		// save thumbnail image to OUTFILE
		final File file = new File(new File(mapFile.getPath()).getParent() + File.separatorChar + "smallMap.jpeg");
		final FileImageOutputStream out = new FileImageOutputStream(file);
		final ImageWriter encoder = ImageIO.getImageWritersByFormatName("JPEG").next();
		final JPEGImageWriteParam param = new JPEGImageWriteParam(null);
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality((float) 1.0);
		encoder.setOutput(out);
		encoder.write((IIOMetadata) null, new IIOImage(thumbImage, null, null), param);
		out.close();
		System.out.println("Image successfully written to " + file.getPath());
		System.exit(0);
	}
	
	private static String getValue(final String arg)
	{
		final int index = arg.indexOf('=');
		if (index == -1)
			return "";
		return arg.substring(index + 1);
	}
	
	private static void handleCommandLineArgs(final String[] args)
	{
		// arg can only be the map folder location.
		if (args.length == 1)
		{
			String value;
			if (args[0].startsWith(TRIPLEA_MAP_FOLDER))
			{
				value = getValue(args[0]);
			}
			else
			{
				value = args[0];
			}
			final File mapFolder = new File(value);
			if (mapFolder.exists())
				s_mapFolderLocation = mapFolder;
			else
				System.out.println("Could not find directory: " + value);
		}
		else if (args.length > 1)
		{
			System.out.println("Only argument allowed is the map directory.");
		}
		// might be set by -D
		if (s_mapFolderLocation == null || s_mapFolderLocation.length() < 1)
		{
			String value = System.getProperty(TRIPLEA_MAP_FOLDER);
			if (value != null && value.length() > 0)
			{
				value = value.replaceAll("\\(", " ");
				final File mapFolder = new File(value);
				if (mapFolder.exists())
					s_mapFolderLocation = mapFolder;
				else
					System.out.println("Could not find directory: " + value);
			}
		}
	}
}
