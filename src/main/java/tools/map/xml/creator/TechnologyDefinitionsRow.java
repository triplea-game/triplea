package tools.map.xml.creator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


class TechnologyDefinitionsRow extends DynamicRow {
  private JTextField textFieldTechnologyName;
  private JComboBox<String> comboBoxPlayerName;
  private JComboBox<String> comboBoxAlreadyEnabled;
  public static String[] selectionTrueFalse = {"false", "true"};

  public TechnologyDefinitionsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel,
      final String technologyName, final String playerName, final String[] playerNames, final String alreadyEnabled) {
    super(technologyName + "_" + playerName, parentRowPanel, stepActionPanel);

    textFieldTechnologyName = new JTextField(technologyName);
    comboBoxPlayerName = new JComboBox<>(playerNames);
    comboBoxAlreadyEnabled = new JComboBox<>(selectionTrueFalse);

    Dimension dimension = textFieldTechnologyName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    textFieldTechnologyName.setPreferredSize(dimension);
    MapXmlUiHelper.addNewFocusListenerForTextField(textFieldTechnologyName, () -> {
      final String inputText = textFieldTechnologyName.getText().trim();
      final String curr_playerName = (String) comboBoxPlayerName.getSelectedItem();
      if (currentRowName.startsWith(inputText + "_")) {
        return;
      }
      final String newRowName = inputText + "_" + curr_playerName;
      if (MapXmlHelper.getTechnologyDefinitionsMap().containsKey(newRowName)) {
        JOptionPane.showMessageDialog(stepActionPanel,
            "Technology '" + inputText + "' already exists for player '" + curr_playerName + "'.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        parentRowPanel.setDataIsConsistent(false);
        SwingUtilities.invokeLater(() -> {
          textFieldTechnologyName.requestFocus();
          textFieldTechnologyName.selectAll();
        });
        return;
      }
      // everything is okay with the new technology name, lets rename everything
      final List<String> newValues = MapXmlHelper.getTechnologyDefinitionsMap().get(currentRowName);
      MapXmlHelper.getTechnologyDefinitionsMap().remove(currentRowName);
      MapXmlHelper.getTechnologyDefinitionsMap().put(newRowName, newValues);
      currentRowName = newRowName;
      parentRowPanel.setDataIsConsistent(true);
    });

    dimension = comboBoxPlayerName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    comboBoxPlayerName.setPreferredSize(dimension);
    comboBoxPlayerName.setSelectedIndex(Arrays.binarySearch(playerNames, playerName));
    comboBoxPlayerName.addFocusListener(new FocusListenerFocusLost() {
      int prevSelectedIndex = comboBoxPlayerName.getSelectedIndex();

      @Override
      public void focusLost(final FocusEvent arg0) {
        if (prevSelectedIndex == comboBoxPlayerName.getSelectedIndex()) {
          return;
        }
        final String techInputText = textFieldTechnologyName.getText().trim();
        final String curr_playerName = (String) comboBoxPlayerName.getSelectedItem();
        if (currentRowName.endsWith("_" + curr_playerName)) {
          return;
        }
        final String newRowName = techInputText + "_" + curr_playerName;
        if (MapXmlHelper.getTechnologyDefinitionsMap().containsKey(newRowName)) {
          JOptionPane.showMessageDialog(stepActionPanel,
              "Technology '" + techInputText + "' already exists for player '" + curr_playerName + "'.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          SwingUtilities.invokeLater(() -> {
            comboBoxPlayerName.setSelectedIndex(prevSelectedIndex);
            comboBoxPlayerName.requestFocus();
          });
          return;
        }
        // everything is okay with the new technology name, lets rename everything
        final List<String> newValues = MapXmlHelper.getTechnologyDefinitionsMap().get(currentRowName);
        MapXmlHelper.getTechnologyDefinitionsMap().remove(currentRowName);
        MapXmlHelper.getTechnologyDefinitionsMap().put(newRowName, newValues);
        currentRowName = newRowName;
        prevSelectedIndex = comboBoxPlayerName.getSelectedIndex();
      }
    });

    dimension = comboBoxAlreadyEnabled.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    comboBoxAlreadyEnabled.setPreferredSize(dimension);
    comboBoxAlreadyEnabled.setSelectedIndex(Arrays.binarySearch(selectionTrueFalse, alreadyEnabled));
    comboBoxAlreadyEnabled.addFocusListener(FocusListenerFocusLost.withAction(() ->
        // everything is okay with the new technology name, lets rename everything
        MapXmlHelper.getTechnologyDefinitionsMap().get(currentRowName).set(1,
            (String) comboBoxAlreadyEnabled.getSelectedItem())));
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<>();
    componentList.add(textFieldTechnologyName);
    componentList.add(comboBoxPlayerName);
    componentList.add(comboBoxAlreadyEnabled);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbcTemplate) {
    parent.add(textFieldTechnologyName, gbcTemplate);

    final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbcTemplate.clone();
    gbc_tClassName.gridx = 1;
    parent.add(comboBoxPlayerName, gbc_tClassName);

    final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbcTemplate.clone();
    gbc_tDisplayName.gridx = 2;
    parent.add(comboBoxAlreadyEnabled, gbc_tDisplayName);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbcTemplate.clone();
    gridBadConstButtonRemove.gridx = 3;
    parent.add(buttonRemovePerRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final TechnologyDefinitionsRow newTechnologyDefinitionsRow = (TechnologyDefinitionsRow) newRow;
    this.textFieldTechnologyName.setText(newTechnologyDefinitionsRow.textFieldTechnologyName.getText());
    this.comboBoxPlayerName.setSelectedIndex(newTechnologyDefinitionsRow.comboBoxPlayerName.getSelectedIndex());
    this.comboBoxAlreadyEnabled.setSelectedIndex(newTechnologyDefinitionsRow.comboBoxAlreadyEnabled.getSelectedIndex());
  }

  @Override
  protected void removeRowAction() {
    MapXmlHelper.getTechnologyDefinitionsMap().remove(currentRowName);
  }
}
