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

package games.strategy.engine.framework.message;


import java.io.*;

import java.util.zip.*;

/**
 * <p>Title: TripleA</p>
 * <p> </p>
 * <p> Copyright (c) 2002</p>
 * <p> </p>
 * @author Sean Bridges
 *
 * Used to transmit the Game data over the network.  Zips the data
 * to reduce network traffic.
 *
 */


public class GameDataResponse implements java.io.Serializable
{

	private static final ZipEntry s_zipEntry = new ZipEntry("Default");

	private int m_originalLength;
	private byte[] m_gameData;

    public GameDataResponse(byte[] gameData)
    {
		try
		{
			m_originalLength = gameData.length;
			ByteArrayOutputStream sink = new ByteArrayOutputStream(gameData.length / 3);;
			ZipOutputStream zip = new ZipOutputStream(sink);
			zip.setLevel(9);
			zip.putNextEntry(s_zipEntry);
			zip.write(gameData);
			zip.closeEntry();
			zip.flush();


			m_gameData = sink.toByteArray();
		}
		catch(IOException ioe)
		{
			throw new RuntimeException(ioe.getMessage());
		}

    }

	public InputStream getGameData()
	{
		ZipInputStream data = new ZipInputStream(new ByteArrayInputStream(m_gameData));
		try
		{
		    data.getNextEntry();
		}
		catch(IOException ioe)
		{
			throw new RuntimeException(ioe.getMessage());
		}
		return data;
	}

	public int getDataSize()
	{
		return m_originalLength;
	}

	public int getTransmittedSize()
	{
		return m_gameData.length;
	}
}