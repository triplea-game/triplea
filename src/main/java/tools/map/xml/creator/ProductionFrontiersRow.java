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
import javax.swing.SwingUtilities;


class ProductionFrontiersRow extends DynamicRow {

  private final JComboBox<String> comboBoxUnitName;
  private final String playerName;

  public ProductionFrontiersRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel,
      final String playerName, final String unitName, final String[] unitNames) {
    super(unitName, parentRowPanel, stepActionPanel);

    this.playerName = playerName;

    comboBoxUnitName = new JComboBox<>(unitNames);
    final Dimension dimension = comboBoxUnitName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    comboBoxUnitName.setPreferredSize(dimension);
    comboBoxUnitName.setSelectedIndex(Arrays.binarySearch(unitNames, unitName));
    comboBoxUnitName.addFocusListener(new FocusListenerFocusLost() {

      String currentValue = unitName;

      @Override
      public void focusLost(final FocusEvent e) {
        final String newUnitValue = (String) comboBoxUnitName.getSelectedItem();
        if (!currentValue.equals(newUnitValue)) {
          final List<String> playerUnitNames = MapXmlHelper.getProductionFrontiersMap().get(playerName);
          if (playerUnitNames.contains(newUnitValue)) {
            JOptionPane.showMessageDialog(stepActionPanel,
                "Unit '" + newUnitValue + "' already selected fpr player '" + playerName + "'.", "Input error",
                JOptionPane.ERROR_MESSAGE);
            comboBoxUnitName.setSelectedItem(currentValue);
            // UI Update
            SwingUtilities.invokeLater(() -> {
              stepActionPanel.revalidate();
              stepActionPanel.repaint();
            });
          } else {
            playerUnitNames.add(newUnitValue);
            currentValue = newUnitValue;
            currentRowName = unitName;
          }
        }
      }
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<>();
    componentList.add(comboBoxUnitName);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbcTemplate) {
    parent.add(comboBoxUnitName, gbcTemplate);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbcTemplate.clone();
    gridBadConstButtonRemove.gridx = 1;
    parent.add(buttonRemovePerRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final ProductionFrontiersRow newRowPlayerAndAlliancesRow = (ProductionFrontiersRow) newRow;
    this.comboBoxUnitName.setSelectedIndex(newRowPlayerAndAlliancesRow.comboBoxUnitName.getSelectedIndex());
  }

  @Override
  protected void removeRowAction() {
    MapXmlHelper.getProductionFrontiersMap().get(playerName).remove(currentRowName);
  }
}
