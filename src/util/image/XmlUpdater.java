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
	/**
	 * Utility for updating old game.xml files to the newer format.
	 */
	public static void main(final String[] args) throws Exception
	{
		final File gameXmlFile = new FileOpen("Select xml file", ".xml").getFile();
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
}
