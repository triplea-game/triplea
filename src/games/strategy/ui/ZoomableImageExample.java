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
 * ZoomableImageExample.java
 *
 * Created on October 30, 2001, 2:43 PM
 */

package games.strategy.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Start with one arg that is the filename
 * press z to set the zoom.
 */
public class ZoomableImageExample extends JFrame
{
	
	ZoomableImage panel;

	public static void main(String[] args)
	{
		if(args.length == 0)
		{
			System.out.println("Expecting first and only arg to be the name of the image file");
		}
		JFrame frame = new ZoomableImageExample(args[0]);
		frame.show();
	}
	
	/** Creates new ZoomableImageExample */
    public ZoomableImageExample(String imageName) 
	{
		Image image = Toolkit.getDefaultToolkit().getImage(imageName);
		panel = new ZoomableImage(image);
		this.setSize(400,400);
		this.addWindowListener(EXIT_ON_CLOSE_WINDOW_LISTENER);
		this.getContentPane().add( new JScrollPane(panel));
		this.addKeyListener(KEY_LISTENER);
    }
	
	
	final KeyListener KEY_LISTENER = new KeyAdapter()
	{
		public void keyPressed(KeyEvent e) 
		{
			char key = e.getKeyChar();
			switch (key)
			{
				case 'z' : ZOOM_COMMAND.actionPerformed(null);
				break;
			}
		}
	};
	
	private final Action ZOOM_COMMAND = new AbstractAction("save")
	{
		public void actionPerformed(ActionEvent e)
		{
			String input = JOptionPane.showInputDialog("Get Zoom Factor");
			panel.setZoom( Integer.parseInt(input) );
			
		}
	};

	
	
	public static final WindowListener EXIT_ON_CLOSE_WINDOW_LISTENER = new WindowAdapter()
	{
		public void windowClosing(WindowEvent e) 
		{
			System.exit(0);
		}
	};
}
