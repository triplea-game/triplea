package util.triplea.mapXmlCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import games.strategy.common.swing.SwingAction;
import games.strategy.ui.Util;
import util.image.FileOpen;


public class MapPropertiesPanel {

  public static void layout(final MapXmlCreator mapXmlCreator) {

    if (!MapXmlHelper.getXmlStringsMap().containsKey("info_@name")) {
      for (final String newKey : new String[] {"info_@name", "info_@version"}) {
        MapXmlHelper.putXmlStrings(newKey, "");
      }
    }

    if (MapXmlHelper.getResourceList().isEmpty()) {
      MapXmlHelper.addResourceList("");
    }

    final JTextField textFieldMapName = new JTextField(MapXmlHelper.getXmlStringsMap().get("info_@name"));
    final JTextField textFieldMapVersion = new JTextField(MapXmlHelper.getXmlStringsMap().get("info_@version"));
    final JTextField textFieldResourceName = new JTextField(MapXmlHelper.getResourceList().get(0));
    final JTextField textFieldMapImageFile =
        new JTextField((MapXmlCreator.mapImageFile == null ? "" : MapXmlCreator.mapImageFile.getAbsolutePath()));
    final JTextField textFieldCentersFile = new JTextField(
        (MapXmlCreator.mapCentersFile == null ? "" : MapXmlCreator.mapCentersFile.getAbsolutePath()));
    final JTextField textFieldWaterFilter = new JTextField(MapXmlCreator.waterFilterString);
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
    final JLabel labelWaterFilterExample = new JLabel("e.g. '" + Util.TERRITORY_SEA_ZONE_INFIX + "'");

    final GridBagLayout gridBadConstLabelPanel = new GridBagLayout();
    gridBadConstLabelPanel.rowHeights = getRowHeights();
    gridBadConstLabelPanel.rowWeights = getRowWeights();
    final JPanel stepActionPanel = mapXmlCreator.getStepActionPanel();
    stepActionPanel.setLayout(gridBadConstLabelPanel);

    // Map Name
    final GridBagConstraints gridBadConstLabelMapName = new GridBagConstraints();
    gridBadConstLabelMapName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelMapName.anchor = GridBagConstraints.NORTHEAST;
    gridBadConstLabelMapName.gridheight = 32;
    gridBadConstLabelMapName.gridy = 1;
    gridBadConstLabelMapName.gridx = 0;
    stepActionPanel.add(labelMapName, gridBadConstLabelMapName);

    textFieldMapName.addFocusListener(FocusListenerFocusLost
        .withAction(() -> MapXmlHelper.putXmlStrings("info_@name", textFieldMapName.getText())));
    textFieldMapName.setMaximumSize(new Dimension(0, 0));
    final int columns = 30;
    textFieldMapName.setColumns(columns);
    final GridBagConstraints gridBadConstTextFieldMapName = (GridBagConstraints) gridBadConstLabelMapName.clone();
    gridBadConstTextFieldMapName.anchor = GridBagConstraints.NORTH;
    gridBadConstTextFieldMapName.gridx = 1;
    textFieldMapName.setMaximumSize(textFieldMapName.getPreferredSize());
    stepActionPanel.add(textFieldMapName, gridBadConstTextFieldMapName);

    final GridBagConstraints gridBadConstLabelMapNameExample = (GridBagConstraints) gridBadConstLabelMapName.clone();
    gridBadConstLabelMapNameExample.anchor = GridBagConstraints.NORTHWEST;
    gridBadConstLabelMapNameExample.gridx = 2;
    stepActionPanel.add(labelMapNameExample, gridBadConstLabelMapNameExample);

    // Map Version
    stepActionPanel.add(labelMapVersion, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 2, 2));

