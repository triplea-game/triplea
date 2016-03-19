package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import util.image.FileOpen;


public class MapPropertiesPanel {

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {

    if (!MapXMLHelper.xmlStrings.containsKey("info_@name")) {
      for (final String newKey : new String[] {"info_@name", "info_@version"}) {
        MapXMLHelper.putXmlStrings(newKey, "");
      }
    }

    if (MapXMLHelper.resourceList.isEmpty())
      MapXMLHelper.addResourceList("");

    final JTextField tMapName = new JTextField(MapXMLHelper.xmlStrings.get("info_@name"));
    final JTextField tMapVersion = new JTextField(MapXMLHelper.xmlStrings.get("info_@version"));
    final JTextField tResourceName = new JTextField(MapXMLHelper.resourceList.get(0));
    final JTextField tMapImageFile =
        new JTextField((MapXMLCreator.mapImageFile == null ? "" : MapXMLCreator.mapImageFile.getAbsolutePath()));
    final JTextField tCentersFile = new JTextField(
        (MapXMLCreator.mapCentersFile == null ? "" : MapXMLCreator.mapCentersFile.getAbsolutePath()));
    final JTextField tWaterFilter = new JTextField(MapXMLCreator.waterFilterString);
    final JLabel labelMapName = new JLabel("Map Name:");
    final JLabel labelMapNameExample = new JLabel("e.g. 'Revised'");
    final JLabel labelMapVersion = new JLabel("Map Version:");
    final JLabel labelMapVersionExample = new JLabel("e.g. '1.2.0.1'");
    final JLabel labelResourceName = new JLabel("Resource Name:");
    final JLabel labelResourceNameExample = new JLabel("e.g. 'PUs'");
    final JLabel labelMapImageFile = new JLabel("Map Image File:");
    final JButton buttonSelectMapImageFile = new JButton("Browse");
    final JLabel labelCentersFile = new JLabel("Map Centers File:");
    final JButton buttonSelectCentersFile = new JButton("Browse");
    final JLabel labelWaterFilter = new JLabel("Water Territory Filter");
    final JLabel labelWaterFilterExample = new JLabel("e.g. 'Sea Zone'");

    GridBagLayout gbl_panel_1 = new GridBagLayout();
    gbl_panel_1.rowHeights = new int[] {32, 32, 32, 32, 32, 32, 32, 0};
    gbl_panel_1.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
    stepActionPanel.setLayout(gbl_panel_1);

    // Map Name
    GridBagConstraints gridBadConstLabelMapName = new GridBagConstraints();
    gridBadConstLabelMapName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelMapName.anchor = GridBagConstraints.NORTHEAST;
    gridBadConstLabelMapName.gridheight = 32;
    gridBadConstLabelMapName.gridy = 1;
    gridBadConstLabelMapName.gridx = 0;
    stepActionPanel.add(labelMapName, gridBadConstLabelMapName);

    tMapName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent e) {
        MapXMLHelper.putXmlStrings("info_@name", tMapName.getText());
      }

