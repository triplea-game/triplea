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

package games.strategy.engine.framework;

/**
 * <p>Title: TripleA</p>
 * <p> </p>
 * <p> Copyright (c) 2002</p>
 * <p> </p>
 * @author Sean Bridges
 *
 */


import java.util.*;
import java.net.*;
import java.io.*;
import org.xml.sax.SAXException;
import java.awt.*;
import javax.swing.*;

import games.strategy.util.Util;
import games.strategy.net.*;
import games.strategy.ui.*;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.gamePlayer.GamePlayer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ui.*;

import games.strategy.engine.chat.*;


import games.strategy.debug.Console;

public class SavedGamedDataLoader implements IGameDataLoader
{

	public GameData loadData()
	{

		SaveGameFileChooser fileChooser = SaveGameFileChooser.getInstance();

		int rVal = fileChooser.showOpenDialog(null);
		if(rVal == JFileChooser.APPROVE_OPTION)
		{
			File f = fileChooser.getSelectedFile();
			try
			{
				return new GameDataManager().loadGame(f);
			} catch(Exception e)
			{
				e.printStackTrace();
				System.exit(0);
				return null;
			}
		}
		else
		{
			System.exit(0);
			return null;
		}
	}

}
