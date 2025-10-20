package tools.map.making;

import games.strategy.engine.data.properties.PropertiesUi;
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
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
import lombok.extern.slf4j.Slf4j;
import org.triplea.swing.IntTextField;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.util.Tuple;
import tools.image.FileSave;
import tools.util.ToolArguments;
import tools.util.ToolRunnableTask;

/**
 * This is the MapPropertiesMaker, it will create a map.properties file for you. <br>
 * The map.properties is located in the map's directory, and it will tell TripleA various display
 * related information about your map. <br>
 * Such things as the dimensions of your map, the colors of each of the players, the size of the
 * unit images, and how zoomed out they are, etc. <br>
 * To use, just fill in the information in the fields below, and click on 'Show More' to show other,
 * optional, fields.
 */
@Slf4j
public final class MapPropertiesMaker extends ToolRunnableTask {
  private Path mapFolderLocation = null;
  private final MapProperties mapProperties = new MapProperties();

  private MapPropertiesMaker() {}

  public static void run() {
    runTask(MapPropertiesMaker.class);
  }

  @Override
  protected void runInternal() {
    handleSystemProperties();
    if (mapFolderLocation == null) {
      log.info("Select the map folder");
      final Path mapFolder =
          new FileSave("Where is your map's folder?", null, mapFolderLocation).getFile();
      if (mapFolder != null && Files.exists(mapFolder)) {
        mapFolderLocation = mapFolder;
        ToolArguments.MAP_FOLDER.setSystemProperty(mapFolderLocation.toString());
      }
    }
    if (mapFolderLocation != null) {
      final MapPropertiesMakerFrame frame = new MapPropertiesMakerFrame();
      frame.setSize(800, 800);
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    } else {
      log.info("No Map Folder Selected. Shutting down.");
    }
  }

  private final class MapPropertiesMakerFrame extends JFrame {
    private static final long serialVersionUID = 8182821091131994702L;

    private final JPanel playerColorChooser = new JPanel();

