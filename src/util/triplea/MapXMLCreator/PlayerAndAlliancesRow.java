package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;

import games.strategy.util.Triple;


class PlayerAndAlliancesRow extends DynamicRow {
  JTextField tPlayerName;
  JComboBox<String> tPlayerAlliance;
  JTextField tInitialResource;

  public PlayerAndAlliancesRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel,
      final String playerName, final String allianceName, final String[] alliances, final int initialResource) {
    super(playerName, parentRowPanel, stepActionPanel);

    tPlayerName = new JTextField(playerName);
    tPlayerAlliance = new JComboBox<String>(alliances);
    final Integer initialResourceInteger = Integer.valueOf(initialResource);
    tInitialResource =
        new JTextField(initialResourceInteger == null ? "0" : Integer.toString(initialResourceInteger));

    Dimension dimension = tPlayerName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    tPlayerName.setPreferredSize(dimension);
    tPlayerName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tPlayerName.getText().trim();
        if (currentRowName.equals(inputText))
          return;
        if (MapXMLHelper.playerName.contains(inputText)) {
          tPlayerName.selectAll();
          JOptionPane.showMessageDialog(stepActionPanel, "Player '" + inputText + "' already exists.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
              tPlayerName.requestFocus();
            }
          });
          return;
        }
        // everything is okay with the new player namer, lets rename everything
        MapXMLHelper.playerName.remove(currentRowName);
        MapXMLHelper.playerName.add(inputText);
        MapXMLHelper.playerAlliance.remove(currentRowName);
        MapXMLHelper.playerAlliance.put(inputText, MapXMLHelper.playerAlliance.get(currentRowName));
        MapXMLHelper.playerInitResources.remove(currentRowName);
        MapXMLHelper.playerInitResources.put(inputText, MapXMLHelper.playerInitResources.get(currentRowName));
        if (!MapXMLHelper.playerSequence.isEmpty()) {
          // Replace Player Names for Player Sequence
          final LinkedHashMap<String, Triple<String, String, Integer>> updatesPlayerSequence =
              Maps.newLinkedHashMap();
          for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.playerSequence
              .entrySet()) {
            final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
            if (currentRowName.equals(oldTriple.getSecond())) {
              updatesPlayerSequence.put(playerSequence.getKey(),
                  Triple.of(oldTriple.getFirst(), inputText, oldTriple.getThird()));
            }
          }
          for (final Entry<String, Triple<String, String, Integer>> playerSequence : updatesPlayerSequence.entrySet()) {
            MapXMLHelper.playerSequence.put(playerSequence.getKey(), playerSequence.getValue());
          }
        }
        if (!MapXMLHelper.productionFrontiers.isEmpty()) {
          final List<String> productionFrontier = MapXMLHelper.productionFrontiers.get(currentRowName);
          if (productionFrontier != null) {
            MapXMLHelper.productionFrontiers.remove(currentRowName);
            MapXMLHelper.productionFrontiers.put(inputText, productionFrontier);
          }
        }

        if (!MapXMLHelper.technologyDefinitions.isEmpty()) {
          // Delete Technology Definitions for this Player Name (techKey ending with '_' + PlayerName)
          final Map<String, List<String>> newEntryMap = Maps.newLinkedHashMap();
          final String compareValue = "_" + currentRowName;
          for (final Entry<String, List<String>> technologyDefinition : MapXMLHelper.technologyDefinitions
              .entrySet()) {
            final String techKey = technologyDefinition.getKey();
            if (techKey.endsWith(compareValue)) {
              final List<String> techValues = technologyDefinition.getValue();
              techValues.set(0, inputText);
              newEntryMap.put(techKey.substring(0, techKey.lastIndexOf(compareValue)) + "_" + inputText, techValues);
            } else
              newEntryMap.put(techKey, technologyDefinition.getValue());
          }
          MapXMLHelper.technologyDefinitions = newEntryMap;
        }
        currentRowName = inputText;
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tPlayerName.selectAll();
      }
    });

    dimension = tPlayerAlliance.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    tPlayerAlliance.setPreferredSize(dimension);
    tPlayerAlliance.setSelectedIndex(Arrays.binarySearch(alliances, allianceName));
    tPlayerAlliance.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent arg0) {
        // everything is okay with the new technology name, lets rename everything
        MapXMLHelper.playerAlliance.put(playerName, (String) tPlayerAlliance.getSelectedItem());
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });

    dimension = tInitialResource.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tInitialResource.setPreferredSize(dimension);
    tInitialResource.addFocusListener(new FocusListener() {
      String prevValue = Integer.toString(initialResource);

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tInitialResource.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          MapXMLHelper.playerInitResources.put(playerName, newValue);
        } catch (NumberFormatException e) {
          tInitialResource.setText(prevValue);
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tInitialResource.updateUI();
              tInitialResource.requestFocus();
              tInitialResource.selectAll();
            }
          });
          return;
        }
        prevValue = inputText;
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tInitialResource.selectAll();
      }
    });

  }

  public boolean isAllianceSelected(final String removeAllianceName) {
    return tPlayerAlliance.getSelectedItem().equals(removeAllianceName);
  }

  public void removeFromComboBoxesAlliance(String removeAlliance) {
    tPlayerAlliance.removeItem(removeAlliance);
  }

  public void updateComboBoxesAlliance(final String newAlliance) {
    tPlayerAlliance.addItem(newAlliance);
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(tPlayerName);
    componentList.add(tPlayerAlliance);
    componentList.add(tInitialResource);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(tPlayerName, gbc_template);

    final GridBagConstraints gbc_tPlayerAlliance = (GridBagConstraints) gbc_template.clone();
    gbc_tPlayerAlliance.gridx = 1;
    parent.add(tPlayerAlliance, gbc_tPlayerAlliance);

    final GridBagConstraints gbc_tInitialResource = (GridBagConstraints) gbc_template.clone();
    gbc_tInitialResource.gridx = 2;
    parent.add(tInitialResource, gbc_tInitialResource);

    final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
    gbc_bRemove.gridx = 3;
    parent.add(bRemoveRow, gbc_bRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final PlayerAndAlliancesRow newRowPlayerAndAlliancesRow = (PlayerAndAlliancesRow) newRow;
    this.tPlayerName.setText(newRowPlayerAndAlliancesRow.tPlayerName.getText());
    this.tPlayerAlliance.setSelectedIndex(newRowPlayerAndAlliancesRow.tPlayerAlliance.getSelectedIndex());
    this.tInitialResource.setText(newRowPlayerAndAlliancesRow.tInitialResource.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.playerName.remove(currentRowName);
    MapXMLHelper.playerAlliance.remove(currentRowName);
    MapXMLHelper.playerInitResources.remove(currentRowName);
    if (!MapXMLHelper.playerSequence.isEmpty()) {
      // Replace Player Sequences using the deleted Player Name
      final ArrayList<String> deleteKeys = new ArrayList<String>();
      for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.playerSequence
          .entrySet()) {
        final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
        if (currentRowName.equals(oldTriple.getSecond())) {
          deleteKeys.add(playerSequence.getKey());
        }
      }
      for (final String deleteKey : deleteKeys)
        MapXMLHelper.playerSequence.remove(deleteKey);
    }
    if (!MapXMLHelper.technologyDefinitions.isEmpty()) {
      // Replace Technology Definitions using the deleted Player Name
      final ArrayList<String> deleteKeys = new ArrayList<String>();
      final String compareValue = "_" + currentRowName;
      for (final Entry<String, List<String>> technologyDefinition : MapXMLHelper.technologyDefinitions
          .entrySet()) {
        final String techKey = technologyDefinition.getKey();
        if (techKey.endsWith(compareValue)) {
          deleteKeys.add(techKey);
        }
      }
      for (final String deleteKey : deleteKeys)
        MapXMLHelper.technologyDefinitions.remove(deleteKey);
    }
  }
}
