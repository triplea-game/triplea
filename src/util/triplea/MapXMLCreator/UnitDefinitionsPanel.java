package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class UnitDefinitionsPanel extends DynamicRowsPanel {

  public UnitDefinitionsPanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    if (me == null || !(me instanceof UnitDefinitionsPanel))
      me = new UnitDefinitionsPanel(stepActionPanel);
    DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
  }

  protected ActionListener getAutoFillAction() {
    return null;
  }

  protected void layoutComponents() {

    final JLabel lUnitName = new JLabel("Unit Name");
    Dimension dimension = lUnitName.getPreferredSize();
    dimension.width = 140;
    lUnitName.setPreferredSize(dimension);
    final JLabel lBuyCost = new JLabel("Buy Cost");
    dimension = (Dimension) dimension.clone();
    dimension.width = 80;
    lBuyCost.setPreferredSize(dimension);
    final JLabel lBuyQuantity = new JLabel("Buy Quantity");
    lBuyQuantity.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXMLHelper.unitDefinitions.size());
    ownPanel.setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
    GridBagConstraints gbc_lUnitName = new GridBagConstraints();
    gbc_lUnitName.insets = new Insets(0, 0, 5, 5);
    gbc_lUnitName.gridy = 0;
    gbc_lUnitName.gridx = 0;
    gbc_lUnitName.anchor = GridBagConstraints.WEST;
    ownPanel.add(lUnitName, gbc_lUnitName);

    GridBagConstraints gbc_lBuyCost = (GridBagConstraints) gbc_lUnitName.clone();
    gbc_lBuyCost.gridx = 1;
    ownPanel.add(lBuyCost, gbc_lBuyCost);

    GridBagConstraints gbc_lBuyQuantity = (GridBagConstraints) gbc_lUnitName.clone();
    gbc_lBuyQuantity.gridx = 2;
    ownPanel.add(lBuyQuantity, gbc_lBuyQuantity);

    // <3> Add Main Input Rows
    int yValue = 1;
    for (final Entry<String, List<Integer>> unitDefinition : MapXMLHelper.unitDefinitions.entrySet()) {
      GridBagConstraints gbc_tUnitName = (GridBagConstraints) gbc_lUnitName.clone();
      gbc_tUnitName.gridx = 0;
      gbc_lUnitName.gridy = yValue;
      final List<Integer> defintionValues = unitDefinition.getValue();
      final UnitDefinitionsRow newRow = new UnitDefinitionsRow(this, ownPanel, unitDefinition.getKey(),
          defintionValues.get(0), defintionValues.get(1));
      newRow.addToComponent(ownPanel, yValue, gbc_tUnitName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton bAddUnit = new JButton("Add Unit");

    bAddUnit.setFont(new Font("Tahoma", Font.PLAIN, 11));
    bAddUnit.addActionListener(new AbstractAction("Add Unit") {
      private static final long serialVersionUID = 6322566373692205163L;

      public void actionPerformed(final ActionEvent e) {
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
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ownPanel.revalidate();
            ownPanel.repaint();
          }
        });
      }
    });
    addButton(bAddUnit);

    GridBagConstraints gbc_bAddUnit = (GridBagConstraints) gbc_lUnitName.clone();
    gbc_bAddUnit.gridx = 0;
    gbc_bAddUnit.gridy = yValue;
    addFinalButtonRow(gbc_bAddUnit);
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
