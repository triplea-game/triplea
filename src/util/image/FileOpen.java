package util.image;

/*
  @author George El-Haddad
  @email  nekromancer@usrs.sourceforge.net
 */

import java.io.*;
import javax.swing.*;

public class FileOpen 
{

	// Error messages
	
	private final String ERR_MSG_1 = "The file could not be found!";
	private final String ERR_MSG_2 = "I/O Error occured with this file!\nTry loading it again.";
	private final String ERR_MSG_3 = "Warning! Could not load the file!";
	private final String ERR_MSG_4 = "Warning! This is an empty file or the first line is NULL.";
	
	private File file = null;

	/**
	   Default Constructor.
	   
	   Creates a file selection dialog starting at the current
	   working directory. Filters out all non-txt files and
	   handles possible file load errors.

	   @exception java.lang.Exception  ex
	*/
	public FileOpen() 
	{
		JFileChooser chooser = new JFileChooser();
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
				       f.isDirectory();
			}

			public String getDescription()
			{
				return "*.txt, *.gif";
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
