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
  @author George El-Haddad
  @email  nekromancer@usrs.sourceforge.net
 */

package util.image;

import java.io.*;
import javax.swing.*;

public class FileOpen 
{

	// Error messages
	
	//private final String ERR_MSG_1 = "The file could not be found!";
	//private final String ERR_MSG_2 = "I/O Error occured with this file!\nTry loading it again.";
	private final String ERR_MSG_3 = "Warning! Could not load the file!";
	//private final String ERR_MSG_4 = "Warning! This is an empty file or the first line is NULL.";
	
	private File file = null;

	/**
	   Default Constructor.
	   
	   @param java.lang.String title  the title of the JFileChooser
	   
	   @exception java.lang.Exception  ex
	   
	   Creates a file selection dialog starting at the current
	   working directory. Filters out all non-txt files and
	   handles possible file load errors.
	*/
	public FileOpen(String title) 
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(title);
		chooser.setCurrentDirectory(new File(System.getProperties().getProperty("user.dir")));

		/*
		 * Show only text and gif files
		 */
		chooser.setFileFilter(new javax.swing.filechooser.FileFilter()
		{
			public boolean accept(File f)
			{
				return f.getName().toLowerCase().endsWith(".txt") || 
				       f.getName().toLowerCase().endsWith(".gif") ||
				       f.getName().toLowerCase().endsWith(".png") ||
				       f.isDirectory();
			}

			public String getDescription()
			{
				return "*.txt, *.gif, *.png";
			}
		});

		int result = chooser.showOpenDialog(null);

		if(result == JFileChooser.CANCEL_OPTION)
		{
			return;
		}

		try
		{
			file = chooser.getSelectedFile();   //get the file

		}
		catch(Exception ex)
		{
			JOptionPane.showMessageDialog(null,ERR_MSG_3,"Warning!", JOptionPane.WARNING_MESSAGE);
			file = null;
		}

	}//constructor
	
	
	/**
	   Returns the newly selected file.
	   Will return null if no file is selected.

	   @return java.io.File
	*/
	public File getFile()
	{
		return file;
	}


	/**
	   Returns the newly selected file.
	   Will return null if no file is selected.

	   @return java.lang.String
	*/
	public String getPathString()
	{
		if(file == null)
		{
			return null;
		}
		else
		{
			return file.getPath();
		}
	}

}//end class FileOpen
