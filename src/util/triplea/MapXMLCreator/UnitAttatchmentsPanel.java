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


public class UnitAttatchmentsPanel extends DynamicRowsPanel {

  private String unitName;

  public UnitAttatchmentsPanel(final JPanel stepActionPanel, final String unitName) {
    super(stepActionPanel);
    this.unitName = unitName;
  }

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel, final String unitName) {
    if (me == null || !(me instanceof UnitAttatchmentsPanel)
        || ((UnitAttatchmentsPanel) me).unitName != unitName)
      me = new UnitAttatchmentsPanel(stepActionPanel, unitName);
    DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
  }

  protected ActionListener getAutoFillAction() {
    return null;
  }

  protected void layoutComponents() {
    final ArrayList<ArrayList<String>> unitAttatchments = new ArrayList<ArrayList<String>>();
    for (final Entry<String, List<String>> unitAttatchmentEntry : MapXMLHelper.unitAttatchments.entrySet()) {
      final String unitAttatmentKey = unitAttatchmentEntry.getKey();
      if (unitAttatmentKey.endsWith("_" + unitName)) {
        final ArrayList<String> newAttachment = new ArrayList<String>();
        newAttachment.add(unitAttatmentKey.substring(0, unitAttatmentKey.lastIndexOf("_" + unitName)));
        newAttachment.add(unitAttatchmentEntry.getValue().get(1));
        unitAttatchments.add(newAttachment);
      }
    }

    final JLabel lAttatchmentName = new JLabel("Attatchment Name");
    Dimension dimension = lAttatchmentName.getPreferredSize();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
    lAttatchmentName.setPreferredSize(dimension);

    final JLabel lValue = new JLabel("Value");
    dimension = (Dimension) dimension.clone();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_SMALL;
    lValue.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, unitAttatchments.size());
    ownPanel.setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Unit Name, Alliance Name, Buy Quantity
    GridBagConstraints gbc_lAttatchmentName = new GridBagConstraints();
    gbc_lAttatchmentName.insets = new Insets(0, 0, 5, 5);
    gbc_lAttatchmentName.gridy = 0;
    gbc_lAttatchmentName.gridx = 0;
    gbc_lAttatchmentName.anchor = GridBagConstraints.WEST;
    ownPanel.add(lAttatchmentName, gbc_lAttatchmentName);


    GridBagConstraints gbc_lValue = (GridBagConstraints) gbc_lAttatchmentName.clone();
    gbc_lValue.gridx = 1;
    dimension = (Dimension) dimension.clone();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_SMALL;
    ownPanel.add(lValue, gbc_lValue);

    // <3> Add Main Input Rows
    int yValue = 1;
    for (final ArrayList<String> unitAttatchment : unitAttatchments) {
      GridBagConstraints gbc_tAttatchmentName = (GridBagConstraints) gbc_lAttatchmentName.clone();
      gbc_tAttatchmentName.gridx = 0;
      gbc_lAttatchmentName.gridy = yValue;
      final UnitAttatchmentsRow newRow =
          new UnitAttatchmentsRow(this, ownPanel, unitName, unitAttatchment.get(0), unitAttatchment.get(1));
      newRow.addToComponent(ownPanel, yValue, gbc_tAttatchmentName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton bAddAttatchment = new JButton("Add Attatchment");

    bAddAttatchment.setFont(new Font("Tahoma", Font.PLAIN, 11));
    bAddAttatchment.addActionListener(new AbstractAction("Add Attatchment") {
      private static final long serialVersionUID = 6322566373692205163L;

      public void actionPerformed(final ActionEvent e) {
        String newAttatchmentName = JOptionPane.showInputDialog(ownPanel, "Enter a new attatchment name:",
            "Attatchment" + (rows.size() + 1));
        if (newAttatchmentName == null || newAttatchmentName.isEmpty())
          return;
        newAttatchmentName = newAttatchmentName.trim();
        final String newUnitAttatchmentKey = newAttatchmentName + "_" + unitName;
        if (MapXMLHelper.unitAttatchments.containsKey(newUnitAttatchmentKey)) {
          JOptionPane.showMessageDialog(ownPanel,
              "Attatchment '" + newAttatchmentName + "' already exists for unit '" + unitName + "'.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        final ArrayList<String> unitAttatchment = new ArrayList<String>();
        unitAttatchment.add(unitName);
        unitAttatchment.add("");
        MapXMLHelper.putUnitAttatchments(newUnitAttatchmentKey, unitAttatchment);

        // UI Update
        setRows((GridBagLayout) ownPanel.getLayout(), rows.size() + 1);
        addRowWith(newAttatchmentName, "");
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ownPanel.revalidate();
            ownPanel.repaint();
          }
        });
      }
    });
    addButton(bAddAttatchment);

    GridBagConstraints gbc_bAddUnit = (GridBagConstraints) gbc_lAttatchmentName.clone();
    gbc_bAddUnit.gridx = 0;
    gbc_bAddUnit.gridy = yValue;
    addFinalButtonRow(gbc_bAddUnit);
  }

  private DynamicRow addRowWith(final String newAttatchmentName, final String value) {
    final UnitAttatchmentsRow newRow = new UnitAttatchmentsRow(this, ownPanel, unitName, newAttatchmentName, value);
    addRow(newRow);
    return newRow;
  }


  protected void initializeSpecifics() {}

  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 30, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0};
  }
}
