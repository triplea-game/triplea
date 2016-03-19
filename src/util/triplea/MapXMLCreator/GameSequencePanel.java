package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class GameSequencePanel extends DynamicRowsPanel {

  public GameSequencePanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof GameSequencePanel))
      me = Optional.of(new GameSequencePanel(stepActionPanel));
    DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
  }

  protected ActionListener getAutoFillAction() {
    return new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(ownPanel,
            "Are you sure you want to use the  Auto-Fill feature?\rIt will remove any information you have entered in this step and propose commonly used choices.",
            "Auto-Fill Overwrite Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
          MapXMLHelper.clearGamePlaySequence();
          MapXMLHelper.gamePlaySequence.put("bid",
              Arrays.asList("BidPurchaseDelegate", "Bid Purchase"));
          MapXMLHelper.gamePlaySequence.put("placeBid",
              Arrays.asList("BidPlaceDelegate", "Bid Placement"));
          MapXMLHelper.gamePlaySequence.put("tech",
              Arrays.asList("TechnologyDelegate", "Research Technology"));
          MapXMLHelper.gamePlaySequence.put("tech_Activation",
              Arrays.asList("TechActivationDelegate", "Activate Technology"));
          MapXMLHelper.gamePlaySequence.put("purchase",
              Arrays.asList("PurchaseDelegate", "Purchase Units"));
          MapXMLHelper.gamePlaySequence.put("move",
              Arrays.asList("MoveDelegate", "Combat Move"));
          MapXMLHelper.gamePlaySequence.put("battle",
              Arrays.asList("BattleDelegate", "Combat"));
          MapXMLHelper.gamePlaySequence.put("place",
              Arrays.asList("PlaceDelegate", "Place Units"));
          MapXMLHelper.gamePlaySequence.put("endTurn",
              Arrays.asList("BidPurchaseDelegate", "Turn Complete"));
          // Update UI
          SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
              resetRows();
              ownPanel.revalidate();
              ownPanel.repaint();
              ownPanel.requestFocus();
            }
          });
        }
      }
    };
  }

  protected void layoutComponents() {

    setOwnPanelLayout();

    GridBagConstraints gbcDefault = getDefaultGBC();

    addLabelsRow(gbcDefault);

    // Add main input rows
    int yValue = 1;
    for (final Entry<String, List<String>> entry : MapXMLHelper.gamePlaySequence.entrySet()) {
      addMainInputRow(gbcDefault, yValue, entry);
      ++yValue;
    }

    addAddSequenceButton();

    addFinalButtonRow(MapXMLHelper.getGBCCloneWith(gbcDefault, 0, yValue));
  }

  /**
   * @return default GridBagConstraints object
   */
  protected GridBagConstraints getDefaultGBC() {
    GridBagConstraints gridBadConstLabelSequenceName = new GridBagConstraints();
    gridBadConstLabelSequenceName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelSequenceName.gridy = 0;
    gridBadConstLabelSequenceName.gridx = 0;
    gridBadConstLabelSequenceName.anchor = GridBagConstraints.WEST;
    return gridBadConstLabelSequenceName;
  }

  /**
   * @param gridBadConstLabelSequenceName GridBagConstraints object for "Sequence Name" label and default for other
   *        labels
   */
  private void addLabelsRow(final GridBagConstraints gridBadConstLabelSequenceName) {
    final JLabel labelSequenceName = new JLabel("Sequence Name");
    Dimension dimension = labelSequenceName.getPreferredSize();
    dimension.width = 140;
    labelSequenceName.setPreferredSize(dimension);
    ownPanel.add(labelSequenceName, gridBadConstLabelSequenceName);

    final JLabel labelClassName = new JLabel("Class Name");
    labelClassName.setPreferredSize(dimension);
    ownPanel.add(labelClassName, MapXMLHelper.getGBCCloneWith(gridBadConstLabelSequenceName, 1, 0));

    final JLabel labelDisplayName = new JLabel("Display Name");
    labelDisplayName.setPreferredSize(dimension);
    ownPanel.add(labelDisplayName, MapXMLHelper.getGBCCloneWith(gridBadConstLabelSequenceName, 2, 0));
  }

  /**
   * @param gbcBase
   * @param rowIndex
   * @param rowEntry
   */
  private void addMainInputRow(GridBagConstraints gbcBase, int rowIndex,
      final Entry<String, List<String>> rowEntry) {
    final List<String> defintionValues = rowEntry.getValue();
    final GameSequenceRow newRow =
        new GameSequenceRow(this, ownPanel, rowEntry.getKey(), defintionValues.get(0),
            defintionValues.get(1));
    newRow.addToComponent(ownPanel, rowIndex, MapXMLHelper.getGBCCloneWith(gbcBase, 0, rowIndex));
    rows.add(newRow);
  }

  private void addAddSequenceButton() {
    final JButton buttonAddSequence = new JButton("Add Sequence");
    buttonAddSequence.setFont(MapXMLHelper.defaultMapXMLCreatorFont);
    buttonAddSequence.addActionListener(new AbstractAction("Add Sequence") {
      private static final long serialVersionUID = 6322566373692205163L;

      public void actionPerformed(final ActionEvent e) {
        Optional<String> newSequenceNameOptional =
            Optional.of(JOptionPane.showInputDialog(ownPanel, "Enter a new sequence name:",
                "Sequence" + (MapXMLHelper.gamePlaySequence.size() + 1)));
        if (!newSequenceNameOptional.isPresent() || newSequenceNameOptional.get().isEmpty())
          return;
        final String newSequenceName = newSequenceNameOptional.get().trim();
        if (MapXMLHelper.gamePlaySequence.containsKey(newSequenceName)) {
          JOptionPane.showMessageDialog(ownPanel, "Sequence '" + newSequenceName + "' already exists.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        final ArrayList<String> newValue = new ArrayList<String>();
        newValue.add("");
        newValue.add("");
        MapXMLHelper.putGamePlaySequence(newSequenceName, newValue);

        // UI Update
        setRows((GridBagLayout) ownPanel.getLayout(), MapXMLHelper.gamePlaySequence.size());
        addRowWith(newSequenceName, "", "");
        SwingUtilities.invokeLater(() -> {
          ownPanel.revalidate();
          ownPanel.repaint();
        });
      }
    });
    addButton(buttonAddSequence);
  }

  /**
   * 
   */
  public void setOwnPanelLayout() {
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXMLHelper.gamePlaySequence.size());
    ownPanel.setLayout(gbl_stepActionPanel);
  }

  private DynamicRow addRowWith(final String newSequenceName, final String className, final String displayName) {
    final GameSequenceRow newRow = new GameSequenceRow(this, ownPanel, newSequenceName, className, displayName);
    addRow(newRow);
    return newRow;
  }


  protected void initializeSpecifics() {}

  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 60, 50, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0};
  }
}
