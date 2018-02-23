package tools.map.making;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;

import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.ui.IntTextField;
import games.strategy.ui.SwingAction;
import games.strategy.util.Tuple;
import tools.image.FileOpen;
import tools.image.FileSave;
import tools.util.ToolLogger;

/**
 * This is the MapPropertiesMaker, it will create a map.properties file for you. <br>
 * The map.properties is located in the map's directory, and it will tell TripleA various
 * display related information about your map. <br>
 * Such things as the dimensions of your map, the colors of each of the players,
 * the size of the unit images, and how zoomed out they are, etc. <br>
 * To use, just fill in the information in the fields below, and click on 'Show More' to
 * show other, optional, fields.
 */
public class MapPropertiesMaker extends JFrame {
  private static final long serialVersionUID = 8182821091131994702L;
  private static File mapFolderLocation = null;
  private static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
  private static final String TRIPLEA_UNIT_ZOOM = "triplea.unit.zoom";
  private static final String TRIPLEA_UNIT_WIDTH = "triplea.unit.width";
  private static final String TRIPLEA_UNIT_HEIGHT = "triplea.unit.height";
  private static final MapProperties mapProperties = new MapProperties();
  private static final JPanel playerColorChooser = new JPanel();

  public static String[] getProperties() {
    return new String[] {TRIPLEA_MAP_FOLDER, TRIPLEA_UNIT_ZOOM, TRIPLEA_UNIT_WIDTH, TRIPLEA_UNIT_HEIGHT};
  }

  /**
   * Entry point for the Map Properties Maker tool.
   */
  public static void main(final String[] args) {
    handleCommandLineArgs(args);
    // JOptionPane.showMessageDialog(null, new JLabel("<html>" + "This is the MapPropertiesMaker, it will create a
    // map.properties file for
    // you. " + "</html>"));
    if (mapFolderLocation == null) {
      ToolLogger.info("Select the map folder");
      final String path = new FileSave("Where is your map's folder?", null, mapFolderLocation).getPathString();
      if (path != null) {
        final File mapFolder = new File(path);
        if (mapFolder.exists()) {
          mapFolderLocation = mapFolder;
          System.setProperty(TRIPLEA_MAP_FOLDER, mapFolderLocation.getPath());
        }
      }
    }
    if (mapFolderLocation != null) {
      final MapPropertiesMaker maker = new MapPropertiesMaker();
      maker.setSize(800, 800);
      maker.setLocationRelativeTo(null);
      maker.setVisible(true);
    } else {
      ToolLogger.info("No Map Folder Selected. Shutting down.");
      System.exit(0);
    }
  } // end main

