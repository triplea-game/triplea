package tools.map.xml.creator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.ui.SwingAction;


public class GameSequencePanel extends DynamicRowsPanel {

  public GameSequencePanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  protected static void layout(final MapXmlCreator mapXmlCreator) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof GameSequencePanel)) {
      me = Optional.of(new GameSequencePanel(mapXmlCreator.getStepActionPanel()));
    }
    DynamicRowsPanel.layout(mapXmlCreator);
  }

  @Override
  protected ActionListener getAutoFillAction() {
    return e -> {
      if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(getOwnPanel(),
          "Are you sure you want to use the  Auto-Fill feature?\r"
              + "It will remove any information you have entered in this step and propose commonly used choices.",
          "Auto-Fill Overwrite Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
        setGamePlaySequenceMapToDefault();
        // Update UI
        repaintOwnPanel();
      }
    };
  }

  @Override
  protected void layoutComponents() {

    setOwnPanelLayout();

    final GridBagConstraints gbcDefault = MapXmlUiHelper.getGbcDefaultTemplateWith(0, 0);

    addLabelsRow(gbcDefault);

    // Add main input rows
    int rowIndex = 1;
    for (final Entry<String, List<String>> entry : MapXmlHelper.getGamePlaySequenceMap().entrySet()) {
      addMainInputRow(gbcDefault, rowIndex, entry);
      ++rowIndex;
    }

    addAddSequenceButton();

    addFinalButtonRow(MapXmlUiHelper.getGbcCloneWith(gbcDefault, 0, rowIndex));
  }

  /**
   * @param gridBadConstLabelSequenceName GridBagConstraints object for "Sequence Name" label and default for other
   *        labels.
   */
  private void addLabelsRow(final GridBagConstraints gridBadConstLabelSequenceName) {
    final JLabel labelSequenceName = new JLabel("Sequence Name");
    final Dimension dimension = labelSequenceName.getPreferredSize();
    dimension.width = 140;
    labelSequenceName.setPreferredSize(dimension);
    getOwnPanel().add(labelSequenceName, gridBadConstLabelSequenceName);

    final JLabel labelClassName = new JLabel("Class Name");
    labelClassName.setPreferredSize(dimension);
    getOwnPanel().add(labelClassName, MapXmlUiHelper.getGbcCloneWith(gridBadConstLabelSequenceName, 1, 0));

    final JLabel labelDisplayName = new JLabel("Display Name");
    labelDisplayName.setPreferredSize(dimension);
    getOwnPanel().add(labelDisplayName, MapXmlUiHelper.getGbcCloneWith(gridBadConstLabelSequenceName, 2, 0));
  }

  private void addMainInputRow(final GridBagConstraints gbcBase, final int rowIndex,
      final Entry<String, List<String>> rowEntry) {
    final List<String> defintionValues = rowEntry.getValue();
    final GameSequenceRow newRow =
        new GameSequenceRow(this, getOwnPanel(), rowEntry.getKey(), defintionValues.get(0),
            defintionValues.get(1));
    newRow.addToParentComponentWithGbc(getOwnPanel(), rowIndex, MapXmlUiHelper.getGbcCloneWith(gbcBase, 0, rowIndex));
    rows.add(newRow);
  }

  private void addAddSequenceButton() {
    final JButton buttonAddSequence = new JButton("Add Sequence");
    buttonAddSequence.setFont(MapXmlUiHelper.defaultMapXMLCreatorFont);
    buttonAddSequence.addActionListener(SwingAction.of("Add Sequence", e -> {
      final Optional<String> newSequenceNameOptional =
          Optional.of(JOptionPane.showInputDialog(getOwnPanel(), "Enter a new sequence name:",
              "Sequence" + (MapXmlHelper.getGamePlaySequenceMap().size() + 1)));
      if (!newSequenceNameOptional.isPresent() || newSequenceNameOptional.get().isEmpty()) {
        return;
      }
      final String newSequenceName = newSequenceNameOptional.get().trim();
      if (MapXmlHelper.getGamePlaySequenceMap().containsKey(newSequenceName)) {
        JOptionPane.showMessageDialog(getOwnPanel(), "Sequence '" + newSequenceName + "' already exists.",
            "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      final ArrayList<String> newValue = new ArrayList<>();
      newValue.add("");
      newValue.add("");
      MapXmlHelper.putGamePlaySequence(newSequenceName, newValue);

      // UI Update
      setRows((GridBagLayout) getOwnPanel().getLayout(), MapXmlHelper.getGamePlaySequenceMap().size());
      addRowWith(newSequenceName, "", "");
      SwingUtilities.invokeLater(() -> {
        getOwnPanel().revalidate();
        getOwnPanel().repaint();
      });
    }));
    addButton(buttonAddSequence);
  }

  private void setOwnPanelLayout() {
    final GridBagLayout gblStepActionPanel = new GridBagLayout();
    setColumns(gblStepActionPanel);
    setRows(gblStepActionPanel, MapXmlHelper.getGamePlaySequenceMap().size());
    getOwnPanel().setLayout(gblStepActionPanel);
  }

  private DynamicRow addRowWith(final String newSequenceName, final String className, final String displayName) {
    final GameSequenceRow newRow = new GameSequenceRow(this, getOwnPanel(), newSequenceName, className, displayName);
    addRow(newRow);
    return newRow;
  }


  @Override
  protected void initializeSpecifics() {}

  @Override
  protected void setColumns(final GridBagLayout gblPanel) {
    gblPanel.columnWidths = new int[] {50, 60, 50, 30};
    gblPanel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0};
  }

  private static void setGamePlaySequenceMapToDefault() {
    MapXmlHelper.clearGamePlaySequence();
    MapXmlHelper.getGamePlaySequenceMap().put("bid",
        Arrays.asList("BidPurchaseDelegate", "Bid Purchase"));
    MapXmlHelper.getGamePlaySequenceMap().put("placeBid",
        Arrays.asList("BidPlaceDelegate", "Bid Placement"));
    MapXmlHelper.getGamePlaySequenceMap().put("tech",
        Arrays.asList("TechnologyDelegate", "Research Technology"));
    MapXmlHelper.getGamePlaySequenceMap().put("tech_Activation",
        Arrays.asList("TechActivationDelegate", "Activate Technology"));
    MapXmlHelper.getGamePlaySequenceMap().put("purchase",
        Arrays.asList("PurchaseDelegate", "Purchase Units"));
    MapXmlHelper.getGamePlaySequenceMap().put("move",
        Arrays.asList("MoveDelegate", "Combat Move"));
    MapXmlHelper.getGamePlaySequenceMap().put("battle",
        Arrays.asList("BattleDelegate", "Combat"));
    MapXmlHelper.getGamePlaySequenceMap().put("place",
        Arrays.asList("PlaceDelegate", "Place Units"));
    MapXmlHelper.getGamePlaySequenceMap().put("endTurn",
        Arrays.asList("BidPurchaseDelegate", "Turn Complete"));
  }
}
