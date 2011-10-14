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

import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class CenterPicker extends JFrame
{
	
	private Image m_image; // The map image will be stored here
	private Map<String, Point> m_centers = new HashMap<String, Point>(); // hash map for center points
	private Map<String, List<Polygon>> m_polygons = new HashMap<String, List<Polygon>>(); // hash map for polygon points
	private JLabel m_location = new JLabel();
	
	/**
	 * main(java.lang.String[])
	 * 
	 * Main program begins here.
	 * Asks the user to select the map then runs the
	 * the actual picker.
	 * 
	 * @param java
	 *            .lang.String[] args the command line arguments
	 * @see Picker(java.lang.String) picker
	 */
	public static void main(String[] args)
	{
		System.out.println("Select the map");
		String mapName = new FileOpen("Select The Map").getPathString();
		
		if (mapName != null)
		{
			System.out.println("Map : " + mapName);
			
			CenterPicker picker = new CenterPicker(mapName);
			picker.setSize(600, 550);
			picker.setVisible(true);
		}
		else
		{
			System.out.println("No Image Map Selected. Shutting down.");
			System.exit(0);
		}
		
	}// end main
	
	/**
	 * Constructor CenterPicker(java.lang.String)
	 * 
	 * Setus up all GUI components, initializes variables with
	 * default or needed values, and prepares the map for user
	 * commands.
	 * 
	 * @param java
	 *            .lang.String mapName name of map file
	 */
	public CenterPicker(String mapName)
	{
		super("Center Picker");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		File file = new File(new File(mapName).getParent() + File.pathSeparator + "polygons.txt");
		if (file.exists()
					&& JOptionPane.showConfirmDialog(new JPanel(), "A polygons.txt file was found in the map's folder, do you want to use the file to supply the territories names?",
								"File Suggestion", 1) == 0)
		{
			try
			{
				System.out.println("Polygons : " + file.getPath());
				m_polygons = PointFileReaderWriter.readOneToManyPolygons(new FileInputStream(file.getPath()));
			} catch (IOException ex1)
			{
				ex1.printStackTrace();
			}
		}
		else
		{
			try
			{
				System.out.println("Select the Polygons file");
				String polyPath = new FileOpen("Select A Polygon File").getPathString();
				
				if (polyPath != null)
				{
					System.out.println("Polygons : " + polyPath);
					m_polygons = PointFileReaderWriter.readOneToManyPolygons(new FileInputStream(polyPath));
				}
				else
				{
					System.out.println("Polygons file not given. Will run regardless");
				}
			} catch (IOException ex1)
			{
				ex1.printStackTrace();
			}
		}
		
		createImage(mapName);
		
		JPanel imagePanel = createMainPanel();
		
		/*
		Add a mouse listener to show
		X : Y coordinates on the lower
		left corner of the screen.
		*/
		imagePanel.addMouseMotionListener(
					new MouseMotionAdapter()
			{
				@Override
				public void mouseMoved(MouseEvent e)
					{
						m_location.setText("x:" + e.getX() + " y:" + e.getY());
					}
			}
					);
		
		/*
		   Add a mouse listener to monitor
		for right mouse button being
		clicked.	
		*/
		imagePanel.addMouseListener(
					new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					mouseEvent(e.getPoint(), e.isControlDown(), SwingUtilities.isRightMouseButton(e));
				}
			}
					);
		
		// set up the image panel size dimensions ...etc
		
		imagePanel.setMinimumSize(new Dimension(m_image.getWidth(this), m_image.getHeight(this)));
		imagePanel.setPreferredSize(new Dimension(m_image.getWidth(this), m_image.getHeight(this)));
		imagePanel.setMaximumSize(new Dimension(m_image.getWidth(this), m_image.getHeight(this)));
		
		// set up the layout manager
		
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(new JScrollPane(imagePanel), BorderLayout.CENTER);
		this.getContentPane().add(m_location, BorderLayout.SOUTH);
		
		// set up the actions
		
		Action openAction = new AbstractAction("Load Centers")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				loadCenters();
			}
		};
		openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Center Points File");
		
		Action saveAction = new AbstractAction("Save Centers")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				saveCenters();
			}
		};
		saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Center Points To File");
		
		Action exitAction = new AbstractAction("Exit")
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				System.exit(0);
			}
		};
		exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
		
		// set up the menu items
		
		JMenuItem openItem = new JMenuItem(openAction);
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		
		JMenuItem saveItem = new JMenuItem(saveAction);
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		
		JMenuItem exitItem = new JMenuItem(exitAction);
		
		// set up the menu bar
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		
		menuBar.add(fileMenu);
		
	}// end constructor
	
	/**
	 * createImage(java.lang.String)
	 * 
	 * creates the image map and makes sure
	 * it is properly loaded.
	 * 
	 * @param java
	 *            .lang.String mapName the path of image map
	 */
	private void createImage(String mapName)
	{
		m_image = Toolkit.getDefaultToolkit().createImage(mapName);
		
		try
		{
			Util.ensureImageLoaded(m_image);
		} catch (InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * javax.swing.JPanel createMainPanel()
	 * 
	 * Creates the main panel and returns
	 * a JPanel object.
	 * 
	 * @return javax.swing.JPanel the panel to return
	 */
	private JPanel createMainPanel()
	{
		JPanel imagePanel = new JPanel()
		{
			@Override
			public void paint(Graphics g)
			{
				// super.paint(g);
				g.drawImage(m_image, 0, 0, this);
				g.setColor(Color.red);
				
				Iterator<String> polyIter = m_centers.keySet().iterator();
				while (polyIter.hasNext())
				{
					String centerName = polyIter.next();
					Point item = m_centers.get(centerName);
					g.fillOval(item.x, item.y, 15, 15);
					g.drawString(centerName, item.x + 17, item.y + 13);
				}
			}
		};
		return imagePanel;
	}
	
	/**
	 * saveCenters()
	 * 
	 * Saves the centers to disk.
	 */
	private void saveCenters()
	{
		
		try
		{
			String fileName = new FileSave("Where To Save centers.txt ?", "centers.txt").getPathString();
			
			if (fileName == null)
			{
				return;
			}
			
			FileOutputStream out = new FileOutputStream(fileName);
			PointFileReaderWriter.writeOneToOne(out, m_centers);
			out.flush();
			out.close();
			
			System.out.println("Data written to :" + new File(fileName).getCanonicalPath());
		}

		catch (FileNotFoundException ex)
		{
			ex.printStackTrace();
		} catch (HeadlessException ex)
		{
			ex.printStackTrace();
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * loadCenters()
	 * 
	 * Loads a pre-defined file with map center points.
	 */
	private void loadCenters()
	{
		try
		{
			System.out.println("Load a center file");
			String centerName = new FileOpen("Load A Center File").getPathString();
			
			if (centerName == null)
			{
				return;
			}
			
			FileInputStream in = new FileInputStream(centerName);
			m_centers = PointFileReaderWriter.readOneToOne(in);
			repaint();
		} catch (FileNotFoundException ex)
		{
			ex.printStackTrace();
		} catch (IOException ex)
		{
			ex.printStackTrace();
		} catch (HeadlessException ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * java.lang.String findTerritoryName(java.awt.Point)
	 * 
	 * Finds a land territory name or
	 * some sea zone name.
	 * 
	 * @param java
	 *            .awt.point p a point on the map
	 */
	private String findTerritoryName(Point p)
	{
		String seaName = "unknown";
		
		// try to find a land territory.
		// sea zones often surround a land territory
		
		Iterator<String> keyIter = m_polygons.keySet().iterator();
		while (keyIter.hasNext())
		{
			String name = keyIter.next();
			Collection<Polygon> polygons = m_polygons.get(name);
			Iterator<Polygon> polyIter = polygons.iterator();
			
			while (polyIter.hasNext())
			{
				Polygon poly = polyIter.next();
				
				if (poly.contains(p))
				{
					if (name.endsWith("Sea Zone") || name.startsWith("Sea Zone"))
					{
						seaName = name;
					}
					else
					{
						return name;
					}
					
				}// if
				
			}// while
			
		}// while
		
		return seaName;
	}
	
	/**
	 * mouseEvent(java.awt.Point, java.lang.boolean, java.lang.boolean)
	 * 
	 * @param java
	 *            .awt.Point point a point clicked by mouse
	 * @param java
	 *            .lang.boolean ctrlDown true if ctrl key was hit
	 * @param java
	 *            .lang.boolean rightMouse true if the right mouse button was hit
	 */
	private void mouseEvent(Point point, boolean ctrlDown, boolean rightMouse)
	{
		if (!rightMouse)
		{
			String name = findTerritoryName(point);
			name = JOptionPane.showInputDialog(this, "Enter the territory name:", name);
			if (name.trim().length() == 0)
				return;
			if (m_centers.containsKey(name) && JOptionPane.showConfirmDialog(this, "Another center exists with the same name. Are you sure you want to replace it with this one?") != 0)
				return;
			
			m_centers.put(name, point);
		}
		else
		{
			String centerClicked = null;
			for (Entry<String, Point> cur : m_centers.entrySet())
			{
				if (new Rectangle(cur.getValue(), new Dimension(15, 15)).intersects(new Rectangle(point, new Dimension(1, 1))))
					centerClicked = cur.getKey();
			}
			if (centerClicked != null && JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this center?") == 0)
				m_centers.remove(centerClicked);
		}
		repaint();
	}
}// end class CenterPicker
