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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XmlUpdater
{
	private static File s_mapFolderLocation = null;
	private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
	
	/**
	 * Utility for updating old game.xml files to the newer format.
	 */
	public static void main(final String[] args) throws Exception
	{
		handleCommandLineArgs(args);
		final File gameXmlFile = new FileOpen("Select xml file", s_mapFolderLocation, ".xml").getFile();
		if (gameXmlFile == null)
		{
			System.out.println("No file selected");
			return;
		}
		// File gameXmlFile = new File("/Users/sgb/Documents/workspace/triplea/maps/revised/games/revised.xml");
		final InputStream source = XmlUpdater.class.getResourceAsStream("gameupdate.xslt");
		if (source == null)
		{
			throw new IllegalStateException("Could not find xslt file");
		}
		final Transformer trans = TransformerFactory.newInstance().newTransformer(new StreamSource(source));
		final InputStream gameXmlStream = new BufferedInputStream(new FileInputStream(gameXmlFile));
		ByteArrayOutputStream resultBuf;
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(true);
			// use a dummy game.dtd, this prevents the xml parser from adding
			// default values
			final URL url = XmlUpdater.class.getResource("");
			final String system = url.toExternalForm();
			final Source xmlSource = new StreamSource(gameXmlStream, system);
			resultBuf = new ByteArrayOutputStream();
			trans.transform(xmlSource, new StreamResult(resultBuf));
		} finally
		{
			gameXmlStream.close();
		}
		gameXmlFile.renameTo(new File(gameXmlFile.getAbsolutePath() + ".backup"));
		new FileOutputStream(gameXmlFile).write(resultBuf.toByteArray());
		System.out.println("Successfully updated:" + gameXmlFile);
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