    MapPropertiesMakerFrame() {
      super("Map Properties Maker");
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      this.getContentPane().setLayout(new BorderLayout());
      final JPanel panel = newPropertiesPanel();
      this.getContentPane().add(new JScrollPane(panel), BorderLayout.CENTER);
      // set up the actions
      final Action saveAction = SwingAction.of("Save Properties", e -> saveProperties());
      saveAction.putValue(Action.SHORT_DESCRIPTION, "Save The Properties To File");
      final Action exitAction =
          SwingAction.of(
              "Exit",
              e -> {
                setVisible(false);
                dispose();
              });
      exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
      // set up the menu items
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

    private JPanel newPropertiesPanel() {
      final JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      int row = 0;
      panel.add(
          new JLabel(
              "<html>"
                  + "This is the MapPropertiesMaker, it will create a map.properties file for you. "
                  + "<br>The map.properties is located in the map's directory, and it will tell "
                  + "TripleA various "
                  + "<br>display related information about your map. "
                  + "<br>Such things as the dimensions of your map, the colors of each of "
                  + "the players, "
                  + "<br>the size of the unit images, and how zoomed out they are, etc. "
                  + "<br>To use, just fill in the information in the fields below, and click on "
                  + "'Show More' to "
                  + "<br>show other, optional, fields. "
                  + "</html>"),
          new GridBagConstraints(
              0,
              row++,
              2,
              1,
              1,
              1,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              new Insets(20, 20, 20, 20),
              0,
              0));
      panel.add(
          new JLabel("The Width in Pixels of your map: "),
          new GridBagConstraints(
              0,
              row,
              1,
              1,
              1,
              1,
              GridBagConstraints.EAST,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
      final IntTextField widthField = new IntTextField(0, Integer.MAX_VALUE);
      widthField.setText("" + mapProperties.getMapWidth());
      widthField.addFocusListener(
          new FocusListener() {
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
      panel.add(
          widthField,
          new GridBagConstraints(
              1,
              row++,
              1,
              1,
              1,
              1,
              GridBagConstraints.WEST,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
      panel.add(
          new JLabel("The Height in Pixels of your map: "),
          new GridBagConstraints(
              0,
              row,
              1,
              1,
              1,
              1,
              GridBagConstraints.EAST,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
      final IntTextField heightField = new IntTextField(0, Integer.MAX_VALUE);
      heightField.setText("" + mapProperties.getMapHeight());
      heightField.addFocusListener(
          new FocusListener() {
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
      panel.add(
          heightField,
          new GridBagConstraints(
              1,
              row++,
              1,
              1,
              1,
              1,
              GridBagConstraints.WEST,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
      panel.add(
          new JLabel(
              "<html>The initial Scale (zoom) of your unit images: "
                  + "<br>Must be one of: 1.25, 1, 0.875, 0.8333, 0.75, 0.6666, 0.5625, 0.5</html>"),
          new GridBagConstraints(
              0,
              row,
              1,
              1,
              1,
              1,
              GridBagConstraints.EAST,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
      final JSpinner scaleField = new JSpinner(new SpinnerNumberModel(0.1, 0.1, 2.0, 1));
      scaleField.setValue(mapProperties.getUnitsScale());
      scaleField.addFocusListener(
          new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
              mapProperties.setUnitsScale((double) scaleField.getValue());
              scaleField.setValue(mapProperties.getUnitsScale());
            }
          });
      panel.add(
          scaleField,
          new GridBagConstraints(
              1,
              row++,
              1,
              1,
              1,
              1,
              GridBagConstraints.WEST,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
      panel.add(
          new JLabel("Create Players and Click on the Color to set their Color: "),
          new GridBagConstraints(
              0,
              row++,
              2,
              1,
              1,
              1,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              new Insets(20, 50, 20, 50),
              0,
              0));
      createPlayerColorChooser();
      panel.add(
          playerColorChooser,
          new GridBagConstraints(
              0,
              row++,
              2,
              1,
              1,
              1,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
      final JButton showMore = new JButton("Show All Options");
      showMore.addActionListener(
          SwingAction.of(
              "Show All Options",
              e -> {
                final Tuple<PropertiesUi, List<MapPropertyWrapper<?>>> propertyWrapperUi =
                    mapProperties.propertyWrapperUi(true);
                JOptionPane.showMessageDialog(this, propertyWrapperUi.getFirst());
                mapProperties.writePropertiesToObject(propertyWrapperUi.getSecond());
                createPlayerColorChooser();
                SwingComponents.redraw(this);
              }));
      panel.add(
          showMore,
          new GridBagConstraints(
              0,
              row,
              2,
              1,
              1,
              1,
              GridBagConstraints.CENTER,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
      return panel;
    }

    private void createPlayerColorChooser() {
      playerColorChooser.removeAll();
      playerColorChooser.setLayout(new GridBagLayout());
      int row = 0;
      for (final Entry<String, Color> entry : mapProperties.getColorMap().entrySet()) {
        playerColorChooser.add(
            new JLabel(entry.getKey()),
            new GridBagConstraints(
                0,
                row,
                1,
                1,
                1,
                1,
                GridBagConstraints.EAST,
                GridBagConstraints.NONE,
                new Insets(10, 10, 10, 10),
                0,
                0));
        final JLabel label =
            new JLabel(entry.getKey()) {
              private static final long serialVersionUID = 5624227155029721033L;

              @Override
              public void paintComponent(final Graphics g) {
                final Graphics2D g2 = (Graphics2D) g;
                g2.setColor(entry.getValue());
                g2.fill(g2.getClip());
              }
            };
        label.setBackground(entry.getValue());
        label.addMouseListener(
            new MouseAdapter() {
              @Override
              public void mouseClicked(final MouseEvent e) {
                log.info(label.getBackground().toString());
                final Color color =
                    JColorChooser.showDialog(label, "Choose color", label.getBackground());
                mapProperties.getColorMap().put(label.getText(), color);
                createPlayerColorChooser();
                validate();
                repaint();
              }
            });
        playerColorChooser.add(
            label,
            new GridBagConstraints(
                1,
                row,
                1,
                1,
                1,
                1,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(10, 10, 10, 10),
                0,
                0));
        final JButton removePlayer = new JButton("Remove " + entry.getKey());
        removePlayer.addActionListener(
            new AbstractAction("Remove " + entry.getKey()) {
              private static final long serialVersionUID = -3593575469168341735L;

              @Override
              public void actionPerformed(final ActionEvent e) {
                mapProperties
                    .getColorMap()
                    .remove(removePlayer.getText().replaceFirst("Remove ", ""));
                createPlayerColorChooser();
                validate();
                repaint();
              }
            });
        playerColorChooser.add(
            removePlayer,
            new GridBagConstraints(
                2,
                row,
                1,
                1,
                1,
                1,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(10, 10, 10, 10),
                0,
                0));
        row++;
      }
      final JTextField nameTextField =
          new JTextField("Player" + (mapProperties.getColorMap().size() + 1));
      final Dimension ourMinimum = new Dimension(150, 30);
      nameTextField.setMinimumSize(ourMinimum);
      nameTextField.setPreferredSize(ourMinimum);
      final JButton addPlayer = new JButton("Add Another Player");
      addPlayer.addActionListener(
          SwingAction.of(
              "Add Another Player",
              e -> {
                mapProperties.getColorMap().put(nameTextField.getText(), Color.GREEN);
                createPlayerColorChooser();
                validate();
                repaint();
              }));
      playerColorChooser.add(
          addPlayer,
          new GridBagConstraints(
              0,
              row,
              1,
              1,
              1,
              1,
              GridBagConstraints.EAST,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
      playerColorChooser.add(
          nameTextField,
          new GridBagConstraints(
              1,
              row,
              1,
              1,
              1,
              1,
              GridBagConstraints.WEST,
              GridBagConstraints.NONE,
              new Insets(10, 10, 10, 10),
              0,
              0));
    }

    private void saveProperties() {
      try {
        final Path fileName =
            new FileSave("Where To Save map.properties ?", "map.properties", mapFolderLocation)
                .getFile();
        if (fileName == null) {
          return;
        }
        final String stringToWrite = getOutputString();
        try (OutputStream sink = Files.newOutputStream(fileName);
            Writer out = new OutputStreamWriter(sink, StandardCharsets.UTF_8)) {
          out.write(stringToWrite);
        }
        log.info("Data written to: {}", fileName.normalize().toAbsolutePath());
        log.info(stringToWrite);
      } catch (final Exception e) {
        log.error("Failed to save map properties", e);
      }
    }

    private String getOutputString() {
      final StringBuilder outString = new StringBuilder();
      for (final Method outMethod : mapProperties.getClass().getMethods()) {
        final boolean startsWithSet = outMethod.getName().startsWith("out");
        if (!startsWithSet) {
          continue;
        }
        try {
          outString.append(outMethod.invoke(mapProperties));
        } catch (final IllegalArgumentException
            | InvocationTargetException
            | IllegalAccessException e) {
          log.error("Failed to invoke method reflectively: " + outMethod.getName(), e);
        }
      }
      return outString.toString();
    }
  }

  private void handleSystemProperties() {
    ToolArguments.ifMapFolder(mapFolderProperty -> mapFolderLocation = mapFolderProperty);
    ToolArguments.ifUnitZoom(mapProperties::setUnitsScale);
    ToolArguments.ifUnitWidth(
        unitWidthProperty -> {
          mapProperties.setUnitsWidth(unitWidthProperty);
          mapProperties.setUnitsCounterOffsetWidth(unitWidthProperty / 4);
        });
    ToolArguments.ifUnitHeight(
        unitHeightProperty -> {
          mapProperties.setUnitsHeight(unitHeightProperty);
          mapProperties.setUnitsCounterOffsetHeight(unitHeightProperty);
        });
  }
}
