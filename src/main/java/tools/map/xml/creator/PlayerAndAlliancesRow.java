package tools.map.xml.creator;

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
  private JTextField textFieldPlayerName;
  private JComboBox<String> comboBoxPlayerAlliance;
  private JTextField textFieldInitialResource;

  public PlayerAndAlliancesRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel,
      final String playerName, final String allianceName, final String[] alliances, final int initialResource) {
    super(playerName, parentRowPanel, stepActionPanel);

    textFieldPlayerName = new JTextField(playerName);
    comboBoxPlayerAlliance = new JComboBox<>(alliances);
    textFieldInitialResource =
        new JTextField(Integer.toString(initialResource));

    Dimension dimension = textFieldPlayerName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    textFieldPlayerName.setPreferredSize(dimension);
    MapXmlUIHelper.addNewFocusListenerForTextField(textFieldPlayerName, () -> {
      final String inputText = textFieldPlayerName.getText().trim();
      if (currentRowName.equals(inputText)) {
        return;
      }
      if (MapXmlHelper.getPlayerNames().contains(inputText)) {
        textFieldPlayerName.selectAll();
        JOptionPane.showMessageDialog(stepActionPanel, "Player '" + inputText + "' already exists.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        parentRowPanel.setDataIsConsistent(false);
        SwingUtilities.invokeLater(() -> textFieldPlayerName.requestFocus());
        return;
      }
      // everything is okay with the new player namer, lets rename everything
      MapXmlHelper.getPlayerNames().remove(currentRowName);
      MapXmlHelper.getPlayerNames().add(inputText);
      MapXmlHelper.getPlayerAllianceMap().remove(currentRowName);
      MapXmlHelper.getPlayerAllianceMap().put(inputText, MapXmlHelper.getPlayerAllianceMap().get(currentRowName));
      MapXmlHelper.getPlayerInitResourcesMap().remove(currentRowName);
      MapXmlHelper.getPlayerInitResourcesMap().put(inputText,
          MapXmlHelper.getPlayerInitResourcesMap().get(currentRowName));
      if (!MapXmlHelper.getPlayerSequenceMap().isEmpty()) {
        // Replace Player Names for Player Sequence
        final LinkedHashMap<String, Triple<String, String, Integer>> updatesPlayerSequence =
            Maps.newLinkedHashMap();
        for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXmlHelper.getPlayerSequenceMap()
            .entrySet()) {
          final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
          if (currentRowName.equals(oldTriple.getSecond())) {
            updatesPlayerSequence.put(playerSequence.getKey(),
                Triple.of(oldTriple.getFirst(), inputText, oldTriple.getThird()));
          }
        }
        for (final Entry<String, Triple<String, String, Integer>> playerSequence : updatesPlayerSequence.entrySet()) {
          MapXmlHelper.getPlayerSequenceMap().put(playerSequence.getKey(), playerSequence.getValue());
        }
      }
      if (!MapXmlHelper.getProductionFrontiersMap().isEmpty()) {
        final List<String> productionFrontier = MapXmlHelper.getProductionFrontiersMap().get(currentRowName);
        if (productionFrontier != null) {
          MapXmlHelper.getProductionFrontiersMap().remove(currentRowName);
          MapXmlHelper.getProductionFrontiersMap().put(inputText, productionFrontier);
        }
      }

      if (!MapXmlHelper.getTechnologyDefinitionsMap().isEmpty()) {
        // Delete Technology Definitions for this Player Name (techKey ending with '_' + PlayerName)
        final Map<String, List<String>> newEntryMap = Maps.newLinkedHashMap();
        final String compareValue = "_" + currentRowName;
        for (final Entry<String, List<String>> technologyDefinition : MapXmlHelper.getTechnologyDefinitionsMap()
            .entrySet()) {
          final String techKey = technologyDefinition.getKey();
          if (techKey.endsWith(compareValue)) {
            final List<String> techValues = technologyDefinition.getValue();
            techValues.set(0, inputText);
            newEntryMap.put(techKey.substring(0, techKey.lastIndexOf(compareValue)) + "_" + inputText, techValues);
          } else {
            newEntryMap.put(techKey, technologyDefinition.getValue());
          }
        }
        MapXmlHelper.setTechnologyDefinitions(newEntryMap);
      }
      currentRowName = inputText;
      parentRowPanel.setDataIsConsistent(true);
    });

    dimension = comboBoxPlayerAlliance.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    comboBoxPlayerAlliance.setPreferredSize(dimension);
    comboBoxPlayerAlliance.setSelectedIndex(Arrays.binarySearch(alliances, allianceName));
    comboBoxPlayerAlliance.addFocusListener(FocusListenerFocusLost.withAction(() ->
        // everything is okay with the new technology name, lets rename everything
        MapXmlHelper.getPlayerAllianceMap().put(playerName, (String) comboBoxPlayerAlliance.getSelectedItem())));

    dimension = textFieldInitialResource.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldInitialResource.setPreferredSize(dimension);
    textFieldInitialResource.addFocusListener(new FocusListener() {
      String prevValue = Integer.toString(initialResource);

      @Override
      public void focusLost(final FocusEvent arg0) {
        final String inputText = textFieldInitialResource.getText().trim();
        try {
          MapXmlHelper.getPlayerInitResourcesMap().put(playerName, Integer.parseInt(inputText));
        } catch (final NumberFormatException e) {
          textFieldInitialResource.setText(prevValue);
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(() -> {
            textFieldInitialResource.updateUI();
            textFieldInitialResource.requestFocus();
            textFieldInitialResource.selectAll();
          });
          return;
        }
        prevValue = inputText;
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(final FocusEvent arg0) {
        textFieldInitialResource.selectAll();
      }
    });

  }

  public boolean isAllianceSelected(final String removeAllianceName) {
    return comboBoxPlayerAlliance.getSelectedItem().equals(removeAllianceName);
  }

  public void removeFromComboBoxesAlliance(final String removeAlliance) {
    comboBoxPlayerAlliance.removeItem(removeAlliance);
  }

  public void updateComboBoxesAlliance(final String newAlliance) {
    comboBoxPlayerAlliance.addItem(newAlliance);
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<>();
    componentList.add(textFieldPlayerName);
    componentList.add(comboBoxPlayerAlliance);
    componentList.add(textFieldInitialResource);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbcTemplate) {
    parent.add(textFieldPlayerName, gbcTemplate);

    final GridBagConstraints gbc_tPlayerAlliance = (GridBagConstraints) gbcTemplate.clone();
    gbc_tPlayerAlliance.gridx = 1;
    parent.add(comboBoxPlayerAlliance, gbc_tPlayerAlliance);

    final GridBagConstraints gbc_tInitialResource = (GridBagConstraints) gbcTemplate.clone();
    gbc_tInitialResource.gridx = 2;
    parent.add(textFieldInitialResource, gbc_tInitialResource);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbcTemplate.clone();
    gridBadConstButtonRemove.gridx = 3;
    parent.add(buttonRemovePerRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final PlayerAndAlliancesRow newRowPlayerAndAlliancesRow = (PlayerAndAlliancesRow) newRow;
    this.textFieldPlayerName.setText(newRowPlayerAndAlliancesRow.textFieldPlayerName.getText());
    this.comboBoxPlayerAlliance.setSelectedIndex(newRowPlayerAndAlliancesRow.comboBoxPlayerAlliance.getSelectedIndex());
    this.textFieldInitialResource.setText(newRowPlayerAndAlliancesRow.textFieldInitialResource.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXmlHelper.getPlayerNames().remove(currentRowName);
    MapXmlHelper.getPlayerAllianceMap().remove(currentRowName);
    MapXmlHelper.getPlayerInitResourcesMap().remove(currentRowName);
    if (!MapXmlHelper.getPlayerSequenceMap().isEmpty()) {
      // Replace Player Sequences using the deleted Player Name
      final ArrayList<String> deleteKeys = new ArrayList<>();
      for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXmlHelper.getPlayerSequenceMap()
          .entrySet()) {
        final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
        if (currentRowName.equals(oldTriple.getSecond())) {
          deleteKeys.add(playerSequence.getKey());
        }
      }
      for (final String deleteKey : deleteKeys) {
        MapXmlHelper.getPlayerSequenceMap().remove(deleteKey);
      }
    }
    if (!MapXmlHelper.getTechnologyDefinitionsMap().isEmpty()) {
      // Replace Technology Definitions using the deleted Player Name
      final ArrayList<String> deleteKeys = new ArrayList<>();
      final String compareValue = "_" + currentRowName;
      for (final Entry<String, List<String>> technologyDefinition : MapXmlHelper.getTechnologyDefinitionsMap()
          .entrySet()) {
        final String techKey = technologyDefinition.getKey();
        if (techKey.endsWith(compareValue)) {
          deleteKeys.add(techKey);
        }
      }
      for (final String deleteKey : deleteKeys) {
        MapXmlHelper.getTechnologyDefinitionsMap().remove(deleteKey);
      }
    }
  }
}
