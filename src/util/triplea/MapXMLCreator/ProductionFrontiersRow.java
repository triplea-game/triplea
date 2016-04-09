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
import javax.swing.SwingUtilities;


class ProductionFrontiersRow extends DynamicRow {

  private JComboBox<String> tUnitName;
  private String playerName;

  public ProductionFrontiersRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel,
      final String playerName, final String unitName, final String[] unitNames) {
    super(unitName, parentRowPanel, stepActionPanel);

    this.playerName = playerName;

    tUnitName = new JComboBox<String>(unitNames);
    Dimension dimension = tUnitName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    tUnitName.setPreferredSize(dimension);
    tUnitName.setSelectedIndex(Arrays.binarySearch(unitNames, unitName));
    tUnitName.addFocusListener(new FocusListener() {

      String currentValue = unitName;

      @Override
      public void focusLost(FocusEvent e) {
        String newUnitValue = (String) tUnitName.getSelectedItem();
        if (!currentValue.equals(newUnitValue)) {
          final List<String> playerUnitNames = MapXMLHelper.productionFrontiers.get(playerName);
          if (playerUnitNames.contains(newUnitValue)) {
            JOptionPane.showMessageDialog(stepActionPanel,
                "Unit '" + newUnitValue + "' already selected fpr player '" + playerName + "'.", "Input error",
                JOptionPane.ERROR_MESSAGE);
            tUnitName.setSelectedItem(currentValue);
            // UI Update
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                stepActionPanel.revalidate();
                stepActionPanel.repaint();
              }
            });
          } else {
            playerUnitNames.add(newUnitValue);
            currentValue = newUnitValue;
            currentRowName = unitName;
          }
        }
      }

      @Override
      public void focusGained(FocusEvent e) {}
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(tUnitName);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(tUnitName, gbc_template);

    final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
    gbc_bRemove.gridx = 1;
    parent.add(bRemoveRow, gbc_bRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final ProductionFrontiersRow newRowPlayerAndAlliancesRow = (ProductionFrontiersRow) newRow;
    this.tUnitName.setSelectedIndex(newRowPlayerAndAlliancesRow.tUnitName.getSelectedIndex());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.productionFrontiers.get(playerName).remove(currentRowName);
  }
}
