package util.triplea.MapXMLCreator;

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
  private JTextField tTechnologyName;
  private JComboBox<String> tPlayerName;
  private JComboBox<String> tAlreadyEnabled;
  public static String[] selectionTrueFalse = {"false", "true"};

  public TechnologyDefinitionsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel,
      final String technologyName, final String playerName, final String[] playerNames, final String alreadyEnabled) {
    super(technologyName + "_" + playerName, parentRowPanel, stepActionPanel);

    tTechnologyName = new JTextField(technologyName);
    tPlayerName = new JComboBox<String>(playerNames);
    tAlreadyEnabled = new JComboBox<String>(selectionTrueFalse);

    Dimension dimension = tTechnologyName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    tTechnologyName.setPreferredSize(dimension);
    tTechnologyName.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tTechnologyName.getText().trim();
        final String curr_playerName = (String) tPlayerName.getSelectedItem();
        if (currentRowName.startsWith(inputText + "_"))
          return;
        final String newRowName = inputText + "_" + curr_playerName;
        if (MapXMLHelper.technologyDefinitions.containsKey(newRowName)) {
          JOptionPane.showMessageDialog(stepActionPanel,
              "Technology '" + inputText + "' already exists for player '" + curr_playerName + "'.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tTechnologyName.requestFocus();
              tTechnologyName.selectAll();
            }
          });
          return;
        }
        // everything is okay with the new technology name, lets rename everything
        final List<String> newValues = MapXMLHelper.technologyDefinitions.get(currentRowName);
        MapXMLHelper.technologyDefinitions.remove(currentRowName);
        MapXMLHelper.technologyDefinitions.put(newRowName, newValues);
        currentRowName = newRowName;
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tTechnologyName.selectAll();
      }
    });

    dimension = tPlayerName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    tPlayerName.setPreferredSize(dimension);
    tPlayerName.setSelectedIndex(Arrays.binarySearch(playerNames, playerName));
    tPlayerName.addFocusListener(new FocusListener() {
      int prevSelectedIndex = tPlayerName.getSelectedIndex();

      @Override
      public void focusLost(FocusEvent arg0) {
        if (prevSelectedIndex == tPlayerName.getSelectedIndex())
          return;
        String techInputText = tTechnologyName.getText().trim();
        final String curr_playerName = (String) tPlayerName.getSelectedItem();
        if (currentRowName.endsWith("_" + curr_playerName))
          return;
        final String newRowName = techInputText + "_" + curr_playerName;
        if (MapXMLHelper.technologyDefinitions.containsKey(newRowName)) {
          JOptionPane.showMessageDialog(stepActionPanel,
              "Technology '" + techInputText + "' already exists for player '" + curr_playerName + "'.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tPlayerName.setSelectedIndex(prevSelectedIndex);
              tPlayerName.requestFocus();
            }
          });
          return;
        }
        // everything is okay with the new technology name, lets rename everything
        final List<String> newValues = MapXMLHelper.technologyDefinitions.get(currentRowName);
        MapXMLHelper.technologyDefinitions.remove(currentRowName);
        MapXMLHelper.technologyDefinitions.put(newRowName, newValues);
        currentRowName = newRowName;
        prevSelectedIndex = tPlayerName.getSelectedIndex();
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });

    dimension = tAlreadyEnabled.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tAlreadyEnabled.setPreferredSize(dimension);
    tAlreadyEnabled.setSelectedIndex(Arrays.binarySearch(selectionTrueFalse, alreadyEnabled));
    tAlreadyEnabled.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent arg0) {
        // everything is okay with the new technology name, lets rename everything
        final List<String> newValues = MapXMLHelper.technologyDefinitions.get(currentRowName);
        newValues.set(1, (String) tAlreadyEnabled.getSelectedItem());
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(tTechnologyName);
    componentList.add(tPlayerName);
    componentList.add(tAlreadyEnabled);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(tTechnologyName, gbc_template);

    final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
    gbc_tClassName.gridx = 1;
    parent.add(tPlayerName, gbc_tClassName);

    final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
    gbc_tDisplayName.gridx = 2;
    parent.add(tAlreadyEnabled, gbc_tDisplayName);

    final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
    gbc_bRemove.gridx = 3;
    parent.add(bRemoveRow, gbc_bRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final TechnologyDefinitionsRow newTechnologyDefinitionsRow = (TechnologyDefinitionsRow) newRow;
    this.tTechnologyName.setText(newTechnologyDefinitionsRow.tTechnologyName.getText());
    this.tPlayerName.setSelectedIndex(newTechnologyDefinitionsRow.tPlayerName.getSelectedIndex());
    this.tAlreadyEnabled.setSelectedIndex(newTechnologyDefinitionsRow.tAlreadyEnabled.getSelectedIndex());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.technologyDefinitions.remove(currentRowName);
  }
}