      @Override
      public void focusGained(FocusEvent e) {}
    });
    tMapName.setMaximumSize(new Dimension(0, 0));
    tMapName.setColumns(30);
    GridBagConstraints gbc_tMapName = (GridBagConstraints) gridBadConstLabelMapName.clone();
    gbc_tMapName.anchor = GridBagConstraints.NORTH;
    gbc_tMapName.gridx = 1;
    tMapName.setMaximumSize(tMapName.getPreferredSize());
    stepActionPanel.add(tMapName, gbc_tMapName);

    GridBagConstraints gridBadConstLabelMapNameExample = (GridBagConstraints) gridBadConstLabelMapName.clone();
    gridBadConstLabelMapNameExample.anchor = GridBagConstraints.NORTHWEST;
    gridBadConstLabelMapNameExample.gridx = 2;
    stepActionPanel.add(labelMapNameExample, gridBadConstLabelMapNameExample);

    // Map Version
    stepActionPanel.add(labelMapVersion, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 2, 2));

    tMapVersion.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent e) {
        MapXMLHelper.putXmlStrings("info_@version", tMapVersion.getText());
      }

      @Override
      public void focusGained(FocusEvent e) {}
    });
    tMapVersion.setMaximumSize(new Dimension(0, 0));
    tMapVersion.setColumns(30);
    stepActionPanel.add(tMapVersion, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 2));


    stepActionPanel.add(labelMapVersionExample, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 2));

    // Resource Name
    stepActionPanel.add(labelResourceName, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 0, 3));

    tResourceName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent e) {
        MapXMLHelper.addResourceList(0, tResourceName.getText());
      }

      @Override
      public void focusGained(FocusEvent e) {}
    });
    tResourceName.setMaximumSize(new Dimension(0, 0));
    tResourceName.setColumns(30);
    stepActionPanel.add(tResourceName, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 3));

    stepActionPanel.add(labelResourceNameExample, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 3));

    // Map Image File
    stepActionPanel.add(labelMapImageFile, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 0, 4));

    tMapImageFile.setEnabled(false);
    tMapImageFile.setMaximumSize(new Dimension(0, 0));
    tMapImageFile.setColumns(30);
    tMapImageFile.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        buttonSelectMapImageFile.doClick();
      }

      @Override
      public void mouseEntered(MouseEvent e) {}

      @Override
      public void mouseExited(MouseEvent e) {}

      @Override
      public void mousePressed(MouseEvent e) {}

      @Override
      public void mouseReleased(MouseEvent e) {}

    });
    stepActionPanel.add(tMapImageFile, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 4));

    buttonSelectMapImageFile.addActionListener(new AbstractAction("Select Map Image File") {
      private static final long serialVersionUID = 3918797244306320614L;

      public void actionPerformed(final ActionEvent e) {
        selectMapImageFile();
        if (MapXMLCreator.mapImageFile != null) {
          tMapImageFile.setText(MapXMLCreator.mapImageFile.getAbsolutePath());
          if (MapXMLCreator.mapFolderLocation != null && MapXMLCreator.mapCentersFile == null) {
            final File fileGuess = new File(MapXMLCreator.mapFolderLocation, "centers.txt");
            if (fileGuess.exists()
                && JOptionPane.showConfirmDialog(new JPanel(),
                    "A centers.txt file was found in the map's folder, do you want to use the file to supply the territories names?",
                    "File Suggestion", 1) == 0) {
              MapXMLCreator.mapCentersFile = fileGuess;
              tCentersFile.setText(MapXMLCreator.mapCentersFile.getAbsolutePath());
              tWaterFilter.setEnabled(true);
            }
          }
          if (MapXMLCreator.mapImageFile != null && tMapName.getText().isEmpty()) {
            String mapFileName = MapXMLCreator.mapImageFile.getName();
            mapFileName = mapFileName.substring(0, mapFileName.lastIndexOf("."));
            tMapName.setText(mapFileName);
            MapXMLHelper.putXmlStrings("info_@name", mapFileName);
          }
        }
      }
    });
    stepActionPanel.add(buttonSelectMapImageFile, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 4));

    // Map Centers File
    stepActionPanel.add(labelCentersFile, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 0, 5));

    tCentersFile.setEnabled(false);
    tCentersFile.setMaximumSize(new Dimension(0, 0));
    tCentersFile.setColumns(30);
    tCentersFile.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        buttonSelectCentersFile.doClick();
      }

      @Override
      public void mouseEntered(MouseEvent e) {}

      @Override
      public void mouseExited(MouseEvent e) {}

      @Override
      public void mousePressed(MouseEvent e) {}

      @Override
      public void mouseReleased(MouseEvent e) {}

    });
    stepActionPanel.add(tCentersFile, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 5));

    buttonSelectCentersFile.addActionListener(new AbstractAction("Select Centers File") {
      private static final long serialVersionUID = -1634037486288735207L;

      public void actionPerformed(final ActionEvent e) {
        selectCentersFile();
        if (MapXMLCreator.mapCentersFile != null)
          tWaterFilter.setEnabled(true);
      }
    });
    stepActionPanel.add(buttonSelectCentersFile, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 5));

    // Water Territory Filter
    stepActionPanel.add(labelWaterFilter, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 0, 6, GridBagConstraints.NORTHWEST));

    tWaterFilter.setEnabled(MapXMLCreator.mapCentersFile != null);
    tWaterFilter.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent e) {
        MapXMLCreator.waterFilterString = tWaterFilter.getText();
      }

      @Override
      public void focusGained(FocusEvent e) {}
    });
    tWaterFilter.setColumns(30);
    tWaterFilter.setMaximumSize(tWaterFilter.getPreferredSize());
    stepActionPanel.add(tWaterFilter, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 6));

    stepActionPanel.add(labelWaterFilterExample, MapXMLHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 6));

    mapXMLCreator.autoFillButton.setEnabled(false);
  }

  public static void selectCentersFile() {
    final FileOpen centersSelection = new FileOpen("Select center.txte File", MapXMLCreator.mapImageFile, ".txt");
    if (centersSelection.getFile() != null) {
      final File mapCentersFile = centersSelection.getFile();
      if (mapCentersFile.exists()) {
        if (MapXMLCreator.mapFolderLocation == null) {
          MapXMLCreator.mapFolderLocation = mapCentersFile.getParentFile();
          System.setProperty(MapXMLCreator.TRIPLEA_MAP_FOLDER, MapXMLCreator.mapFolderLocation.getPath());
        }
        MapXMLCreator.mapCentersFile = mapCentersFile;

      }
    }
  }

  public static void selectMapImageFile() {
    final FileOpen mapSelection = new FileOpen("Select Map Image File", MapXMLCreator.mapImageFile, ".gif", ".png");
    if (mapSelection.getFile() != null) {

      final File mapImageFile = mapSelection.getFile();
      if (mapImageFile.exists()) {
        if (MapXMLCreator.mapFolderLocation == null) {
          MapXMLCreator.mapFolderLocation = mapImageFile.getParentFile();
          System.setProperty(MapXMLCreator.TRIPLEA_MAP_FOLDER, MapXMLCreator.mapFolderLocation.getPath());
        }
        MapXMLCreator.mapImageFile = mapImageFile;

      }
    }
  }

}
