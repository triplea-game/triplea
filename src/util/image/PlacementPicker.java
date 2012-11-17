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

import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.MapData;
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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
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

public class PlacementPicker extends JFrame
{
	private static final long serialVersionUID = 953019978051420881L;
	private final JCheckBoxMenuItem showAllModeItem;
	private final JCheckBoxMenuItem showOverflowModeItem;
	private final JCheckBoxMenuItem showIncompleteModeItem;
	private static boolean s_showAllMode = false;
	private static boolean s_showOverflowMode = false;
	private static boolean s_showIncompleteMode = false;
	private static int s_incompleteNum = 1;
	private Point m_currentSquare;
	private Image m_image;
	private final JLabel m_location = new JLabel();
	private Map<String, List<Polygon>> m_polygons = new HashMap<String, List<Polygon>>();
	private Map<String, List<Point>> m_placements;
	private List<Point> m_currentPlacements;
	private String m_currentCountry;
	private static int PLACEWIDTH = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private static int PLACEHEIGHT = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private static boolean placeDimensionsSet = false;
	private static double unit_zoom_percent = 1;
	private static int unit_width = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private static int unit_height = UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
	private static File s_mapFolderLocation = null;
	private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
	private static final String TRIPLEA_UNIT_ZOOM = "triplea.unit.zoom";
	private static final String TRIPLEA_UNIT_WIDTH = "triplea.unit.width";
	private static final String TRIPLEA_UNIT_HEIGHT = "triplea.unit.height";
	
	public static String[] getProperties()
	{
		return new String[] { TRIPLEA_MAP_FOLDER, TRIPLEA_UNIT_ZOOM, TRIPLEA_UNIT_WIDTH, TRIPLEA_UNIT_HEIGHT };
	}
	
	/**
	 * main(java.lang.String[])
	 * 
	 * Main program begins here.
	 * Asks the user to select the map then runs the
	 * the actual placement picker program.
	 * 
	 * @param java
	 *            .lang.String[] args the command line arguments
	 * @see Picker(java.lang.String) picker
	 */
	public static void main(final String[] args)
	{
		handleCommandLineArgs(args);
		JOptionPane.showMessageDialog(null, new JLabel("<html>"
					+ "This is the PlacementPicker, it will create a place.txt file for you. "
					+ "<br>In order to run this, you must already have created a centers.txt file and a polygons.txt file. "
					+ "<br><br>The program will ask for unit scale (unit zoom) level [normally between 0.5 and 1.0], "
					+ "<br>Then it will ask for the unit image size when not zoomed [normally 48x48]. "
					+ "<br><br>If you want to have less, or more, room around the edges of your units, you can change the unit size. "
					+ "<br><br>After it starts, you may Load an existing place.txt file, that way you can make changes to it then save it. "
					+ "<br><br>LEFT CLICK = Select a new territory. "
					+ "<br><br>Holding CTRL/SHIFT + LEFT CLICK = Create a new placement for that territory. "
					+ "<br><br>RIGHT CLICK = Remove last placement for that territory. "
					+ "<br><br>Holding CTRL/SHIFT + RIGHT CLICK = Save all placements for that territory. "
					+ "<br><br>It is a very good idea to check each territory using the PlacementPicker after running the AutoPlacementFinder "
					+ "<br>to make sure there are enough placements for each territory. If not, you can always add more then save it. "
					+ "<br><br>IF there are not enough placements, the units will Overflow to the RIGHT of the very LAST placement made, "
					+ "<br>so be sure that the last placement is on the right side of the territory "
					+ "<br>or that it does not overflow directly on top of other placements. "
					+ "<br><br>To show all placements, or see the overflow direction, or see which territories you have not yet completed enough, "
					+ "<br>placements for, turn on the mode options in the 'edit' menu. "
					+ "</html>"));
		System.out.println("Select the map");
		final FileOpen mapSelection = new FileOpen("Select The Map", s_mapFolderLocation, ".gif", ".png");
		final String mapName = mapSelection.getPathString();
		if (s_mapFolderLocation == null && mapSelection.getFile() != null)
			s_mapFolderLocation = mapSelection.getFile().getParentFile();
		if (mapName != null)
		{
			final PlacementPicker picker = new PlacementPicker(mapName);
			picker.setSize(800, 600);
			picker.setLocationRelativeTo(null);
			picker.setVisible(true);
		}
		else
		{
			System.out.println("No Image Map Selected. Shutting down.");
			System.exit(0);
		}
	}// end main
	
