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


import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ui.SaveGameFileChooser;

import java.io.File;

import javax.swing.JFileChooser;

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
