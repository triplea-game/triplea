package util.image;

import games.strategy.engine.framework.GameRunner;
import games.strategy.triplea.ResourceLoader;
import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * 
 This is the DecorationPlacer, it will create a text file for you containing the points to place images at. <br>
 * <br>
 * In order to begin this, you must already have the map file, as well as the centers.txt and polygons.txt finished. <br>
 * To start, load you map image. Then you will be asked which kind of Image Point File you are creating. <br>
 * <br>
 * There are basically 2 different kinds of image point files, and with each of those are 2 different sub-types. <br>
 * The 1st type is a folder full of many different images, that after being placed on the map will never be changed. <br>
 * Examples of this are the decorations.txt file [misc folder] and the name_place.txt file [territoryNames folder]. <br>
 * In these files the 'point' string directly corresponds to exact name of an image file in the folder, with the only <br>
 * exception being whether the point string needs the .png extension or not (decorations do, name_place does not). <br>
 * <br>
 * The 2nd type is single image, or small set of images, where the chosen image is determined by something in the xml file. <br>
 * Examples of this are the pu_place.txt file [PUs folder] and the capitols.txt file [flags folder]. <br>
 * In these files, the 'point' string is the exact name of a territory, while the image file has a different name, <br>
 * and is chosen by the engine based on the game data. For things like the pu_place you may want the decoration placer <br>
 * to generate placements for all territories, while others like capitols are more rare and you may want to individually <br>
 * select which territories you need a placement point for. <br>
 * <br>
 * After selecting the point file type you want to make, the program will choose the default selections for you, <br>
 * but it will still confirm with you by asking you the questions. Just hit 'enter' a lot if you do not know the answers. <br>
 * <br>
 * Any images that this program can not find the point for, will start in the upper left corner of the map, <br>
 * and you may click on them to move them to their appropriate place." <br>
 * <br>
 * Do not forget to save the points when finished. To save and continue with another set of images, choose the <br>
 * option to 'Save Current And Keep On Map And Load New'. To reset all currently image points, use 'Load Image Points'.
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public class DecorationPlacer extends JFrame
{
	private static final long serialVersionUID = 6385408390173085656L;
	private Image m_image; // The map image will be stored here
	private Map<String, List<Point>> m_currentPoints = new HashMap<String, List<Point>>(); // hash map for image points
	private Map<String, Point> m_centers = new HashMap<String, Point>(); // hash map for center points
	private Map<String, List<Polygon>> m_polygons = new HashMap<String, List<Polygon>>(); // hash map for polygon points
	private final JLabel m_location = new JLabel();
	private static File s_mapFolderLocation = null;
	private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
	private static File s_currentImageFolderLocation = null;
	private static File s_currentImagePointsTextFile = null;
	private Point m_currentMousePoint = new Point(0, 0);
	private Triple<String, Image, Point> m_currentSelectedImage = null;
	private Map<String, Tuple<Image, List<Point>>> m_currentImagePoints = new HashMap<String, Tuple<Image, List<Point>>>();
	private static boolean s_highlightAll = false;
	private static boolean s_createNewImageOnRightClick = false;
	private static Image s_staticImageForPlacing = null;
	private static boolean s_showFromTopLeft = true;
	private static ImagePointType s_imagePointType = ImagePointType.decorations;
	private static boolean s_cheapMutex = false;
	private static boolean s_showPointNames = false;
	
	public static void main(final String[] args)
	{
		handleCommandLineArgs(args);
		System.out.println("Select the map");
		final FileOpen mapSelection = new FileOpen("Select The Map", s_mapFolderLocation, ".gif", ".png");
		final String mapName = mapSelection.getPathString();
		if (s_mapFolderLocation == null && mapSelection.getFile() != null)
			s_mapFolderLocation = mapSelection.getFile().getParentFile();
		if (mapName != null)
		{
			System.out.println("Map : " + mapName);
			final DecorationPlacer picker = new DecorationPlacer(mapName);
			picker.setSize(800, 600);
			picker.setLocationRelativeTo(null);
			picker.setVisible(true);
			JOptionPane.showMessageDialog(picker, new JLabel("<html>"
									+ "This is the DecorationPlacer, it will create a text file for you containing the points to place images at. "
									+ "<br><br>In order to begin this, you must already have the map file, as well as the centers.txt and polygons.txt finished. "
									+ "<br>To start, load you map image. Then you will be asked which kind of Image Point File you are creating. "
									+ "<br><br>There are basically 2 different kinds of image point files, and with each of those are 2 different sub-types. "
									+ "<br>The 1st type is a folder full of many different images, that after being placed on the map will never be changed. "
									+ "<br>Examples of this are the decorations.txt file [misc folder] and the name_place.txt file [territoryNames folder]. "
									+ "<br>In these files the 'point' string directly corresponds to exact name of an image file in the folder, with the only "
									+ "<br>exception being whether the point string needs the .png extension or not (decorations do, name_place does not). "
									+ "<br><br>The 2nd type is single image, or small set of images, where the chosen image is determined by something in the xml file. "
									+ "<br>Examples of this are the pu_place.txt file [PUs folder] and the capitols.txt file [flags folder]. "
									+ "<br>In these files, the 'point' string is the exact name of a territory, while the image file has a different name, "
									+ "<br>and is chosen by the engine based on the game data.  For things like the pu_place you may want the decoration placer "
									+ "<br>to generate placements for all territories, while others like capitols are more rare and you may want to individually "
									+ "<br>select which territories you need a placement point for."
									+ "<br><br>After selecting the point file type you want to make, the program will choose the default selections for you, "
									+ "<br>but it will still confirm with you by asking you the questions. Just hit 'enter' a lot if you do not know the answers. "
									+ "<br><br>Any images that this program can not find the point for, will start in the upper left corner of the map, "
									+ "<br>and you may click on them to move them to their appropriate place."
									+ "<br><br>Do not forget to save the points when finished. To save and continue with another set of images, choose the "
									+ "<br>option to 'Save Current And Keep On Map And Load New'.  To reset all currently image points, use 'Load Image Points'."
									+ "</html>"));
			picker.loadImagesAndPoints();
		}
		else
		{
			System.out.println("No Image Map Selected. Shutting down.");
			System.exit(0);
		}
	}// end main
	
	public DecorationPlacer(final String mapName)
	{
		super("Decoration Placer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		s_highlightAll = false;
		File fileCenters = null;
		if (s_mapFolderLocation != null && s_mapFolderLocation.exists())
			fileCenters = new File(s_mapFolderLocation, "centers.txt");
		if (fileCenters == null || !fileCenters.exists())
			fileCenters = new File(new File(mapName).getParent() + File.separator + "centers.txt");
		if (fileCenters.exists()
					&& JOptionPane.showConfirmDialog(new JPanel(), "A centers.txt file was found in the map's folder, do you want to use the file to supply the territories centers?",
								"File Suggestion",
								1) == 0)
		{
			try
			{
				System.out.println("Centers : " + fileCenters.getPath());
				m_centers = PointFileReaderWriter.readOneToOne(new FileInputStream(fileCenters.getPath()));
			} catch (final IOException ex1)
			{
				System.out.println("Something wrong with Centers file");
				ex1.printStackTrace();
				System.exit(0);
			}
		}
		else
		{
			try
			{
				System.out.println("Select the Centers file");
				final String centerPath = new FileOpen("Select A Center File", s_mapFolderLocation, ".txt").getPathString();
				if (centerPath != null)
				{
					System.out.println("Centers : " + centerPath);
					m_centers = PointFileReaderWriter.readOneToOne(new FileInputStream(centerPath));
				}
				else
				{
					System.out.println("You must specify a centers file.");
					System.out.println("Shutting down.");
					System.exit(0);
				}
			} catch (final IOException ex1)
			{
				System.out.println("Something wrong with Centers file");
				ex1.printStackTrace();
				System.exit(0);
			}
		}
		File filePoly = null;
		if (s_mapFolderLocation != null && s_mapFolderLocation.exists())
			filePoly = new File(s_mapFolderLocation, "polygons.txt");
		if (filePoly == null || !filePoly.exists())
			filePoly = new File(new File(mapName).getParent() + File.separator + "polygons.txt");
		if (filePoly.exists()
					&& JOptionPane.showConfirmDialog(new JPanel(), "A polygons.txt file was found in the map's folder, do you want to use the file to supply the territories polygons?",
								"File Suggestion", 1) == 0)
		{
			try
			{
				System.out.println("Polygons : " + filePoly.getPath());
				m_polygons = PointFileReaderWriter.readOneToManyPolygons(new FileInputStream(filePoly.getPath()));
			} catch (final IOException ex1)
			{
				System.out.println("Something wrong with your Polygons file");
				ex1.printStackTrace();
				System.exit(0);
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
					System.out.println("You must specify a Polgyon file.");
					System.out.println("Shutting down.");
					System.exit(0);
				}
			} catch (final IOException ex1)
			{
				System.out.println("Something wrong with your Polygons file");
				ex1.printStackTrace();
				System.exit(0);
			}
		}
		
		m_image = createImage(mapName);
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
				m_location.setText((m_currentSelectedImage == null ? "" : m_currentSelectedImage.getFirst()) + "    x:" + e.getX() + " y:" + e.getY());
				m_currentMousePoint = new Point(e.getPoint());
				DecorationPlacer.this.repaint();
			}
		});
		m_location.setFont(new Font("Ariel", Font.BOLD, 16));
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
		final Action openAction = new AbstractAction("Load Image Locations")
		{
			private static final long serialVersionUID = 2712234474452114083L;
			
			public void actionPerformed(final ActionEvent event)
			{
				loadImagesAndPoints();
			}
		};
		openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Image Points File");
		final Action saveAction = new AbstractAction("Save Image Locations")
		{
			private static final long serialVersionUID = -4519036149978621171L;
			
			public void actionPerformed(final ActionEvent event)
			{
				saveImagePoints();
			}
		};
		saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Image Points To File");
		final Action keepGoingAction = new AbstractAction("Save Current and Keep Them On Map and Load New File")
		{
			private static final long serialVersionUID = -7217861953409073730L;
			
			public void actionPerformed(final ActionEvent event)
			{
				saveImagePoints();
				saveCurrentToMapPicture();
				loadImagesAndPoints();
			}
		};
		keepGoingAction.putValue(Action.SHORT_DESCRIPTION, "Save current points to a file, then draw the images onto the map, then load a new points file.");
		final Action exitAction = new AbstractAction("Exit")
		{
			private static final long serialVersionUID = -5631457890653630218L;
			
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
		fileMenu.add(keepGoingAction);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		final JCheckBoxMenuItem highlightAllModeItem = new JCheckBoxMenuItem("Highlight All", false);
		highlightAllModeItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent event)
			{
				s_highlightAll = highlightAllModeItem.getState();
				DecorationPlacer.this.repaint();
			}
		});
		final JCheckBoxMenuItem showNamesModeItem = new JCheckBoxMenuItem("Show Point Names", false);
		showNamesModeItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent event)
			{
				s_showPointNames = showNamesModeItem.getState();
				DecorationPlacer.this.repaint();
			}
		});
		final Action clearAction = new AbstractAction("Clear all current points.")
		{
			private static final long serialVersionUID = -7217861953409073730L;
			
			public void actionPerformed(final ActionEvent event)
			{
				m_currentImagePoints.clear();
			}
		};
		clearAction.putValue(Action.SHORT_DESCRIPTION, "Delete all points.");
		final JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		editMenu.add(highlightAllModeItem);
		editMenu.add(showNamesModeItem);
		editMenu.addSeparator();
		editMenu.add(clearAction);
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
	private Image createImage(final String mapName)
	{
		final Image image = Toolkit.getDefaultToolkit().createImage(mapName);
		try
		{
			Util.ensureImageLoaded(image);
		} catch (final InterruptedException ex)
		{
			ex.printStackTrace();
		}
		return image;
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
			private static final long serialVersionUID = -7130828419508975924L;
			
			@Override
			public void paint(final Graphics g)
			{
				// super.paint(g);
				paintToG(g);
			}
		};
		return imagePanel;
	}
	
	private void paintToG(final Graphics g)
	{
		if (s_cheapMutex)
			return;
		g.drawImage(m_image, 0, 0, this);
		g.setColor(Color.red);
		for (final Entry<String, Tuple<Image, List<Point>>> entry : m_currentImagePoints.entrySet())
		{
			for (final Point p : entry.getValue().getSecond())
			{
				g.drawImage(entry.getValue().getFirst(), p.x, p.y - (s_showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)), null);
				if (m_currentSelectedImage != null && m_currentSelectedImage.getThird().equals(p))
				{
					g.setColor(Color.green);
					g.drawRect(p.x, p.y - (s_showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)), entry.getValue().getFirst().getWidth(null),
								entry.getValue().getFirst().getHeight(null));
					g.setColor(Color.red);
				}
				else if (s_highlightAll)
				{
					g.drawRect(p.x, p.y - (s_showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)), entry.getValue().getFirst().getWidth(null),
								entry.getValue().getFirst().getHeight(null));
				}
				if (s_showPointNames)
				{
					g.drawString(entry.getKey(), p.x, p.y - (s_showFromTopLeft ? 0 : entry.getValue().getFirst().getHeight(null)));
				}
			}
		}
		if (m_currentSelectedImage != null)
		{
			g.setColor(Color.green);
			g.drawImage(m_currentSelectedImage.getSecond(), m_currentMousePoint.x, m_currentMousePoint.y - (s_showFromTopLeft ? 0 : m_currentSelectedImage.getSecond().getHeight(null)), null);
			if (s_highlightAll)
				g.drawRect(m_currentMousePoint.x, m_currentMousePoint.y - (s_showFromTopLeft ? 0 : m_currentSelectedImage.getSecond().getHeight(null)),
							m_currentSelectedImage.getSecond().getWidth(null), m_currentSelectedImage.getSecond().getHeight(null));
			if (s_showPointNames)
				g.drawString(m_currentSelectedImage.getFirst(), m_currentMousePoint.x, m_currentMousePoint.y - (s_showFromTopLeft ? 0 : m_currentSelectedImage.getSecond().getHeight(null)));
		}
	}
	
	private void saveCurrentToMapPicture()
	{
		final BufferedImage bufferedImage = new BufferedImage(m_image.getWidth(null), m_image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		final Graphics g = bufferedImage.getGraphics();
		final boolean saveHighlight = s_highlightAll;
		final boolean saveNames = s_showPointNames;
		s_highlightAll = false;
		s_showPointNames = false;
		paintToG(g);
		g.dispose();
		s_highlightAll = saveHighlight;
		s_showPointNames = saveNames;
		m_image = bufferedImage;
	}
	
	/**
	 * saveCenters()
	 * 
	 * Saves the centers to disk.
	 */
	private void saveImagePoints()
	{
		m_currentPoints = new HashMap<String, List<Point>>();
		for (final Entry<String, Tuple<Image, List<Point>>> entry : m_currentImagePoints.entrySet())
		{
			// remove duplicates
			final LinkedHashSet<Point> pointSet = new LinkedHashSet<Point>();
			pointSet.addAll(entry.getValue().getSecond());
			entry.getValue().getSecond().clear();
			entry.getValue().getSecond().addAll(pointSet);
			m_currentPoints.put(entry.getKey(), entry.getValue().getSecond());
		}
		try
		{
			final String fileName = new FileSave("Where To Save Image Points Text File?", JFileChooser.FILES_ONLY, s_currentImagePointsTextFile, s_mapFolderLocation).getPathString();
			if (fileName == null)
			{
				return;
			}
			final FileOutputStream out = new FileOutputStream(fileName);
			// if (s_imagePointType.isCanHaveMultiplePoints())
			PointFileReaderWriter.writeOneToMany(out, m_currentPoints);
			// else
			// PointFileReaderWriter.writeOneToOne(out, m_currentPoints);
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
		System.out.println("");
	}
	
	private void selectImagePointType()
	{
		System.out.println("Select Which type of image points file are we making?");
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(new JLabel("Which type of image points file are we making?"));
		final ButtonGroup group = new ButtonGroup();
		for (final ImagePointType type : ImagePointType.getTypes())
		{
			final JRadioButton button = new JRadioButton(type.toString() + "      :      " + type.getDescription());
			button.setActionCommand(type.toString());
			if (s_imagePointType == type)
			{
				button.setSelected(true);
			}
			else
				button.setSelected(false);
			group.add(button);
			panel.add(button);
		}
		JOptionPane.showMessageDialog(this, panel, "Which type of image points file are we making?", JOptionPane.QUESTION_MESSAGE);
		final ButtonModel selected = group.getSelection();
		final String choice = selected.getActionCommand();
		for (final ImagePointType type : ImagePointType.getTypes())
		{
			if (type.toString().equals(choice))
			{
				s_imagePointType = type;
				System.out.println("Selected Type: " + choice);
				break;
			}
		}
	}
	
	private void topLeftOrBottomLeft()
	{
		final Object[] showFromTopLeft = { "Point is Top Left", "Point is Bottom Left" };
		System.out.println("Select Show images from top left or bottom left point?");
		if (JOptionPane.showOptionDialog(this, "Are the images shown from the top left, or from the bottom left point? \r\n"
					+ "All images are shown from the top left, except for 'name_place.txt', 'pu_place.txt', and 'comments.txt'. \r\n"
					+ "For these 3 files, whether they are top left or bottom left is determined by the \r\n"
					+ "'map.properties' property: 'map.drawNamesFromTopLeft', which defaults to false if not specified [meaning bottom left].",
					"Show images from top left or bottom left point?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, showFromTopLeft,
					showFromTopLeft[(s_imagePointType.isCanUseBottomLeftPoint() ? 1 : 0)]) == JOptionPane.NO_OPTION)
			s_showFromTopLeft = false;
		else
			s_showFromTopLeft = true;
	}
	
	private void loadImagesAndPoints()
	{
		s_cheapMutex = true;
		m_currentImagePoints = new HashMap<String, Tuple<Image, List<Point>>>();
		m_currentSelectedImage = null;
		selectImagePointType();
		final Object[] miscOrNamesOptions = { "Folder Full of Images", "Text File Full of Points" };
		final Object[] pointsAreNamesOptions = { "Points end in .png", "Points do NOT end in .png" };
		final Object[] fillAllOptions = { "Fill In All Territories", "Let Me Select Territories" };
		System.out.println("Select Folder full of images OR Text file full of points?");
		if (JOptionPane.showOptionDialog(this, "Are you doing a folder full of different images (decorations.txt [misc] and name_place.txt [territoryNames]) \r\n"
					+ "Or are we doing a per territory static or dynamic image based on game data (pu_place.txt [PUs], capitols.txt [flags], etc, basically all others) ?",
					"Folder full of images OR Text file full of points?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, miscOrNamesOptions,
					miscOrNamesOptions[(s_imagePointType.isUseFolder() ? 0 : 1)]) != JOptionPane.NO_OPTION)
		{
			// points are 'territory name' as opposed to exactly an image file name, like 'territory name.png' (everything but decorations is a territory name, while decorations are image file names)
			loadImageFolder();
			if (s_currentImageFolderLocation == null)
				return;
			loadImagePointTextFile();
			topLeftOrBottomLeft();
			// decorations.txt (misc folder) and name_place.txt (territoryNames folder) use a different image for each point,
			// while everything else (like pu_place.txt (PUs folder)) will use either a static image or a dynamically chosen image based on some in game property in the game data.
			System.out.println("Points end in .png OR they do not?");
			fillCurrentImagePointsBasedOnImageFolder(JOptionPane.showOptionDialog(this, "Does the text file use the exact image file name, including the .png extension (decorations.txt) \r\n"
						+ "Or does the text file not use the full file name with no extension, just a territory name (name_place.txt) ?",
						"Points end in .png OR they do not?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, pointsAreNamesOptions,
						pointsAreNamesOptions[(s_imagePointType.isEndInPNG() ? 0 : 1)]) == JOptionPane.NO_OPTION);
			s_createNewImageOnRightClick = false;
			s_staticImageForPlacing = null;
		}
		else
		{
			loadImagePointTextFile();
			topLeftOrBottomLeft();
			// load all territories? things like pu_place.txt should have all or most territories, while things like blockade.txt and kamikaze_place.txt and capitols.txt will only have a small number of territories
			System.out.println("Select Fill in all territories OR let you select them?");
			fillCurrentImagePointsBasedOnTextFile(JOptionPane.showOptionDialog(this, "Are you going to do a point for every single territory (pu_place.txt) \r\n"
						+ "Or are you going to do just a few territories (capitols.txt, convoy.txt, vc.txt, etc, most others) ? \r\n"
						+ "(If you choose the later option, you must Right Click on a territory to create an image for that territory.)",
						"Fill in all territories OR let you select them?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, fillAllOptions,
						fillAllOptions[(s_imagePointType.isFillAll() ? 0 : 1)]) != JOptionPane.NO_OPTION);
		}
		
		s_cheapMutex = false;
		System.out.println("");
		repaint();
		JOptionPane.showMessageDialog(this, new JLabel(s_imagePointType.getInstructions()));
	}
	
	private void loadImageFolder()
	{
		System.out.println("Load an image folder (eg: 'misc' or 'territoryNames', etc)");
		File folder = new File(s_mapFolderLocation, s_imagePointType.getFolderName());
		if (folder == null || !folder.exists())
			folder = s_mapFolderLocation;
		final FileSave imageFolder = new FileSave("Load an Image Folder", null, folder);
		if (imageFolder == null || imageFolder.getPathString() == null || imageFolder.getFile() == null || !imageFolder.getFile().exists())
		{
			s_currentImageFolderLocation = null;
		}
		else
		{
			s_currentImageFolderLocation = imageFolder.getFile();
		}
	}
	
	private void loadImagePointTextFile()
	{
		try
		{
			System.out.println("Load the points text file (eg: decorations.txt or pu_place.txt, etc)");
			final FileOpen centerName = new FileOpen("Load an Image Points Text File", s_mapFolderLocation, new File(s_mapFolderLocation, s_imagePointType.getFileName()), ".txt");
			s_currentImagePointsTextFile = centerName.getFile();
			if (centerName != null && centerName.getFile() != null && centerName.getFile().exists() && centerName.getPathString() != null)
			{
				final FileInputStream in = new FileInputStream(centerName.getPathString());
				m_currentPoints = PointFileReaderWriter.readOneToMany(in);
			}
			else
			{
				m_currentPoints = new HashMap<String, List<Point>>();
			}
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
	
	private void fillCurrentImagePointsBasedOnTextFile(final boolean fillInAllTerritories)
	{
		s_staticImageForPlacing = null;
		File image = new File(s_mapFolderLocation + File.separator + s_imagePointType.getFolderName(), s_imagePointType.getImageName());
		if (image == null || !image.exists())
			image = new File(GameRunner.getRootFolder() + File.separator + ResourceLoader.RESOURCE_FOLDER + File.separator + s_imagePointType.getFolderName(), s_imagePointType.getImageName());
		if (image == null || !image.exists())
			image = null;
		while (s_staticImageForPlacing == null)
		{
			final FileOpen imageSelection = new FileOpen("Select Example Image To Use", (image == null ? s_mapFolderLocation : new File(image.getParent())), image, ".gif", ".png");
			if (imageSelection == null || imageSelection.getFile() == null || !imageSelection.getFile().exists())
				continue;
			s_staticImageForPlacing = createImage(imageSelection.getPathString());
		}
		final int width = s_staticImageForPlacing.getWidth(null);
		final int height = s_staticImageForPlacing.getHeight(null);
		final int addY = (s_imagePointType == ImagePointType.comments ? ((-ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS))
					: (s_imagePointType == ImagePointType.pu_place ? (ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS) : 0));
		if (fillInAllTerritories)
		{
			for (final Entry<String, Point> entry : m_centers.entrySet())
			{
				List<Point> points = m_currentPoints.get(entry.getKey());
				if (points == null)
				{
					System.out.println("Did NOT find point for: " + entry.getKey());
					points = new ArrayList<Point>();
					final Point p = new Point(entry.getValue().x - (width / 2), entry.getValue().y + addY + ((s_showFromTopLeft ? -1 : 1) * (height / 2)));
					points.add(p);
				}
				else
					System.out.println("Found point for: " + entry.getKey());
				m_currentImagePoints.put(entry.getKey(), new Tuple<Image, List<Point>>(s_staticImageForPlacing, points));
			}
		}
		else
		{
			for (final Entry<String, List<Point>> entry : m_currentPoints.entrySet())
			{
				m_currentImagePoints.put(entry.getKey(), new Tuple<Image, List<Point>>(s_staticImageForPlacing, entry.getValue()));
			}
		}
		s_createNewImageOnRightClick = true; // !fillInAllTerritories;
	}
	
	private void fillCurrentImagePointsBasedOnImageFolder(final boolean pointsAreExactlyTerritoryNames)
	{
		final int addY = (s_imagePointType == ImagePointType.comments ? ((-ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS))
					: (s_imagePointType == ImagePointType.pu_place ? (ImagePointType.SPACE_BETWEEN_NAMES_AND_PUS) : 0));
		final List<String> allTerritories = new ArrayList<String>(m_centers.keySet());
		for (final File file : s_currentImageFolderLocation.listFiles())
		{
			if (!file.getPath().endsWith(".png") && !file.getPath().endsWith(".gif"))
				continue;
			final String imageName = file.getName();
			final String possibleTerritoryName = imageName.substring(0, imageName.length() - 4);
			final Image image = createImage(file.getPath());
			List<Point> points = (m_currentPoints != null ? m_currentPoints.get((pointsAreExactlyTerritoryNames ? possibleTerritoryName : imageName)) : null);
			if (points == null)
			{
				points = new ArrayList<Point>();
				Point p = m_centers.get(possibleTerritoryName);
				if (p == null)
				{
					System.out.println("Did NOT find point for: " + possibleTerritoryName);
					points.add(new Point(50, 50));
				}
				else
				{
					/*
					if (!s_imagePointType.isUsesCentersPoint())
					{
						final Rectangle territoryBounds = MapData.getBoundingRect(possibleTerritoryName, m_polygons);
					}*/
					p = new Point(p.x - (image.getWidth(null) / 2), p.y + addY + ((s_showFromTopLeft ? -1 : 1) * (image.getHeight(null) / 2)));
					points.add(p);
					allTerritories.remove(possibleTerritoryName);
					System.out.println("Found point for: " + possibleTerritoryName);
				}
			}
			else
			{
				allTerritories.remove(possibleTerritoryName);
				
			}
			m_currentImagePoints.put((pointsAreExactlyTerritoryNames ? possibleTerritoryName : imageName), new Tuple<Image, List<Point>>(image, points));
		}
		if (!allTerritories.isEmpty() && s_imagePointType == ImagePointType.name_place)
		{
			JOptionPane.showMessageDialog(this, new JLabel("Territory images not found in folder: " + allTerritories));
			System.out.println(allTerritories);
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
		String seaName = null;
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
	 * @param java
	 *            .awt.Point point a point clicked by mouse
	 * @param java
	 *            .lang.boolean ctrlDown true if ctrl key was hit
	 * @param java
	 *            .lang.boolean rightMouse true if the right mouse button was hit
	 */
	private void mouseEvent(final Point point, final boolean ctrlDown, final boolean rightMouse)
	{
		if (s_cheapMutex)
			return;
		if (!rightMouse && !ctrlDown && m_currentSelectedImage == null)
		{
			// find whatever image we are left clicking on
			Point testPoint = null;
			for (final Entry<String, Tuple<Image, List<Point>>> entry : m_currentImagePoints.entrySet())
			{
				for (final Point p : entry.getValue().getSecond())
				{
					if (testPoint == null || p.distance(m_currentMousePoint) < testPoint.distance(m_currentMousePoint))
					{
						testPoint = p;
						m_currentSelectedImage = new Triple<String, Image, Point>(entry.getKey(), entry.getValue().getFirst(), p);
					}
				}
			}
		}
		else if (!rightMouse && !ctrlDown && m_currentSelectedImage != null)
		{
			// save the image
			final Tuple<Image, List<Point>> imagePoints = m_currentImagePoints.get(m_currentSelectedImage.getFirst());
			final List<Point> points = imagePoints.getSecond();
			points.remove(m_currentSelectedImage.getThird());
			points.add(new Point(m_currentMousePoint));
			m_currentImagePoints.put(new String(m_currentSelectedImage.getFirst()), new Tuple<Image, List<Point>>(m_currentSelectedImage.getSecond(), points));
			m_currentSelectedImage = null;
		}
		else if (rightMouse && !ctrlDown && s_createNewImageOnRightClick && s_staticImageForPlacing != null && m_currentSelectedImage == null)
		{
			// create a new point here in this territory
			final String territoryName = findTerritoryName(m_currentMousePoint);
			if (territoryName != null)
			{
				final List<Point> points = new ArrayList<Point>();
				points.add(new Point(m_currentMousePoint));
				m_currentImagePoints.put(territoryName, new Tuple<Image, List<Point>>(s_staticImageForPlacing, points));
			}
		}
		else if (rightMouse && !ctrlDown && s_imagePointType.isCanHaveMultiplePoints())
		{
			// if none selected find the image we are clicking on, and duplicate it (not replace/move it)
			if (m_currentSelectedImage == null)
			{
				Point testPoint = null;
				for (final Entry<String, Tuple<Image, List<Point>>> entry : m_currentImagePoints.entrySet())
				{
					for (final Point p : entry.getValue().getSecond())
					{
						if (testPoint == null || p.distance(m_currentMousePoint) < testPoint.distance(m_currentMousePoint))
						{
							testPoint = p;
							m_currentSelectedImage = new Triple<String, Image, Point>(entry.getKey(), entry.getValue().getFirst(), null);
						}
					}
				}
			}
			else
			{
				m_currentSelectedImage = new Triple<String, Image, Point>(m_currentSelectedImage.getFirst(), m_currentSelectedImage.getSecond(), null);
			}
			// then save (same code as above for saving)
			final Tuple<Image, List<Point>> imagePoints = m_currentImagePoints.get(m_currentSelectedImage.getFirst());
			final List<Point> points = imagePoints.getSecond();
			points.remove(m_currentSelectedImage.getThird());
			points.add(new Point(m_currentMousePoint));
			m_currentImagePoints.put(new String(m_currentSelectedImage.getFirst()), new Tuple<Image, List<Point>>(m_currentSelectedImage.getSecond(), points));
			m_currentSelectedImage = null;
		}
		else if (rightMouse && ctrlDown)
		{
			// must be right click AND ctrl down to delete an image
			if (m_currentSelectedImage == null)
				return;
			final Tuple<Image, List<Point>> current = m_currentImagePoints.get(m_currentSelectedImage.getFirst());
			final List<Point> points = current.getSecond();
			points.remove(m_currentSelectedImage.getThird());
			if (points.isEmpty())
				m_currentImagePoints.remove(m_currentSelectedImage.getFirst());
			m_currentSelectedImage = null;
		}
		repaint();
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
		// arg can only be the map folder location.
		if (args.length == 1)
		{
			String value;
			if (args[0].startsWith(TRIPLEA_MAP_FOLDER))
			{
				value = getValue(args[0]);
			}
			else
			{
				value = args[0];
			}
			final File mapFolder = new File(value);
			if (mapFolder.exists())
				s_mapFolderLocation = mapFolder;
			else
				System.out.println("Could not find directory: " + value);
		}
		else if (args.length > 1)
		{
			System.out.println("Only argument allowed is the map directory.");
		}
		// might be set by -D
		if (s_mapFolderLocation == null || s_mapFolderLocation.length() < 1)
		{
			String value = System.getProperty(TRIPLEA_MAP_FOLDER);
			if (value != null && value.length() > 0)
			{
				value = value.replaceAll("\\(", " ");
				final File mapFolder = new File(value);
				if (mapFolder.exists())
					s_mapFolderLocation = mapFolder;
				else
					System.out.println("Could not find directory: " + value);
			}
		}
	}
}


enum ImagePointType
{
	decorations("decorations.txt", "misc", null, true, true, true, false, true, true, "decorations.txt will place any kind of image you want anywhere, using the 'misc' folder",
				"<html>decorations.txt will allow for multiple points per image. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = create a copy of currently selected image OR closest image <br>CTRL/SHIFT + Right Click = delete currently selected image point</html>"),
	name_place("name_place.txt", "territoryNames", null, true, false, true, true, false, false,
				"name_place.txt only places images with the exact name of the territories on map, using the 'territoryNames' folder",
				"<html>name_place.txt only allows 1 point per image/territory. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = nothing <br>CTRL/SHIFT + Right Click = delete currently selected image point</html>"),

	pu_place("pu_place.txt", "PUs", "2.png", false, false, true, true, false, false, "pu_place.txt is the point where the PUs get shown, and picks the PU images (like '2.png') from the 'PUs' folder",
				"<html>pu_place.txt only allows 1 point per image/territory. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = create an image and point for this territory if none exists yet <br>CTRL/SHIFT + Right Click = delete currently selected image point</html>"),
	capitols("capitols.txt", "flags", "Neutral_large.png", false, false, false, false, false, true,
				"capitols.txt is the point where a capitol flag is shown, and picks the <name>_large.png image from the 'flags' folder",
				"<html>pu_place.txt only allows 1 point per image/territory. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = create an image and point for this territory if none exists yet <br>CTRL/SHIFT + Right Click = delete currently selected image point</html>"),
	vc("vc.txt", "misc", "vc.png", false, false, false, false, false, true, "vc.txt is the point where a Victory City icon is shown, and picks the 'vc.png' image from the 'misc' folder",
				"<html>pu_place.txt only allows 1 point per image/territory. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = create an image and point for this territory if none exists yet <br>CTRL/SHIFT + Right Click = delete currently selected image point</html>"),
	blockade("blockade.txt", "misc", "blockade.png", false, false, false, false, false, true,
				"blockade.txt is the point where a blockade zone icon is shown, and picks the 'blockade.png' image from the 'misc' folder",
				"<html>pu_place.txt only allows 1 point per image/territory. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = create an image and point for this territory if none exists yet <br>CTRL/SHIFT + Right Click = delete currently selected image point</html>"),
	convoy("convoy.txt", "flags", "Neutral.png", false, false, false, false, false, true,
				"convoy.txt is the point where a nation flag is shown on any sea zone that has production ability, and picks the <name>.png image from the 'flags' folder",
				"<html>pu_place.txt only allows 1 point per image/territory. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = create an image and point for this territory if none exists yet <br>CTRL/SHIFT + Right Click = delete currently selected image point</html>"),
	comments("comments.txt", "misc", "exampleConvoyText.png", false, false, false, true, false, false,
				"comments.txt is the point where text details about a convoy zone or route is shown, and it does not use any image, instead it writes the text in-game",
				"<html>pu_place.txt only allows 1 point per image/territory. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = create an image and point for this territory if none exists yet <br>CTRL/SHIFT + Right Click = delete currently selected image point</html>"),
	kamikaze_place("kamikaze_place.txt", "flags", "Neutral_fade.png", false, false, false, false, false, true,
				"kamikaze_place.txt is the point where a kamikaze zone symbol is shown, and it picks the <name>_fade.png image from the 'flags' folder",
				"<html>pu_place.txt only allows 1 point per image/territory. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = create an image and point for this territory if none exists yet <br>CTRL/SHIFT + Right Click = delete currently selected image point</html>"),
	territory_effects("territory_effects.txt", "territoryEffects", "mountain.png", false, false, false, false, true, true,
				"territory_effects.txt is the point where a territory effect image is shown, and it picks the <effect>.png image from the 'territoryEffects' folder",
				"<html>pu_place.txt will allow for multiple points per image. <br>Left Click = select closest image  OR  place currently selected image " +
							"<br>Right click = copy selected image OR create an image for this territory<br>CTRL/SHIFT + Right Click = delete currently selected image point</html>");
	
	public static final int SPACE_BETWEEN_NAMES_AND_PUS = 32;
	private final String m_fileName;
	private final String m_folderName;
	private final String m_imageName;
	private final boolean m_useFolder;
	private final boolean m_endInPNG;
	private final boolean m_fillAll;
	private final boolean m_canUseBottomLeftPoint;
	private final boolean m_canHaveMultiplePoints;
	private final boolean m_usesCentersPoint;
	private final String m_description;
	private final String m_instructions;
	
	public static ImagePointType[] getTypes()
	{
		return new ImagePointType[] { decorations, name_place, pu_place, capitols, vc, blockade, convoy, comments, kamikaze_place, territory_effects };
	}
	
	ImagePointType(final String fileName, final String folderName, final String imageName, final boolean useFolder, final boolean endInPNG, final boolean fillAll, final boolean canUseBottomLeftPoint,
				final boolean canHaveMultiplePoints, final boolean usesCentersPoint, final String description, final String instructions)
	{
		m_fileName = fileName;
		m_folderName = folderName;
		m_imageName = imageName;
		m_useFolder = useFolder;
		m_endInPNG = endInPNG;
		m_fillAll = fillAll;
		m_canUseBottomLeftPoint = canUseBottomLeftPoint;
		m_canHaveMultiplePoints = canHaveMultiplePoints;
		m_usesCentersPoint = usesCentersPoint;
		m_description = description;
		m_instructions = instructions;
	}
	
	public String getFileName()
	{
		return m_fileName;
	}
	
	public String getFolderName()
	{
		return m_folderName;
	}
	
	public String getImageName()
	{
		return m_imageName;
	}
	
	public boolean isUseFolder()
	{
		return m_useFolder;
	}
	
	public boolean isEndInPNG()
	{
		return m_endInPNG;
	}
	
	public boolean isFillAll()
	{
		return m_fillAll;
	}
	
	public boolean isCanUseBottomLeftPoint()
	{
		return m_canUseBottomLeftPoint;
	}
	
	public boolean isCanHaveMultiplePoints()
	{
		return m_canHaveMultiplePoints;
	}
	
	public boolean isUsesCentersPoint()
	{
		return m_usesCentersPoint;
	}
	
	public String getDescription()
	{
		return m_description;
	}
	
	public String getInstructions()
	{
		return m_instructions;
	}
}
