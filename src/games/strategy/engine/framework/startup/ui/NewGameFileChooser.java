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

package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.GameRunner;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * <p>Title: TripleA</p>
 * <p> </p>
 * <p> Copyright (c) 2002</p>
 * <p> </p>
 * @author Sean Bridges
 *
 */

public class NewGameFileChooser extends JFileChooser
{

	public static final File DEFAULT_DIRECTORY = new File(GameRunner.getRootFolder(),  "/games");

	private static NewGameFileChooser s_instance;

	public static NewGameFileChooser getInstance()
	{
		if(s_instance == null)
			s_instance = new NewGameFileChooser();
		return s_instance;
	}

    public NewGameFileChooser()
    {
	    super();
		setFileFilter(m_gameDataFileFilter);
		setCurrentDirectory(DEFAULT_DIRECTORY);
    }


	FileFilter m_gameDataFileFilter = new FileFilter()
	{
		public  boolean accept(File f)
		{
			if (f.isDirectory())
				return true;

			return f.getName().endsWith(".xml") || f.getName().endsWith(".txml");
		}

		public String getDescription()
		{
		    return "Game Files, *.xml";
		}
	};
}

