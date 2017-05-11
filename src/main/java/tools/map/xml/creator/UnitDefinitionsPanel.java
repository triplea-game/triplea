package tools.map.xml.creator;

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

import games.strategy.ui.SwingAction;


public class UnitDefinitionsPanel extends DynamicRowsPanel {

  public UnitDefinitionsPanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXmlCreator mapXmlCreator) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof UnitDefinitionsPanel)) {
      me = Optional.of(new UnitDefinitionsPanel(mapXmlCreator.getStepActionPanel()));
    }
    DynamicRowsPanel.layout(mapXmlCreator);
  }

  @Override
  protected ActionListener getAutoFillAction() {
    return null;
  }

  @Override
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
    final GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXmlHelper.getUnitDefinitionsMap().size());
    getOwnPanel().setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
    final GridBagConstraints gridBadConstLabelUnitName = new GridBagConstraints();
    gridBadConstLabelUnitName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelUnitName.gridy = 0;
    gridBadConstLabelUnitName.gridx = 0;
    gridBadConstLabelUnitName.anchor = GridBagConstraints.WEST;
    getOwnPanel().add(labelUnitName, gridBadConstLabelUnitName);

    final GridBagConstraints gridBadConstLabelBuyCost = (GridBagConstraints) gridBadConstLabelUnitName.clone();
    gridBadConstLabelBuyCost.gridx = 1;
    getOwnPanel().add(labelBuyCost, gridBadConstLabelBuyCost);

    final GridBagConstraints gridBadConstLabelBuyQuantity = (GridBagConstraints) gridBadConstLabelUnitName.clone();
    gridBadConstLabelBuyQuantity.gridx = 2;
    getOwnPanel().add(labelBuyQuantity, gridBadConstLabelBuyQuantity);

    // <3> Add Main Input Rows
    int rowIndex = 1;
    for (final Entry<String, List<Integer>> unitDefinition : MapXmlHelper.getUnitDefinitionsMap().entrySet()) {
      final GridBagConstraints gbc_tUnitName = (GridBagConstraints) gridBadConstLabelUnitName.clone();
      gbc_tUnitName.gridx = 0;
      gridBadConstLabelUnitName.gridy = rowIndex;
      final List<Integer> defintionValues = unitDefinition.getValue();
      final UnitDefinitionsRow newRow = new UnitDefinitionsRow(this, getOwnPanel(), unitDefinition.getKey(),
          defintionValues.get(0), defintionValues.get(1));
      newRow.addToParentComponentWithGbc(getOwnPanel(), rowIndex, gbc_tUnitName);
      rows.add(newRow);
      ++rowIndex;
    }

    // <4> Add Final Button Row
    final JButton buttonAddUnit = new JButton("Add Unit");

    buttonAddUnit.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonAddUnit.addActionListener(SwingAction.of("Add Unit", e -> {
      String newUnitName = JOptionPane.showInputDialog(getOwnPanel(), "Enter a new unit name:",
          "Unit" + (MapXmlHelper.getUnitDefinitionsMap().size() + 1));
      if (newUnitName == null || newUnitName.isEmpty()) {
        return;
      }
      if (MapXmlHelper.getUnitDefinitionsMap().containsKey(newUnitName)) {
        JOptionPane.showMessageDialog(getOwnPanel(), "Unit '" + newUnitName + "' already exists.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      newUnitName = newUnitName.trim();

      final ArrayList<Integer> newValue = new ArrayList<>();
      newValue.add(0);
      newValue.add(1);
      MapXmlHelper.putUnitDefinitions(newUnitName, newValue);

      // UI Update
      setRows((GridBagLayout) getOwnPanel().getLayout(), MapXmlHelper.getUnitDefinitionsMap().size());
      addRowWith(newUnitName, 0, 1);
      SwingUtilities.invokeLater(() -> {
        getOwnPanel().revalidate();
        getOwnPanel().repaint();
      });
    }));
    addButton(buttonAddUnit);

    final GridBagConstraints gridBadConstButtonAddUnit = (GridBagConstraints) gridBadConstLabelUnitName.clone();
    gridBadConstButtonAddUnit.gridx = 0;
    gridBadConstButtonAddUnit.gridy = rowIndex;
    addFinalButtonRow(gridBadConstButtonAddUnit);
  }

  private DynamicRow addRowWith(final String newUnitName, final int buyCost, final int buyQuantity) {
    final UnitDefinitionsRow newRow = new UnitDefinitionsRow(this, getOwnPanel(), newUnitName, buyCost, buyQuantity);
    addRow(newRow);
    return newRow;
  }


  @Override
  protected void initializeSpecifics() {}

  @Override
  protected void setColumns(final GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 60, 50, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0};
  }
}
