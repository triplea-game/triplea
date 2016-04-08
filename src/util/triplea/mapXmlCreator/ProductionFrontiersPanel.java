package util.triplea.mapXmlCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.common.swing.SwingAction;


public class ProductionFrontiersPanel extends DynamicRowsPanel {

  private final String playerName;
  private final TreeSet<String> allUnitNames;

  public ProductionFrontiersPanel(final JPanel stepActionPanel, final String playerName) {
    super(stepActionPanel);
    this.playerName = playerName;
    allUnitNames = new TreeSet<String>(MapXmlHelper.getUnitDefinitionsMap().keySet());
  }

  public static void layout(final MapXmlCreator mapXMLCreator, final JPanel stepActionPanel, final String playerName) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof ProductionFrontiersPanel)
        || ((ProductionFrontiersPanel) me.get()).playerName != playerName) {
      me = Optional.of(new ProductionFrontiersPanel(stepActionPanel, playerName));
    }
    DynamicRowsPanel.layout(mapXMLCreator);
  }

  @Override
  protected ActionListener getAutoFillAction() {
    return new ActionListener() {

      @Override
      public void actionPerformed(final ActionEvent e) {

        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(getOwnPanel(),
            "Are you sure you want to use the  Auto-Fill feature?\rIt will remove any information you have entered in this step and propose commonly used choices.",
            "Auto-Fill Overwrite Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
          MapXmlHelper.clearProductionFrontiers();
          for (final String playerName : MapXmlHelper.getPlayerNames()) {
            MapXmlHelper.putProductionFrontiers(playerName, new ArrayList<String>(allUnitNames));
          }
          // Update UI
          repaintOwnPanel();
        }
      }
    };
  }

  @Override
  protected void layoutComponents() {
    final List<String> playersUnitNames = MapXmlHelper.getProductionFrontiersMap().get(playerName);
    if (playersUnitNames == null) {
      return;
    }

    final JLabel labelUnitName = new JLabel("Unit Name");
    final Dimension dimension = labelUnitName.getPreferredSize();
    dimension.width = 140;
    labelUnitName.setPreferredSize(dimension);

    // <1> Set panel layout
    final GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, playersUnitNames.size());
    getOwnPanel().setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
    final GridBagConstraints gridBadConstLabelUnitName = new GridBagConstraints();
    gridBadConstLabelUnitName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelUnitName.gridy = 0;
    gridBadConstLabelUnitName.gridx = 0;
    gridBadConstLabelUnitName.anchor = GridBagConstraints.WEST;
    getOwnPanel().add(labelUnitName, gridBadConstLabelUnitName);

    // <3> Add Main Input Rows
    int yValue = 1;
    final String[] allUnitNamesArray = allUnitNames.toArray(new String[allUnitNames.size()]);
    for (final String unitName : playersUnitNames) {
      final GridBagConstraints gbc_tUnitName = (GridBagConstraints) gridBadConstLabelUnitName.clone();
      gbc_tUnitName.gridx = 0;
      gridBadConstLabelUnitName.gridy = yValue;
      final ProductionFrontiersRow newRow =
          new ProductionFrontiersRow(this, getOwnPanel(), playerName, unitName, allUnitNamesArray);
      newRow.addToParentComponentWithGbc(getOwnPanel(), yValue, gbc_tUnitName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton buttonAddUnit = new JButton("Add Unit");

    buttonAddUnit.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonAddUnit.addActionListener(SwingAction.of("Add Unit", e -> {
      final List<String> curr_playersUnitNames = MapXmlHelper.getProductionFrontiersMap().get(playerName);

      // UI Update
      setRows((GridBagLayout) getOwnPanel().getLayout(), MapXmlHelper.getUnitDefinitionsMap().size());
      final String[] allUnitNamesArray2 = allUnitNames.toArray(new String[allUnitNames.size()]);
      final HashSet<String> freeUnitNames = new HashSet<String>(allUnitNames);
      freeUnitNames.removeAll(curr_playersUnitNames);
      final String newUnitName = freeUnitNames.iterator().next();
      if (newUnitName == null) {
        JOptionPane.showMessageDialog(getOwnPanel(), "All units already selected.", "Input error",
            JOptionPane.ERROR_MESSAGE);
      } else {
        curr_playersUnitNames.add(newUnitName);
        addRowWith(newUnitName, allUnitNamesArray2);
        SwingUtilities.invokeLater(() -> {
          getOwnPanel().revalidate();
          getOwnPanel().repaint();
        });
      }
    }));
    addButton(buttonAddUnit);

    final GridBagConstraints gridBadConstButtonAddUnit = (GridBagConstraints) gridBadConstLabelUnitName.clone();
    gridBadConstButtonAddUnit.gridx = 0;
    gridBadConstButtonAddUnit.gridy = yValue;
    addFinalButtonRow(gridBadConstButtonAddUnit);
  }

  private DynamicRow addRowWith(final String newUnitName, final String[] unitNames) {
    final ProductionFrontiersRow newRow =
        new ProductionFrontiersRow(this, getOwnPanel(), playerName, newUnitName, unitNames);
    addRow(newRow);
    return newRow;
  }


  @Override
  protected void initializeSpecifics() {}

  @Override
  protected void setColumns(final GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0};
  }
}
