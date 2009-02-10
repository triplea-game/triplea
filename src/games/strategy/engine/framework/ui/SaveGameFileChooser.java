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

package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.GameRunner;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * @author Sean Bridges
 *
 */

public class SaveGameFileChooser extends JFileChooser
{

	
	public static final String AUTOSAVE_FILE_NAME = "autosave.tsvg";

	public static final File DEFAULT_DIRECTORY;
	
	/*
	 * The default is to store saved games in the save game folder, but a request was made to allow
	 * them in the users home.
	 * Check the value in triplea.properties to see where we want to save the saved games.
	 * The change was suggested by William McQueen.
	 */
	static
	{
	    //the default
	    File defaultDirectory;
        if(GameRunner.isMac())
            defaultDirectory = new File(System.getProperties().getProperty("user.home") +"/Documents/triplea/savedGames/");
        else
            defaultDirectory = new File(System.getProperties().getProperty("user.home") +"/triplea/savedGames/");
        
	    DEFAULT_DIRECTORY = defaultDirectory;
	}
	
	
	private static SaveGameFileChooser s_instance;

	public static SaveGameFileChooser getInstance()
	{
		if(s_instance == null)
			s_instance = new SaveGameFileChooser();
		return s_instance;
	}

    public SaveGameFileChooser()
    {
	    super();
		setFileFilter(m_gameDataFileFilter);
		ensureDefaultDirExists();
		setCurrentDirectory(DEFAULT_DIRECTORY);
    }

	public static void ensureDefaultDirExists()
	{
		ensureDirectoryExists(DEFAULT_DIRECTORY);
	}
	
	private static void ensureDirectoryExists(File f)
	{
	    
	    if(!f.getParentFile().exists())
	        ensureDirectoryExists(f.getParentFile());
	   
	    if(!f.exists())
	    {
	        f.mkdir();
	    }
	}


	FileFilter m_gameDataFileFilter = new FileFilter()
	{
		public  boolean accept(File f)
		{
			if (f.isDirectory())
				return true;

            //the extension should be .tsvg, but find svg extensions as well
			//also, macs download the file as tsvg.gz, so accept that as well
			return f.getName().endsWith(".tsvg") || f.getName().endsWith(".svg") || f.getName().endsWith("tsvg.gz");
		}

		public String getDescription()
		{
		    return "Saved Games, *.tsvg";
		}
	};
}

