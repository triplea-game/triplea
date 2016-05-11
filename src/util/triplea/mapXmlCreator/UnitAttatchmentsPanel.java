package util.triplea.mapXmlCreator;

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


public class UnitAttachmentsPanel extends DynamicRowsPanel {

  private String unitName;

  public UnitAttachmentsPanel(final JPanel stepActionPanel, final String unitName) {
    super(stepActionPanel);
    this.unitName = unitName;
  }

  public static void layout(final MapXmlCreator mapXmlCreator, final JPanel stepActionPanel, final String unitName) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof UnitAttachmentsPanel)
        || ((UnitAttachmentsPanel) me.get()).unitName != unitName)
      me = Optional.of(new UnitAttachmentsPanel(stepActionPanel, unitName));
    DynamicRowsPanel.layout(mapXmlCreator);
  }

  protected ActionListener getAutoFillAction() {
    return null;
  }

  protected void layoutComponents() {
    final ArrayList<ArrayList<String>> unitAttachments = new ArrayList<ArrayList<String>>();
    for (final Entry<String, List<String>> unitAttachmentEntry : MapXmlHelper.getUnitAttachmentsMap().entrySet()) {
      final String unitAttatmentKey = unitAttachmentEntry.getKey();
      if (unitAttatmentKey.endsWith("_" + unitName)) {
        final ArrayList<String> newAttachment = new ArrayList<String>();
        newAttachment.add(unitAttatmentKey.substring(0, unitAttatmentKey.lastIndexOf("_" + unitName)));
        newAttachment.add(unitAttachmentEntry.getValue().get(1));
        unitAttachments.add(newAttachment);
      }
    }

    final JLabel labelAttachmentName = new JLabel("Attachment Name");
    Dimension dimension = labelAttachmentName.getPreferredSize();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
    labelAttachmentName.setPreferredSize(dimension);

    final JLabel labelValue = new JLabel("Value");
    dimension = (Dimension) dimension.clone();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_SMALL;
    labelValue.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, unitAttachments.size());
    getOwnPanel().setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Unit Name, Alliance Name, Buy Quantity
    GridBagConstraints gridBadConstLabelAttachmentName = new GridBagConstraints();
    gridBadConstLabelAttachmentName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelAttachmentName.gridy = 0;
    gridBadConstLabelAttachmentName.gridx = 0;
    gridBadConstLabelAttachmentName.anchor = GridBagConstraints.WEST;
    getOwnPanel().add(labelAttachmentName, gridBadConstLabelAttachmentName);


    GridBagConstraints gridBadConstLabelValue = (GridBagConstraints) gridBadConstLabelAttachmentName.clone();
    gridBadConstLabelValue.gridx = 1;
    dimension = (Dimension) dimension.clone();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_SMALL;
    getOwnPanel().add(labelValue, gridBadConstLabelValue);

    // <3> Add Main Input Rows
    int yValue = 1;
    for (final ArrayList<String> unitAttachment : unitAttachments) {
      GridBagConstraints gbc_tAttachmentName = (GridBagConstraints) gridBadConstLabelAttachmentName.clone();
      gbc_tAttachmentName.gridx = 0;
      gridBadConstLabelAttachmentName.gridy = yValue;
      final UnitAttachmentsRow newRow =
          new UnitAttachmentsRow(this, getOwnPanel(), unitName, unitAttachment.get(0), unitAttachment.get(1));
      newRow.addToParentComponentWithGbc(getOwnPanel(), yValue, gbc_tAttachmentName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton buttonAddAttachment = new JButton("Add Attachment");

    buttonAddAttachment.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonAddAttachment.addActionListener(SwingAction.of("Add Attachment", e -> {
      String newAttachmentName = JOptionPane.showInputDialog(getOwnPanel(), "Enter a new attachment name:",
          "Attachment" + (rows.size() + 1));
      if (newAttachmentName == null || newAttachmentName.isEmpty())
        return;
      newAttachmentName = newAttachmentName.trim();
      final String newUnitAttachmentKey = newAttachmentName + "_" + unitName;
      if (MapXmlHelper.getUnitAttachmentsMap().containsKey(newUnitAttachmentKey)) {
        JOptionPane.showMessageDialog(getOwnPanel(),
            "Attachment '" + newAttachmentName + "' already exists for unit '" + unitName + "'.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      final ArrayList<String> unitAttachment = new ArrayList<String>();
      unitAttachment.add(unitName);
      unitAttachment.add("");
      MapXmlHelper.putUnitAttachments(newUnitAttachmentKey, unitAttachment);

      // UI Update
      setRows((GridBagLayout) getOwnPanel().getLayout(), rows.size() + 1);
      addRowWith(newAttachmentName, "");
      SwingUtilities.invokeLater(() -> {
        getOwnPanel().revalidate();
        getOwnPanel().repaint();
      });
    }));
    addButton(buttonAddAttachment);

    GridBagConstraints gridBadConstButtonAddUnit = (GridBagConstraints) gridBadConstLabelAttachmentName.clone();
    gridBadConstButtonAddUnit.gridx = 0;
    gridBadConstButtonAddUnit.gridy = yValue;
    addFinalButtonRow(gridBadConstButtonAddUnit);
  }

  private DynamicRow addRowWith(final String newAttachmentName, final String value) {
    final UnitAttachmentsRow newRow = new UnitAttachmentsRow(this, getOwnPanel(), unitName, newAttachmentName, value);
    addRow(newRow);
    return newRow;
  }


  protected void initializeSpecifics() {}

  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 30, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0};
  }
}
