package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.common.swing.SwingAction;


public class UnitDefinitionsPanel extends DynamicRowsPanel {

  public UnitDefinitionsPanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof UnitDefinitionsPanel))
      me = Optional.of(new UnitDefinitionsPanel(stepActionPanel));
    DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
  }

  protected ActionListener getAutoFillAction() {
    return null;
  }

  protected void layoutComponents() {

    final JLabel labelUnitName = new JLabel("Unit Name");
    Dimension dimension = labelUnitName.getPreferredSize();
    dimension.width = 140;
    labelUnitName.setPreferredSize(dimension);
    final JLabel labelBuyCost = new JLabel("Buy Cost");
    dimension = (Dimension) dimension.clone();
    dimension.width = 80;
    labelBuyCost.setPreferredSize(dimension);
    final JLabel labelBuyQuantity = new JLabel("Buy Quantity");
    labelBuyQuantity.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXMLHelper.unitDefinitions.size());
    ownPanel.setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
    GridBagConstraints gridBadConstLabelUnitName = new GridBagConstraints();
    gridBadConstLabelUnitName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelUnitName.gridy = 0;
    gridBadConstLabelUnitName.gridx = 0;
    gridBadConstLabelUnitName.anchor = GridBagConstraints.WEST;
    ownPanel.add(labelUnitName, gridBadConstLabelUnitName);

    GridBagConstraints gridBadConstLabelBuyCost = (GridBagConstraints) gridBadConstLabelUnitName.clone();
    gridBadConstLabelBuyCost.gridx = 1;
    ownPanel.add(labelBuyCost, gridBadConstLabelBuyCost);

    GridBagConstraints gridBadConstLabelBuyQuantity = (GridBagConstraints) gridBadConstLabelUnitName.clone();
    gridBadConstLabelBuyQuantity.gridx = 2;
    ownPanel.add(labelBuyQuantity, gridBadConstLabelBuyQuantity);

    // <3> Add Main Input Rows
    int yValue = 1;
    for (final Entry<String, List<Integer>> unitDefinition : MapXMLHelper.unitDefinitions.entrySet()) {
      GridBagConstraints gbc_tUnitName = (GridBagConstraints) gridBadConstLabelUnitName.clone();
      gbc_tUnitName.gridx = 0;
      gridBadConstLabelUnitName.gridy = yValue;
      final List<Integer> defintionValues = unitDefinition.getValue();
      final UnitDefinitionsRow newRow = new UnitDefinitionsRow(this, ownPanel, unitDefinition.getKey(),
          defintionValues.get(0), defintionValues.get(1));
      newRow.addToComponent(ownPanel, yValue, gbc_tUnitName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton buttonAddUnit = new JButton("Add Unit");

    buttonAddUnit.setFont(MapXMLHelper.defaultMapXMLCreatorFont);
    buttonAddUnit.addActionListener(SwingAction.of("Add Unit", e -> {
      String newUnitName = JOptionPane.showInputDialog(ownPanel, "Enter a new unit name:",
          "Unit" + (MapXMLHelper.unitDefinitions.size() + 1));
      if (newUnitName == null || newUnitName.isEmpty())
        return;
      if (MapXMLHelper.unitDefinitions.containsKey(newUnitName)) {
        JOptionPane.showMessageDialog(ownPanel, "Unit '" + newUnitName + "' already exists.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      newUnitName = newUnitName.trim();

      final ArrayList<Integer> newValue = new ArrayList<Integer>();
      newValue.add(0);
      newValue.add(1);
      MapXMLHelper.putUnitDefinitions(newUnitName, newValue);

      // UI Update
      setRows((GridBagLayout) ownPanel.getLayout(), MapXMLHelper.unitDefinitions.size());
      addRowWith(newUnitName, 0, 1);
      SwingUtilities.invokeLater(() -> {
        ownPanel.revalidate();
        ownPanel.repaint();
      });
    }));
    addButton(buttonAddUnit);

    GridBagConstraints gridBadConstButtonAddUnit = (GridBagConstraints) gridBadConstLabelUnitName.clone();
    gridBadConstButtonAddUnit.gridx = 0;
    gridBadConstButtonAddUnit.gridy = yValue;
    addFinalButtonRow(gridBadConstButtonAddUnit);
  }

  private DynamicRow addRowWith(final String newUnitName, final int buyCost, final int buyQuantity) {
    final UnitDefinitionsRow newRow = new UnitDefinitionsRow(this, ownPanel, newUnitName, buyCost, buyQuantity);
    addRow(newRow);
    return newRow;
  }


  protected void initializeSpecifics() {}

  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 60, 50, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0};
  }
}
