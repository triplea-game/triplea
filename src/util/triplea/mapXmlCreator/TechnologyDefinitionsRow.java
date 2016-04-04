package util.triplea.mapXmlCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
    comboBoxPlayerName = new JComboBox<String>(playerNames);
    comboBoxAlreadyEnabled = new JComboBox<String>(selectionTrueFalse);

    Dimension dimension = textFieldTechnologyName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    textFieldTechnologyName.setPreferredSize(dimension);
    textFieldTechnologyName.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldTechnologyName.getText().trim();
        final String curr_playerName = (String) comboBoxPlayerName.getSelectedItem();
        if (currentRowName.startsWith(inputText + "_"))
          return;
        final String newRowName = inputText + "_" + curr_playerName;
        if (MapXmlHelper.getTechnologyDefinitionsMap().containsKey(newRowName)) {
          JOptionPane.showMessageDialog(stepActionPanel,
              "Technology '" + inputText + "' already exists for player '" + curr_playerName + "'.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              textFieldTechnologyName.requestFocus();
              textFieldTechnologyName.selectAll();
            }
          });
          return;
        }
        // everything is okay with the new technology name, lets rename everything
        final List<String> newValues = MapXmlHelper.getTechnologyDefinitionsMap().get(currentRowName);
        MapXmlHelper.getTechnologyDefinitionsMap().remove(currentRowName);
        MapXmlHelper.getTechnologyDefinitionsMap().put(newRowName, newValues);
        currentRowName = newRowName;
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldTechnologyName.selectAll();
      }
    });

    dimension = comboBoxPlayerName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    comboBoxPlayerName.setPreferredSize(dimension);
    comboBoxPlayerName.setSelectedIndex(Arrays.binarySearch(playerNames, playerName));
    comboBoxPlayerName.addFocusListener(new FocusListener() {
      int prevSelectedIndex = comboBoxPlayerName.getSelectedIndex();

      @Override
      public void focusLost(FocusEvent arg0) {
        if (prevSelectedIndex == comboBoxPlayerName.getSelectedIndex())
          return;
        String techInputText = textFieldTechnologyName.getText().trim();
        final String curr_playerName = (String) comboBoxPlayerName.getSelectedItem();
        if (currentRowName.endsWith("_" + curr_playerName))
          return;
        final String newRowName = techInputText + "_" + curr_playerName;
        if (MapXmlHelper.getTechnologyDefinitionsMap().containsKey(newRowName)) {
          JOptionPane.showMessageDialog(stepActionPanel,
              "Technology '" + techInputText + "' already exists for player '" + curr_playerName + "'.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              comboBoxPlayerName.setSelectedIndex(prevSelectedIndex);
              comboBoxPlayerName.requestFocus();
            }
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

      @Override
      public void focusGained(FocusEvent arg0) {}
    });

    dimension = comboBoxAlreadyEnabled.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    comboBoxAlreadyEnabled.setPreferredSize(dimension);
    comboBoxAlreadyEnabled.setSelectedIndex(Arrays.binarySearch(selectionTrueFalse, alreadyEnabled));
    comboBoxAlreadyEnabled.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent arg0) {
        // everything is okay with the new technology name, lets rename everything
        final List<String> newValues = MapXmlHelper.getTechnologyDefinitionsMap().get(currentRowName);
        newValues.set(1, (String) comboBoxAlreadyEnabled.getSelectedItem());
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(textFieldTechnologyName);
    componentList.add(comboBoxPlayerName);
    componentList.add(comboBoxAlreadyEnabled);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(textFieldTechnologyName, gbc_template);

    final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
    gbc_tClassName.gridx = 1;
    parent.add(comboBoxPlayerName, gbc_tClassName);

    final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
    gbc_tDisplayName.gridx = 2;
    parent.add(comboBoxAlreadyEnabled, gbc_tDisplayName);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
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