	/**
	 * Constructor PlacementPicker(java.lang.String)
	 * 
	 * Setus up all GUI components, initializes variables with
	 * default or needed values, and prepares the map for user
	 * commands.
	 * 
	 * @param java
	 *            .lang.String mapName name of map file
	 */
	public PlacementPicker(final String mapName)
	{
		super("Placement Picker");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		if (!placeDimensionsSet)
		{
			try
			{
				File file = null;
				if (s_mapFolderLocation != null && s_mapFolderLocation.exists())
					file = new File(s_mapFolderLocation, "map.properties");
				if (file == null || !file.exists())
					file = new File(new File(mapName).getParent() + File.separator + "map.properties");
				if (file.exists())
				{
					double scale = unit_zoom_percent;
					int width = unit_width;
					int height = unit_height;
					boolean found = false;
					final String scaleProperty = MapData.PROPERTY_UNITS_SCALE + "=";
					final String widthProperty = MapData.PROPERTY_UNITS_WIDTH + "=";
					final String heightProperty = MapData.PROPERTY_UNITS_HEIGHT + "=";
					
					final FileReader reader = new FileReader(file);
					final LineNumberReader reader2 = new LineNumberReader(reader);
					int i = 0;
					while (true)
					{
						reader2.setLineNumber(i);
						final String line = reader2.readLine();
						if (line == null)
							break;
						if (line.contains(scaleProperty))
						{
							try
							{
								scale = Double.parseDouble(line.substring(line.indexOf(scaleProperty) + scaleProperty.length()).trim());
								found = true;
							} catch (final NumberFormatException ex)
							{
							}
						}
						if (line.contains(widthProperty))
						{
							try
							{
								width = Integer.parseInt(line.substring(line.indexOf(widthProperty) + widthProperty.length()).trim());
								found = true;
							} catch (final NumberFormatException ex)
							{
							}
						}
						if (line.contains(heightProperty))
						{
							try
							{
								height = Integer.parseInt(line.substring(line.indexOf(heightProperty) + heightProperty.length()).trim());
								found = true;
							} catch (final NumberFormatException ex)
							{
							}
						}
					}
					i++;
					if (found)
					{
						final int result = JOptionPane.showConfirmDialog(new JPanel(),
										"A map.properties file was found in the map's folder, " +
													"\r\n do you want to use the file to supply the info for the placement box size? " +
													"\r\n Zoom = " + scale + ",  Width = " + width + ",  Height = " + height +
													",    Result = (" + ((int) (scale * width)) + "x" + ((int) (scale * height)) + ")", "File Suggestion", 1);
						// if (result == 2)
						// return;
						if (result == 0)
						{
							unit_zoom_percent = scale;
							PLACEWIDTH = (int) (unit_zoom_percent * width);
							PLACEHEIGHT = (int) (unit_zoom_percent * height);
							placeDimensionsSet = true;
						}
					}
				}
			} catch (final Exception ex)
			{
			}
		}
		if (!placeDimensionsSet
					|| JOptionPane.showConfirmDialog(new JPanel(), "Placement Box Size already set (" + PLACEWIDTH + "x" + PLACEHEIGHT + "), " +
								"do you wish to continue with this?\r\nSelect Yes to continue, Select No to override and change the size.", "Placement Box Size", JOptionPane.YES_NO_OPTION) == 1)
		{
			try
			{
				final String result = getUnitsScale();
				try
				{
					unit_zoom_percent = Double.parseDouble(result.toLowerCase());
				} catch (final NumberFormatException ex)
				{
				}
				final String width = JOptionPane.showInputDialog(null, "Enter the unit's image width in pixels (unscaled / without zoom).\r\n(e.g. 48)");
				if (width != null)
				{
					try
					{
						PLACEWIDTH = (int) (unit_zoom_percent * Integer.parseInt(width));
					} catch (final NumberFormatException ex)
					{
					}
				}
				final String height = JOptionPane.showInputDialog(null, "Enter the unit's image height in pixels (unscaled / without zoom).\r\n(e.g. 48)");
				if (height != null)
				{
					try
					{
						PLACEHEIGHT = (int) (unit_zoom_percent * Integer.parseInt(height));
					} catch (final NumberFormatException ex)
					{
					}
				}
				placeDimensionsSet = true;
			} catch (final Exception ex)
			{
			}
		}
		
		File file = null;
		if (s_mapFolderLocation != null && s_mapFolderLocation.exists())
			file = new File(s_mapFolderLocation, "polygons.txt");
		if (file == null || !file.exists())
			file = new File(new File(mapName).getParent() + File.separator + "polygons.txt");
		if (file.exists()
					&& JOptionPane.showConfirmDialog(new JPanel(), "A polygons.txt file was found in the map's folder, do you want to use the file to supply the territories?", "File Suggestion", 1) == 0)
		{
			try
			{
				System.out.println("Polygons : " + file.getPath());
				m_polygons = PointFileReaderWriter.readOneToManyPolygons(new FileInputStream(file.getPath()));
			} catch (final IOException ex1)
			{
				ex1.printStackTrace();
			}
		}
		else
		{
			try
			{
				System.out.println("Select the Polygons file");
				final String polyPath = new FileOpen("Select A Polygon File", s_mapFolderLocation, ".txt").getPathString();
				if (polyPath != null)
				{
					System.out.println("Polygons : " + polyPath);
					m_polygons = PointFileReaderWriter.readOneToManyPolygons(new FileInputStream(polyPath));
				}
				else
				{
					System.out.println("Polygons file not given. Will run regardless");
				}
			} catch (final IOException ex1)
			{
				ex1.printStackTrace();
			}
		}
		createImage(mapName);
		final JPanel imagePanel = createMainPanel();
		/*
		Add a mouse listener to show
		X : Y coordinates on the lower
		left corner of the screen.
		*/
		imagePanel.addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseMoved(final MouseEvent e)
			{
				m_location.setText("x:" + e.getX() + " y:" + e.getY());
				m_currentSquare = new Point(e.getPoint());
				repaint();
			}
		});
		/*
		   Add a mouse listener to monitor
		for right mouse button being
		clicked.	
		*/
		imagePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(final MouseEvent e)
			{
				mouseEvent(e.getPoint(), e.isControlDown() || e.isShiftDown(), SwingUtilities.isRightMouseButton(e));
			}
		});
		// set up the image panel size dimensions ...etc
		imagePanel.setMinimumSize(new Dimension(m_image.getWidth(this), m_image.getHeight(this)));
		imagePanel.setPreferredSize(new Dimension(m_image.getWidth(this), m_image.getHeight(this)));
		imagePanel.setMaximumSize(new Dimension(m_image.getWidth(this), m_image.getHeight(this)));
		// set up the layout manager
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(new JScrollPane(imagePanel), BorderLayout.CENTER);
		this.getContentPane().add(m_location, BorderLayout.SOUTH);
		// set up the actions
		final Action openAction = new AbstractAction("Load Placements")
		{
			private static final long serialVersionUID = -2894085191455411106L;
			
			public void actionPerformed(final ActionEvent event)
			{
				loadPlacements();
			}
		};
		openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Placement File");
		final Action saveAction = new AbstractAction("Save Placements")
		{
			private static final long serialVersionUID = -3341738809601318716L;
			
			public void actionPerformed(final ActionEvent event)
			{
				savePlacements();
			}
		};
		saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Placements To File");
		final Action exitAction = new AbstractAction("Exit")
		{
			private static final long serialVersionUID = -9093426903644867897L;
			
			public void actionPerformed(final ActionEvent event)
			{
				System.exit(0);
			}
		};
		exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
		// set up the menu items
		final JMenuItem openItem = new JMenuItem(openAction);
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		final JMenuItem saveItem = new JMenuItem(saveAction);
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		final JMenuItem exitItem = new JMenuItem(exitAction);
		// set up the menu bar
		final JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		final JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		s_showAllMode = false;
		s_showOverflowMode = false;
		s_showIncompleteMode = false;
		s_incompleteNum = 1;
		showAllModeItem = new JCheckBoxMenuItem("Show All Placements Mode", false);
		showAllModeItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent event)
			{
				s_showAllMode = showAllModeItem.getState();
				repaint();
			}
		});
		showOverflowModeItem = new JCheckBoxMenuItem("Show Overflow Mode", false);
		showOverflowModeItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent event)
			{
				s_showOverflowMode = showOverflowModeItem.getState();
				repaint();
			}
		});
		showIncompleteModeItem = new JCheckBoxMenuItem("Show Incomplete Placements Mode", false);
		showIncompleteModeItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent event)
			{
				if (showIncompleteModeItem.getState())
				{
					final String num = JOptionPane.showInputDialog(null, "Enter the minimum number of placements each territory must have.\r\n(examples: 1, 4, etc.)");
					try
					{
						s_incompleteNum = Math.max(1, Math.min(50, Integer.parseInt(num)));
					} catch (final Exception ex)
					{
						s_incompleteNum = 1;
					}
				}
				s_showIncompleteMode = showIncompleteModeItem.getState();
				repaint();
			}
		});
		final JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		editMenu.add(showAllModeItem);
		editMenu.add(showOverflowModeItem);
		editMenu.add(showIncompleteModeItem);
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
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
	private void createImage(final String mapName)
	{
		m_image = Toolkit.getDefaultToolkit().createImage(mapName);
		try
		{
			Util.ensureImageLoaded(m_image);
		} catch (final InterruptedException ex)
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
		final JPanel imagePanel = new JPanel()
		{
			private static final long serialVersionUID = -3941975573431195136L;
			
			@Override
			public void paint(final Graphics g)
			{
				// super.paint(g);
				g.drawImage(m_image, 0, 0, this);
				if (s_showAllMode)
				{
					g.setColor(Color.yellow);
					for (final Entry<String, List<Point>> entry : m_placements.entrySet())
					{
						if (entry.getKey().equals(m_currentCountry) && m_currentPlacements != null && !m_currentPlacements.isEmpty())
							continue;
						final Iterator<Point> pointIter = entry.getValue().iterator();
						while (pointIter.hasNext())
						{
							final Point item = pointIter.next();
							g.fillRect(item.x, item.y, PLACEWIDTH, PLACEHEIGHT);
							if (s_showOverflowMode && !pointIter.hasNext())
							{
								g.setColor(Color.gray);
								g.fillRect(item.x + PLACEWIDTH, item.y + PLACEHEIGHT / 2, PLACEWIDTH, 4);
								g.setColor(Color.yellow);
							}
						}
					}
				}
				if (s_showIncompleteMode)
				{
					g.setColor(Color.green);
					final Set<String> territories = new HashSet<String>(m_polygons.keySet());
					final Iterator<String> terrIter = territories.iterator();
					while (terrIter.hasNext())
					{
						final String terr = terrIter.next();
						final List<Point> points = m_placements.get(terr);
						if (points != null && points.size() >= s_incompleteNum)
							terrIter.remove();
					}
					for (final String terr : territories)
					{
						final List<Polygon> polys = m_polygons.get(terr);
						if (polys == null || polys.isEmpty())
							continue;
						for (final Polygon poly : polys)
						{
							g.fillPolygon(poly);
						}
					}
				}
				
				g.setColor(Color.red);
				if (m_currentSquare != null)
				{
					g.drawRect(m_currentSquare.x, m_currentSquare.y, PLACEWIDTH, PLACEHEIGHT);
				}
				if (m_currentPlacements == null)
				{
					return;
				}
				final Iterator<Point> pointIter = m_currentPlacements.iterator();
				while (pointIter.hasNext())
				{
					final Point item = pointIter.next();
					g.fillRect(item.x, item.y, PLACEWIDTH, PLACEHEIGHT);
					if (s_showOverflowMode && !pointIter.hasNext())
					{
						g.setColor(Color.gray);
						g.fillRect(item.x + PLACEWIDTH, item.y + PLACEHEIGHT / 2, PLACEWIDTH, 4);
						g.setColor(Color.red);
					}
				}
			}// paint
		};
		return imagePanel;
	}
	
	/**
	 * savePlacements()
	 * 
	 * Saves the placements to disk.
	 */
	private void savePlacements()
	{
		try
		{
			final String fileName = new FileSave("Where To Save place.txt ?", "place.txt", s_mapFolderLocation).getPathString();
			if (fileName == null)
			{
				return;
			}
			final FileOutputStream out = new FileOutputStream(fileName);
			PointFileReaderWriter.writeOneToMany(out, m_placements);
			out.flush();
			out.close();
			System.out.println("Data written to :" + new File(fileName).getCanonicalPath());
		} catch (final FileNotFoundException ex)
		{
			ex.printStackTrace();
		} catch (final HeadlessException ex)
		{
			ex.printStackTrace();
		} catch (final Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * loadPlacements()
	 * 
	 * Loads a pre-defined file with map placement points.
	 */
	private void loadPlacements()
	{
		try
		{
			System.out.println("Load a placement file");
			final String placeName = new FileOpen("Load A Placement File", s_mapFolderLocation, ".txt").getPathString();
			if (placeName == null)
			{
				return;
			}
			final FileInputStream in = new FileInputStream(placeName);
			m_placements = PointFileReaderWriter.readOneToMany(in);
			repaint();
		} catch (final FileNotFoundException ex)
		{
			ex.printStackTrace();
		} catch (final IOException ex)
		{
			ex.printStackTrace();
		} catch (final HeadlessException ex)
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
	private String findTerritoryName(final Point p)
	{
		String seaName = "there be dragons";
		// try to find a land territory.
		// sea zones often surround a land territory
		for (final String name : m_polygons.keySet())
		{
			final Collection<Polygon> polygons = m_polygons.get(name);
			for (final Polygon poly : polygons)
			{
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
	 * Usage:
	 * left button start in territory
	 * left button + control, add point
	 * right button and ctrl write
	 * right button remove last
	 * 
	 * @param java
	 *            .awt.Point point a point clicked by mouse
	 * @param java
	 *            .lang.boolean ctrlDown true if ctrl key was hit
	 * @param java
	 *            .lang.boolean rightMouse true if the right mouse button was hit
	 */
	private void mouseEvent(final Point point, final boolean ctrlDown, final boolean rightMouse)
	{
		if (!rightMouse && !ctrlDown)
		{
			m_currentCountry = findTerritoryName(point);
			// If there isn't an existing array, create one
			if (m_placements == null || m_placements.get(m_currentCountry) == null)
				m_currentPlacements = new ArrayList<Point>();
			else
				m_currentPlacements = new ArrayList<Point>(m_placements.get(m_currentCountry));
			JOptionPane.showMessageDialog(this, m_currentCountry);
		}
		else if (!rightMouse && ctrlDown)
		{
			if (m_currentPlacements != null)
				m_currentPlacements.add(point);
		}
		else if (rightMouse && ctrlDown)
		{
			if (m_currentPlacements != null)
			{
				// If there isn't an existing hashmap, create one
				if (m_placements == null)
				{
					m_placements = new HashMap<String, List<Point>>();
				}
				m_placements.put(m_currentCountry, m_currentPlacements);
				
				m_currentPlacements = new ArrayList<Point>();
				System.out.println("done:" + m_currentCountry);
			}
		}
		else if (rightMouse)
		{
			if (m_currentPlacements != null && !m_currentPlacements.isEmpty())
			{
				m_currentPlacements.remove(m_currentPlacements.size() - 1);
			}
		}
		repaint();
	}
	
	private static String getUnitsScale()
	{
		final String unitsScale = JOptionPane.showInputDialog(null, "Enter the unit's scale (zoom).\r\n(e.g. 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5)");
		if (unitsScale != null)
		{
			return unitsScale;
		}
		else
		{
			return "1";
		}
	}
	
	private static String getValue(final String arg)
	{
		final int index = arg.indexOf('=');
		if (index == -1)
			return "";
		return arg.substring(index + 1);
	}
	
	private static void handleCommandLineArgs(final String[] args)
	{
		final String[] properties = getProperties();
		if (args.length == 1)
		{
			String value;
			if (args[0].startsWith(TRIPLEA_UNIT_ZOOM))
			{
				value = getValue(args[0]);
			}
			else
			{
				value = args[0];
			}
			try
			{
				Double.parseDouble(value);
				System.setProperty(TRIPLEA_UNIT_ZOOM, value);
			} catch (final Exception ex)
			{
			}
		}
		else if (args.length == 2)
		{
			String value0;
			if (args[0].startsWith(TRIPLEA_UNIT_WIDTH))
			{
				value0 = getValue(args[0]);
			}
			else
			{
				value0 = args[0];
			}
			try
			{
				Integer.parseInt(value0);
				System.setProperty(TRIPLEA_UNIT_WIDTH, value0);
			} catch (final Exception ex)
			{
			}
			
			String value1;
			if (args[0].startsWith(TRIPLEA_UNIT_HEIGHT))
			{
				value1 = getValue(args[1]);
			}
			else
			{
				value1 = args[1];
			}
			try
			{
				Integer.parseInt(value1);
				System.setProperty(TRIPLEA_UNIT_HEIGHT, value1);
			} catch (final Exception ex)
			{
			}
		}
		
		boolean usagePrinted = false;
		for (int argIndex = 0; argIndex < args.length; argIndex++)
		{
			boolean found = false;
			String arg = args[argIndex];
			final int indexOf = arg.indexOf('=');
			if (indexOf > 0)
			{
				arg = arg.substring(0, indexOf);
				for (int propIndex = 0; propIndex < properties.length; propIndex++)
				{
					if (arg.equals(properties[propIndex]))
					{
						final String value = getValue(args[argIndex]);
						System.getProperties().setProperty(properties[propIndex], value);
						System.out.println(properties[propIndex] + ":" + value);
						found = true;
						break;
					}
				}
			}
			if (!found)
			{
				System.out.println("Unrecogized:" + args[argIndex]);
				if (!usagePrinted)
				{
					usagePrinted = true;
					System.out.println("Arguments\r\n"
									+ "   " + TRIPLEA_MAP_FOLDER + "=<FILE_PATH>\r\n"
									+ "   " + TRIPLEA_UNIT_ZOOM + "=<UNIT_ZOOM_LEVEL>\r\n"
									+ "   " + TRIPLEA_UNIT_WIDTH + "=<UNIT_WIDTH>\r\n"
									+ "   " + TRIPLEA_UNIT_HEIGHT + "=<UNIT_HEIGHT>\r\n");
				}
			}
		}
		String folderString = System.getProperty(TRIPLEA_MAP_FOLDER);
		if (folderString != null && folderString.length() > 0)
		{
			folderString = folderString.replaceAll("\\(", " ");
			final File mapFolder = new File(folderString);
			if (mapFolder.exists())
				s_mapFolderLocation = mapFolder;
			else
				System.out.println("Could not find directory: " + folderString);
		}
		final String zoomString = System.getProperty(TRIPLEA_UNIT_ZOOM);
		if (zoomString != null && zoomString.length() > 0)
		{
			try
			{
				unit_zoom_percent = Double.parseDouble(zoomString);
				System.out.println("Unit Zoom Percent to use: " + unit_zoom_percent);
				placeDimensionsSet = true;
			} catch (final Exception ex)
			{
				System.err.println("Not a decimal percentage: " + zoomString);
			}
		}
		final String widthString = System.getProperty(TRIPLEA_UNIT_WIDTH);
		if (widthString != null && widthString.length() > 0)
		{
			try
			{
				unit_width = Integer.parseInt(widthString);
				System.out.println("Unit Width to use: " + unit_width);
				placeDimensionsSet = true;
			} catch (final Exception ex)
			{
				System.err.println("Not an integer: " + widthString);
			}
		}
		final String heightString = System.getProperty(TRIPLEA_UNIT_HEIGHT);
		if (heightString != null && heightString.length() > 0)
		{
			try
			{
				unit_height = Integer.parseInt(heightString);
				System.out.println("Unit Height to use: " + unit_height);
				placeDimensionsSet = true;
			} catch (final Exception ex)
			{
				System.err.println("Not an integer: " + heightString);
			}
		}
		if (placeDimensionsSet)
		{
			PLACEWIDTH = (int) (unit_zoom_percent * unit_width);
			PLACEHEIGHT = (int) (unit_zoom_percent * unit_height);
			System.out.println("Place Dimensions to use: " + PLACEWIDTH + "x" + PLACEHEIGHT);
		}
	}
}// end class PlacementPicker