  private MapPropertiesMaker() {
    super("Map Properties Maker");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.getContentPane().setLayout(new BorderLayout());
    final JPanel panel = createPropertiesPanel();
    this.getContentPane().add(new JScrollPane(panel), BorderLayout.CENTER);
    // set up the actions
    final Action openAction = SwingAction.of("Load Properties", e -> loadProperties());
    openAction.putValue(Action.SHORT_DESCRIPTION, "Load An Existing Properties File");
    final Action saveAction = SwingAction.of("Save Properties", e -> saveProperties());
    saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Properties To File");
    final Action exitAction = SwingAction.of("Exit", e -> System.exit(0));
    exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
    // set up the menu items
    final JMenuItem openItem = new JMenuItem(openAction);
    openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
    final JMenuItem saveItem = new JMenuItem(saveAction);
    saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
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

  private JPanel createPropertiesPanel() {
    final JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    int row = 0;
    panel.add(
        new JLabel("<html>" + "This is the MapPropertiesMaker, it will create a map.properties file for you. "
            + "<br>The map.properties is located in the map's directory, and it will tell TripleA various "
            + "<br>display related information about your map. "
            + "<br>Such things as the dimensions of your map, the colors of each of the players, "
            + "<br>the size of the unit images, and how zoomed out they are, etc. "
            + "<br>To use, just fill in the information in the fields below, and click on 'Show More' to "
            + "<br>show other, optional, fields. " + "</html>"),
        new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
            new Insets(20, 20, 20, 20), 0, 0));
    panel.add(new JLabel("The Width in Pixels of your map: "), new GridBagConstraints(0, row, 1, 1, 1, 1,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    final IntTextField widthField = new IntTextField(0, Integer.MAX_VALUE);
    widthField.setText("" + mapProperties.getMapWidth());
    widthField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {}

      @Override
      public void focusLost(final FocusEvent e) {
        try {
          mapProperties.setMapWidth(Integer.parseInt(widthField.getText()));
        } catch (final Exception ex) {
          // ignore malformed input
        }
        widthField.setText("" + mapProperties.getMapWidth());
      }
    });
    panel.add(widthField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 10, 10, 10), 0, 0));
    panel.add(new JLabel("The Height in Pixels of your map: "), new GridBagConstraints(0, row, 1, 1, 1, 1,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    final IntTextField heightField = new IntTextField(0, Integer.MAX_VALUE);
    heightField.setText("" + mapProperties.getMapHeight());
    heightField.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {}

      @Override
      public void focusLost(final FocusEvent e) {
        try {
          mapProperties.setMapHeight(Integer.parseInt(heightField.getText()));
        } catch (final Exception ex) {
          // ignore malformed input
        }
        heightField.setText("" + mapProperties.getMapHeight());
      }
    });
    panel.add(heightField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    panel.add(
        new JLabel("<html>The initial Scale (zoom) of your unit images: "
            + "<br>Must be one of: 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5</html>"),
        new GridBagConstraints(0, row, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(10, 10, 10, 10), 0, 0));
    final JSpinner scaleField = new JSpinner(new SpinnerNumberModel(0.1, 0.1, 2.0, 1));
    scaleField.setValue(mapProperties.getUnitsScale());
    scaleField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(final FocusEvent e) {
        mapProperties.setUnitsScale((double) scaleField.getValue());
        scaleField.setValue(mapProperties.getUnitsScale());
      }
    });
    panel.add(scaleField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(10, 10, 10, 10), 0, 0));
    panel.add(new JLabel("Create Players and Click on the Color to set their Color: "), new GridBagConstraints(0, row++,
        2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(20, 50, 20, 50), 0, 0));
    createPlayerColorChooser();
    panel.add(playerColorChooser, new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    final JButton showMore = new JButton("Show All Options");
    showMore.addActionListener(SwingAction.of("Show All Options", e -> {
      final Tuple<PropertiesUi, List<MapPropertyWrapper<?>>> propertyWrapperUi =
          MapPropertiesMaker.mapProperties.propertyWrapperUi(true);
      JOptionPane.showMessageDialog(MapPropertiesMaker.this, propertyWrapperUi.getFirst());
      mapProperties.writePropertiesToObject(propertyWrapperUi.getSecond());
      MapPropertiesMaker.this.createPlayerColorChooser();
      MapPropertiesMaker.this.validate();
      MapPropertiesMaker.this.repaint();
    }));
    panel.add(showMore, new GridBagConstraints(0, row++, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 10, 10, 10), 0, 0));
    return panel;
  }

  private void createPlayerColorChooser() {
    playerColorChooser.removeAll();
    playerColorChooser.setLayout(new GridBagLayout());
    int row = 0;
    for (final Entry<String, Color> entry : mapProperties.getColorMap().entrySet()) {
      playerColorChooser.add(new JLabel(entry.getKey()), new GridBagConstraints(0, row, 1, 1, 1, 1,
          GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
      final JLabel label = new JLabel(entry.getKey()) {
        private static final long serialVersionUID = 5624227155029721033L;

        @Override
        public void paintComponent(final Graphics g) {
          final Graphics2D g2 = (Graphics2D) g;
          g2.setColor(entry.getValue());
          g2.fill(g2.getClip());
        }
      };
      label.setBackground(entry.getValue());
      label.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
          ToolLogger.info(label.getBackground().toString());
          final Color color = JColorChooser.showDialog(label, "Choose color", label.getBackground());
          mapProperties.getColorMap().put(label.getText(), color);
          MapPropertiesMaker.this.createPlayerColorChooser();
          MapPropertiesMaker.this.validate();
          MapPropertiesMaker.this.repaint();
        }
      });
      playerColorChooser.add(label, new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
      final JButton removePlayer = new JButton("Remove " + entry.getKey());
      removePlayer.addActionListener(new AbstractAction("Remove " + entry.getKey()) {
        private static final long serialVersionUID = -3593575469168341735L;

        @Override
        public void actionPerformed(final ActionEvent e) {
          mapProperties.getColorMap().remove(removePlayer.getText().replaceFirst("Remove ", ""));
          MapPropertiesMaker.this.createPlayerColorChooser();
          MapPropertiesMaker.this.validate();
          MapPropertiesMaker.this.repaint();
        }
      });
      playerColorChooser.add(removePlayer, new GridBagConstraints(2, row, 1, 1, 1, 1, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
      row++;
    }
    final JTextField nameTextField = new JTextField("Player" + (mapProperties.getColorMap().size() + 1));
    final Dimension ourMinimum = new Dimension(150, 30);
    nameTextField.setMinimumSize(ourMinimum);
    nameTextField.setPreferredSize(ourMinimum);
    final JButton addPlayer = new JButton("Add Another Player");
    addPlayer.addActionListener(SwingAction.of("Add Another Player", e -> {
      mapProperties.getColorMap().put(nameTextField.getText(), Color.GREEN);
      MapPropertiesMaker.this.createPlayerColorChooser();
      MapPropertiesMaker.this.validate();
      MapPropertiesMaker.this.repaint();

    }));
    playerColorChooser.add(addPlayer, new GridBagConstraints(0, row, 1, 1, 1, 1, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
    playerColorChooser.add(nameTextField, new GridBagConstraints(1, row++, 1, 1, 1, 1, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
  }

  private void loadProperties() {
    ToolLogger.info("Load a properties file");
    final String centerName =
        new FileOpen("Load A Properties File", mapFolderLocation, ".properties").getPathString();
    if (centerName == null) {
      return;
    }
    try (InputStream in = new FileInputStream(centerName)) {
      final Properties properties = new Properties();
      properties.load(in);
    } catch (final IOException e) {
      ToolLogger.error("Failed to load map properties: " + centerName, e);
    }
    for (final Method setter : mapProperties.getClass().getMethods()) {
      final boolean startsWithSet = setter.getName().startsWith("set");
      if (!startsWithSet) {
        continue;
      }

      // TODO: finish this
    }
    validate();
    repaint();
  }

  private static void saveProperties() {
    try {
      final String fileName =
          new FileSave("Where To Save map.properties ?", "map.properties", mapFolderLocation).getPathString();
      if (fileName == null) {
        return;
      }
      final String stringToWrite = getOutPutString();
      try (OutputStream sink = new FileOutputStream(fileName);
          Writer out = new OutputStreamWriter(sink, StandardCharsets.UTF_8)) {
        out.write(stringToWrite);
      }
      ToolLogger.info("Data written to :" + new File(fileName).getCanonicalPath());
      ToolLogger.info(stringToWrite);
    } catch (final Exception e) {
      ToolLogger.error("Failed to save map properties", e);
    }
  }

  private static String getOutPutString() {
    final StringBuilder outString = new StringBuilder();
    for (final Method outMethod : mapProperties.getClass().getMethods()) {
      final boolean startsWithSet = outMethod.getName().startsWith("out");
      if (!startsWithSet) {
        continue;
      }
      try {
        outString.append(outMethod.invoke(mapProperties));
      } catch (final IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
        ToolLogger.error("Failed to invoke method reflectively: " + outMethod.getName(), e);
      }
    }
    return outString.toString();
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }

  private static void handleCommandLineArgs(final String[] args) {
    final String[] properties = getProperties();
    boolean usagePrinted = false;
    for (final String arg2 : args) {
      boolean found = false;
      String arg = arg2;
      final int indexOf = arg.indexOf('=');
      if (indexOf > 0) {
        arg = arg.substring(0, indexOf);
        for (final String propertie : properties) {
          if (arg.equals(propertie)) {
            final String value = getValue(arg2);
            System.setProperty(propertie, value);
            ToolLogger.info(propertie + ":" + value);
            found = true;
            break;
          }
        }
      }
      if (!found) {
        ToolLogger.info("Unrecogized:" + arg2);
        if (!usagePrinted) {
          usagePrinted = true;
          ToolLogger.info("Arguments\r\n" + "   " + TRIPLEA_MAP_FOLDER + "=<FILE_PATH>\r\n" + "   "
              + TRIPLEA_UNIT_ZOOM + "=<UNIT_ZOOM_LEVEL>\r\n" + "   " + TRIPLEA_UNIT_WIDTH + "=<UNIT_WIDTH>\r\n" + "   "
              + TRIPLEA_UNIT_HEIGHT + "=<UNIT_HEIGHT>\r\n");
        }
      }
    }
    // now account for anything set by -D
    final String folderString = System.getProperty(TRIPLEA_MAP_FOLDER);
    if ((folderString != null) && (folderString.length() > 0)) {
      final File mapFolder = new File(folderString);
      if (mapFolder.exists()) {
        mapFolderLocation = mapFolder;
      } else {
        ToolLogger.info("Could not find directory: " + folderString);
      }
    }
    final String zoomString = System.getProperty(TRIPLEA_UNIT_ZOOM);
    if ((zoomString != null) && (zoomString.length() > 0)) {
      try {
        mapProperties.setUnitsScale(Double.parseDouble(zoomString));
        ToolLogger.info("Unit Zoom Percent to use: " + zoomString);
      } catch (final Exception e) {
        ToolLogger.error("Not a decimal percentage: " + zoomString);
      }
    }
    final String widthString = System.getProperty(TRIPLEA_UNIT_WIDTH);
    if ((widthString != null) && (widthString.length() > 0)) {
      try {
        final int unitWidth = Integer.parseInt(widthString);
        mapProperties.setUnitsWidth(unitWidth);
        mapProperties.setUnitsCounterOffsetWidth(unitWidth / 4);
        ToolLogger.info("Unit Width to use: " + unitWidth);
      } catch (final Exception e) {
        ToolLogger.error("Not an integer: " + widthString);
      }
    }
    final String heightString = System.getProperty(TRIPLEA_UNIT_HEIGHT);
    if ((heightString != null) && (heightString.length() > 0)) {
      try {
        final int unitHeight = Integer.parseInt(heightString);
        mapProperties.setUnitsHeight(unitHeight);
        mapProperties.setUnitsCounterOffsetHeight(unitHeight);
        ToolLogger.info("Unit Height to use: " + unitHeight);
      } catch (final Exception e) {
        ToolLogger.error("Not an integer: " + heightString);
      }
    }
  }
}
