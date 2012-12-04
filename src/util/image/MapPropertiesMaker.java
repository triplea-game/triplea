package util.image;

import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.ui.DoubleTextField;
import games.strategy.ui.IntTextField;
import games.strategy.util.Tuple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * This is the MapPropertiesMaker, it will create a map.properties file for you. <br>
 * The map.properties is located in the map's directory, and it will tell TripleA various
 * display related information about your map. <br>
 * Such things as the dimensions of your map, the colors of each of the players,
 * the size of the unit images, and how zoomed out they are, etc. <br>
 * To use, just fill in the information in the fields below, and click on 'Show More' to
 * show other, optional, fields.
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public class MapPropertiesMaker extends JFrame
{
	private static final long serialVersionUID = 8182821091131994702L;
	private static File s_mapFolderLocation = null;
	private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
	private static final String TRIPLEA_UNIT_ZOOM = "triplea.unit.zoom";
	private static final String TRIPLEA_UNIT_WIDTH = "triplea.unit.width";
	private static final String TRIPLEA_UNIT_HEIGHT = "triplea.unit.height";
	private static final MapProperties s_mapProperties = new MapProperties();
	private static JPanel s_playerColorChooser = new JPanel();
	
	public static String[] getProperties()
	{
		return new String[] { TRIPLEA_MAP_FOLDER, TRIPLEA_UNIT_ZOOM, TRIPLEA_UNIT_WIDTH, TRIPLEA_UNIT_HEIGHT };
	}
	
	public static void main(final String[] args)
	{
		handleCommandLineArgs(args);
		// JOptionPane.showMessageDialog(null, new JLabel("<html>" + "This is the MapPropertiesMaker, it will create a map.properties file for you. " + "</html>"));
		if (s_mapFolderLocation == null)
		{
			System.out.println("Select the map folder");
			final String path = new FileSave("Where is your map's folder?", null, s_mapFolderLocation).getPathString();
			if (path != null)
			{
				final File mapFolder = new File(path);
				if (mapFolder.exists())
				{
					s_mapFolderLocation = mapFolder;
					System.setProperty(TRIPLEA_MAP_FOLDER, s_mapFolderLocation.getPath());
				}
			}
		}
		if (s_mapFolderLocation != null)
		{
			final MapPropertiesMaker maker = new MapPropertiesMaker();
			maker.setSize(800, 800);
			maker.setLocationRelativeTo(null);
			maker.setVisible(true);
		}
		else
		{
			System.out.println("No Map Folder Selected. Shutting down.");
			System.exit(0);
		}
	}// end main
	
	public MapPropertiesMaker()
	{
		super("Map Properties Maker");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.getContentPane().setLayout(new BorderLayout());
		final JPanel panel = createPropertiesPanel();
		this.getContentPane().add(new JScrollPane(panel), BorderLayout.CENTER);
		// set up the actions
		final Action openAction = new AbstractAction("Load Properties")
		{
			private static final long serialVersionUID = -3135749471880991185L;
			
			public void actionPerformed(final ActionEvent event)
			{
				loadProperties();
			}
		};
		openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Properties File");
		final Action saveAction = new AbstractAction("Save Properties")
		{
			private static final long serialVersionUID = -5608941822299486808L;
			
			public void actionPerformed(final ActionEvent event)
			{
				saveProperties();
			}
		};
		saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Properties To File");
		final Action exitAction = new AbstractAction("Exit")
		{
			private static final long serialVersionUID = -9212762817640498442L;
			
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
		// fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		menuBar.add(fileMenu);
	}
	
	private JPanel createPropertiesPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		int row = 0;
		panel.add(new JLabel("<html>"
					+ "This is the MapPropertiesMaker, it will create a map.properties file for you. "
					+ "<br>The map.properties is located in the map's directory, and it will tell TripleA various "
					+ "<br>display related information about your map. "
					+ "<br>Such things as the dimensions of your map, the colors of each of the players, "
					+ "<br>the size of the unit images, and how zoomed out they are, etc. "
					+ "<br>To use, just fill in the information in the fields below, and click on 'Show More' to "
					+ "<br>show other, optional, fields. "
					+ "</html>"),
					new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 20, 20, 20), 0, 0));
		
		panel.add(new JLabel("The Width in Pixels of your map: "), new GridBagConstraints(0, row, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		final IntTextField widthField = new IntTextField(0, Integer.MAX_VALUE);
		widthField.setText("" + s_mapProperties.getMAP_WIDTH());
		widthField.addFocusListener(new FocusListener()
		{
			public void focusGained(final FocusEvent e)
			{
			}
			
			public void focusLost(final FocusEvent e)
			{
				try
				{
					s_mapProperties.setMAP_WIDTH(Integer.parseInt(widthField.getText()));
				} catch (final Exception ex)
				{
				}
				widthField.setText("" + s_mapProperties.getMAP_WIDTH());
			}
		});
		panel.add(widthField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		
		panel.add(new JLabel("The Height in Pixels of your map: "), new GridBagConstraints(0, row, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		final IntTextField heightField = new IntTextField(0, Integer.MAX_VALUE);
		heightField.setText("" + s_mapProperties.getMAP_HEIGHT());
		heightField.addFocusListener(new FocusListener()
		{
			public void focusGained(final FocusEvent e)
			{
			}
			
			public void focusLost(final FocusEvent e)
			{
				try
				{
					s_mapProperties.setMAP_HEIGHT(Integer.parseInt(heightField.getText()));
				} catch (final Exception ex)
				{
				}
				heightField.setText("" + s_mapProperties.getMAP_HEIGHT());
			}
		});
		panel.add(heightField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		
		panel.add(new JLabel("<html>The initial Scale (zoom) of your unit images: "
					+ "<br>Must be one of: 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5</html>"),
					new GridBagConstraints(0, row, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		final DoubleTextField scaleField = new DoubleTextField(0.1d, 2.0d);
		scaleField.setText("" + s_mapProperties.getUNITS_SCALE());
		scaleField.addFocusListener(new FocusListener()
		{
			public void focusGained(final FocusEvent e)
			{
			}
			
			public void focusLost(final FocusEvent e)
			{
				try
				{
					// s_mapProperties.setUNITS_SCALE(Double.parseDouble(scaleField.getText()));
					s_mapProperties.setUNITS_SCALE(scaleField.getText());
				} catch (final Exception ex)
				{
				}
				scaleField.setText("" + s_mapProperties.getUNITS_SCALE());
			}
		});
		panel.add(scaleField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		
		panel.add(new JLabel("Create Players and Click on the Color to set their Color: "),
					new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 50, 20, 50), 0, 0));
		createPlayerColorChooser();
		panel.add(s_playerColorChooser, new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		final JButton showMore = new JButton("Show All Options");
		showMore.addActionListener(new AbstractAction("Show All Options")
		{
			private static final long serialVersionUID = -794092512377464803L;
			
			public void actionPerformed(final ActionEvent e)
			{
				@SuppressWarnings("rawtypes")
				final Tuple<PropertiesUI, List<MapPropertyWrapper>> propertyWrapperUI = MapPropertiesMaker.s_mapProperties.propertyWrapperUI(true);
				JOptionPane.showMessageDialog(MapPropertiesMaker.this, propertyWrapperUI.getFirst());
				s_mapProperties.writePropertiesToObject(propertyWrapperUI.getSecond());
				MapPropertiesMaker.this.createPlayerColorChooser();
				MapPropertiesMaker.this.validate();
				MapPropertiesMaker.this.repaint();
			}
		});
		panel.add(showMore, new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		return panel;
	}
	
	private void createPlayerColorChooser()
	{
		s_playerColorChooser.removeAll();
		s_playerColorChooser.setLayout(new GridBagLayout());
		int row = 0;
		for (final Entry<String, Color> entry : s_mapProperties.getCOLOR_MAP().entrySet())
		{
			s_playerColorChooser.add(new JLabel(entry.getKey()), new GridBagConstraints(0, row, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
			final JLabel label = new JLabel(entry.getKey())
			{
				private static final long serialVersionUID = 5624227155029721033L;
				
				@Override
				public void paintComponent(final Graphics g)
				{
					final Graphics2D g2 = (Graphics2D) g;
					g2.setColor(entry.getValue());
					g2.fill(g2.getClip());
				}
			};
			label.setBackground(entry.getValue());
			label.addMouseListener(new MouseListener()
			{
				public void mouseClicked(final MouseEvent e)
				{
					System.out.println(label.getBackground());
					final Color color = JColorChooser.showDialog(label, "Choose color", label.getBackground());
					s_mapProperties.getCOLOR_MAP().put(label.getText(), color);
					MapPropertiesMaker.this.createPlayerColorChooser();
					MapPropertiesMaker.this.validate();
					MapPropertiesMaker.this.repaint();
				}
				
				public void mouseEntered(final MouseEvent e)
				{
				}
				
				public void mouseExited(final MouseEvent e)
				{
				}
				
				public void mousePressed(final MouseEvent e)
				{
				}
				
				public void mouseReleased(final MouseEvent e)
				{
				}
			});
			s_playerColorChooser.add(label, new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
			final JButton removePlayer = new JButton("Remove " + entry.getKey());
			removePlayer.addActionListener(new AbstractAction("Remove " + entry.getKey())
			{
				private static final long serialVersionUID = -3593575469168341735L;
				
				public void actionPerformed(final ActionEvent e)
				{
					s_mapProperties.getCOLOR_MAP().remove(removePlayer.getText().replaceFirst("Remove ", ""));
					MapPropertiesMaker.this.createPlayerColorChooser();
					MapPropertiesMaker.this.validate();
					MapPropertiesMaker.this.repaint();
				}
			});
			s_playerColorChooser.add(removePlayer, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
			row++;
		}
		final JTextField nameTextField = new JTextField("Player" + (s_mapProperties.getCOLOR_MAP().size() + 1));
		final Dimension ourMinimum = new Dimension(150, 30);
		nameTextField.setMinimumSize(ourMinimum);
		nameTextField.setPreferredSize(ourMinimum);
		final JButton addPlayer = new JButton("Add Another Player");
		addPlayer.addActionListener(new AbstractAction("Add Another Player")
		{
			private static final long serialVersionUID = -794092512377464803L;
			
			public void actionPerformed(final ActionEvent e)
			{
				s_mapProperties.getCOLOR_MAP().put(nameTextField.getText(), Color.GREEN);
				MapPropertiesMaker.this.createPlayerColorChooser();
				MapPropertiesMaker.this.validate();
				MapPropertiesMaker.this.repaint();
			}
		});
		s_playerColorChooser.add(addPlayer, new GridBagConstraints(0, row, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		s_playerColorChooser.add(nameTextField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
	}
	
	private void loadProperties()
	{
		final Properties properties = new Properties();
		try
		{
			System.out.println("Load a properties file");
			final String centerName = new FileOpen("Load A Properties File", s_mapFolderLocation, ".properties").getPathString();
			if (centerName == null)
			{
				return;
			}
			final FileInputStream in = new FileInputStream(centerName);
			properties.load(in);
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
		for (final Method setter : s_mapProperties.getClass().getMethods())
		{
			final boolean startsWithSet = setter.getName().startsWith("set");
			if (!startsWithSet)
				continue;
			final String propertyName = setter.getName().substring(Math.min(3, setter.getName().length()), setter.getName().length());
			final String value = properties.getProperty(propertyName);
			if (value == null)
				continue;
			// TODO: finish this
		}
		validate();
		repaint();
	}
	
	private void saveProperties()
	{
		try
		{
			final String fileName = new FileSave("Where To Save map.properties ?", "map.properties", s_mapFolderLocation).getPathString();
			if (fileName == null)
			{
				return;
			}
			final FileOutputStream sink = new FileOutputStream(fileName);
			final String stringToWrite = getOutPutString();
			final OutputStreamWriter out = new OutputStreamWriter(sink);
			out.write(stringToWrite);
			out.flush();
			out.close();
			System.out.println("");
			System.out.println("Data written to :" + new File(fileName).getCanonicalPath());
			System.out.println("");
			System.out.println(stringToWrite);
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
	
	private String getOutPutString()
	{
		final StringBuilder outString = new StringBuilder();
		for (final Method outMethod : s_mapProperties.getClass().getMethods())
		{
			final boolean startsWithSet = outMethod.getName().startsWith("out");
			if (!startsWithSet)
				continue;
			try
			{
				outString.append(outMethod.invoke(s_mapProperties));
			} catch (final IllegalArgumentException e)
			{
				e.printStackTrace();
			} catch (final IllegalAccessException e)
			{
				e.printStackTrace();
			} catch (final InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}
		return outString.toString();
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
		// now account for anything set by -D
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
				final double unit_zoom_percent = Double.parseDouble(zoomString);
				// s_mapProperties.setUNITS_SCALE(unit_zoom_percent);
				s_mapProperties.setUNITS_SCALE(zoomString);
				System.out.println("Unit Zoom Percent to use: " + unit_zoom_percent);
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
				final int unit_width = Integer.parseInt(widthString);
				s_mapProperties.setUNITS_WIDTH(unit_width);
				s_mapProperties.setUNITS_COUNTER_OFFSET_WIDTH(unit_width / 4);
				System.out.println("Unit Width to use: " + unit_width);
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
				final int unit_height = Integer.parseInt(heightString);
				s_mapProperties.setUNITS_HEIGHT(unit_height);
				s_mapProperties.setUNITS_COUNTER_OFFSET_HEIGHT(unit_height);
				System.out.println("Unit Height to use: " + unit_height);
			} catch (final Exception ex)
			{
				System.err.println("Not an integer: " + heightString);
			}
		}
	}
}