    textFieldMapVersion.addFocusListener(FocusListenerFocusLost
        .withAction(() -> MapXmlHelper.putXmlStrings("info_@version", textFieldMapVersion.getText())));
    textFieldMapVersion.setMaximumSize(new Dimension(0, 0));
    textFieldMapVersion.setColumns(columns);
    stepActionPanel.add(textFieldMapVersion, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 2));


    stepActionPanel.add(labelMapVersionExample, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 2));

    // Resource Name
    stepActionPanel.add(labelResourceName, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 0, 3));

    textFieldResourceName.addFocusListener(FocusListenerFocusLost
        .withAction(() -> MapXmlHelper.addResourceList(0, textFieldResourceName.getText())));
    textFieldResourceName.setMaximumSize(new Dimension(0, 0));
    textFieldResourceName.setColumns(columns);
    stepActionPanel.add(textFieldResourceName, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 3));

    stepActionPanel.add(labelResourceNameExample,
        MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 3));

    // Map Image File
    stepActionPanel.add(labelMapImageFile, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 0, 4));

    textFieldMapImageFile.setEnabled(false);
    textFieldMapImageFile.setMaximumSize(new Dimension(0, 0));
    textFieldMapImageFile.setColumns(columns);
    textFieldMapImageFile.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        buttonSelectMapImageFile.doClick();
      }
    });
    stepActionPanel.add(textFieldMapImageFile, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 4));

    buttonSelectMapImageFile.addActionListener(SwingAction.of("Select Map Image File", e -> {
      selectMapImageFile();
      if (MapXmlCreator.mapImageFile != null) {
        textFieldMapImageFile.setText(MapXmlCreator.mapImageFile.getAbsolutePath());
        if (MapXmlCreator.mapFolderLocation != null && MapXmlCreator.mapCentersFile == null) {
          final File fileGuess = new File(MapXmlCreator.mapFolderLocation, "centers.txt");
          if (fileGuess.exists()
              && JOptionPane.showConfirmDialog(new JPanel(),
                  "A centers.txt file was found in the map's folder, do you want to use the file to supply the territories names?",
                  "File Suggestion", 1) == 0) {
            MapXmlCreator.mapCentersFile = fileGuess;
            textFieldCentersFile.setText(MapXmlCreator.mapCentersFile.getAbsolutePath());
            textFieldWaterFilter.setEnabled(true);
          }
        }
        if (MapXmlCreator.mapImageFile != null && textFieldMapName.getText().isEmpty()) {
          String mapFileName = MapXmlCreator.mapImageFile.getName();
          mapFileName = mapFileName.substring(0, mapFileName.lastIndexOf("."));
          textFieldMapName.setText(mapFileName);
          MapXmlHelper.putXmlStrings("info_@name", mapFileName);
        }
      }
    }));
    stepActionPanel.add(buttonSelectMapImageFile,
        MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 4));

    // Map Centers File
    stepActionPanel.add(labelCentersFile, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 0, 5));

    textFieldCentersFile.setEnabled(false);
    textFieldCentersFile.setMaximumSize(new Dimension(0, 0));
    textFieldCentersFile.setColumns(columns);
    textFieldCentersFile.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        buttonSelectCentersFile.doClick();
      }
    });
    stepActionPanel.add(textFieldCentersFile, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 5));

    buttonSelectCentersFile.addActionListener(SwingAction.of("Select Centers File", e -> {
      selectCentersFile();
      if (MapXmlCreator.mapCentersFile != null) {
        textFieldWaterFilter.setEnabled(true);
      }
    }));
    stepActionPanel.add(buttonSelectCentersFile, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 5));

    // Water Territory Filter
    stepActionPanel.add(labelWaterFilter,
        MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 0, 6, GridBagConstraints.NORTHWEST));

    textFieldWaterFilter.setEnabled(MapXmlCreator.mapCentersFile != null);
    textFieldWaterFilter.addFocusListener(
        FocusListenerFocusLost.withAction(() -> MapXmlCreator.waterFilterString = textFieldWaterFilter.getText()));
    textFieldWaterFilter.setColumns(columns);
    textFieldWaterFilter.setMaximumSize(textFieldWaterFilter.getPreferredSize());
    stepActionPanel.add(textFieldWaterFilter, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapName, 1, 6));

    stepActionPanel.add(labelWaterFilterExample, MapXmlUIHelper.getGBCCloneWith(gridBadConstLabelMapNameExample, 2, 6));

    mapXmlCreator.setAutoFillActionListener(null);
  }

  private static double[] getRowWeights() {
    return new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
  }

  private static int[] getRowHeights() {
    return new int[] {32, 32, 32, 32, 32, 32, 32, 0};
  }

  public static void selectCentersFile() {
    final FileOpen centersSelection = new FileOpen("Select center.txte File", MapXmlCreator.mapImageFile, ".txt");
    if (centersSelection.getFile() != null) {
      final File mapCentersFile = centersSelection.getFile();
      if (mapCentersFile.exists()) {
        if (MapXmlCreator.mapFolderLocation == null) {
          MapXmlCreator.mapFolderLocation = mapCentersFile.getParentFile();
          System.setProperty(MapXmlCreator.TRIPLEA_MAP_FOLDER, MapXmlCreator.mapFolderLocation.getPath());
        }
        MapXmlCreator.mapCentersFile = mapCentersFile;

      }
    }
  }

  public static void selectMapImageFile() {
    final FileOpen mapSelection = new FileOpen("Select Map Image File", MapXmlCreator.mapImageFile, ".gif", ".png");
    if (mapSelection.getFile() != null) {

      final File mapImageFile = mapSelection.getFile();
      if (mapImageFile.exists()) {
        if (MapXmlCreator.mapFolderLocation == null) {
          MapXmlCreator.mapFolderLocation = mapImageFile.getParentFile();
          System.setProperty(MapXmlCreator.TRIPLEA_MAP_FOLDER, MapXmlCreator.mapFolderLocation.getPath());
        }
        MapXmlCreator.mapImageFile = mapImageFile;

      }
    }
  }

}
