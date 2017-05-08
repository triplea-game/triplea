package tools.map.xml.creator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.ui.SwingAction;
import games.strategy.util.Triple;


public class PlayerSequencePanel extends DynamicRowsPanel {

  private final TreeSet<String> gameSequenceNames = new TreeSet<>();
  private final TreeSet<String> playerNames = new TreeSet<>();

  public PlayerSequencePanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXmlCreator mapXmlCreator) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof PlayerSequencePanel)) {
      me = Optional.of(new PlayerSequencePanel(mapXmlCreator.getStepActionPanel()));
    }
    DynamicRowsPanel.layout(mapXmlCreator);
  }

  @Override
  protected ActionListener getAutoFillAction() {
    return null;
  }

  @Override
  protected void layoutComponents() {

    final JLabel labelSequenceName = new JLabel("Sequence Name");
    Dimension dimension = labelSequenceName.getPreferredSize();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
    labelSequenceName.setPreferredSize(dimension);
    final JLabel labelGameSequenceName = new JLabel("Game Sequence");
    labelGameSequenceName.setPreferredSize(dimension);
    final JLabel labelPlayerName = new JLabel("Player Name");
    labelPlayerName.setPreferredSize(dimension);
    final JLabel labelMaxRunCount = new JLabel("Max Run Count");
    dimension = (Dimension) dimension.clone();
    dimension.width = 90;
    labelMaxRunCount.setPreferredSize(dimension);

    // <1> Set panel layout
    final GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXmlHelper.getPlayerSequenceMap().size());
    getOwnPanel().setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
    final GridBagConstraints gridBadConstLabelSequenceName = new GridBagConstraints();
    gridBadConstLabelSequenceName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelSequenceName.gridy = 0;
    gridBadConstLabelSequenceName.gridx = 0;
    gridBadConstLabelSequenceName.anchor = GridBagConstraints.WEST;
    getOwnPanel().add(labelSequenceName, gridBadConstLabelSequenceName);

    final GridBagConstraints gridBadConstLabelGameSequenceName =
        (GridBagConstraints) gridBadConstLabelSequenceName.clone();
    gridBadConstLabelGameSequenceName.gridx = 1;
    getOwnPanel().add(labelGameSequenceName, gridBadConstLabelGameSequenceName);

    final GridBagConstraints gridBadConstLabelPlayerName = (GridBagConstraints) gridBadConstLabelSequenceName.clone();
    gridBadConstLabelPlayerName.gridx = 2;
    getOwnPanel().add(labelPlayerName, gridBadConstLabelPlayerName);

    final GridBagConstraints gridBadConstLabelMaxRunCount = (GridBagConstraints) gridBadConstLabelSequenceName.clone();
    gridBadConstLabelMaxRunCount.gridx = 3;
    getOwnPanel().add(labelMaxRunCount, gridBadConstLabelMaxRunCount);

    // <3> Add Main Input Rows
    int rowIndex = 1;

    final String[] gameSequenceNamesArray = gameSequenceNames.toArray(new String[gameSequenceNames.size()]);
    final String[] playerNamesArray = playerNames.toArray(new String[playerNames.size()]);
    for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXmlHelper.getPlayerSequenceMap()
        .entrySet()) {
      final GridBagConstraints gbc_tSequenceName = (GridBagConstraints) gridBadConstLabelSequenceName.clone();
      gbc_tSequenceName.gridx = 0;
      gridBadConstLabelSequenceName.gridy = rowIndex;
      final Triple<String, String, Integer> defintionValues = playerSequence.getValue();
      final PlayerSequenceRow newRow =
          new PlayerSequenceRow(this, getOwnPanel(), playerSequence.getKey(), defintionValues.getFirst(),
              gameSequenceNamesArray, defintionValues.getSecond(), playerNamesArray, defintionValues.getThird());
      newRow.addToParentComponentWithGbc(getOwnPanel(), rowIndex, gbc_tSequenceName);
      rows.add(newRow);
      ++rowIndex;
    }

    // <4> Add Final Button Row
    final JButton buttonAddSequence = new JButton("Add Sequence");

    buttonAddSequence.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonAddSequence.addActionListener(SwingAction.of("Add Sequence", e -> {
      String newSequenceName = JOptionPane.showInputDialog(getOwnPanel(), "Enter a new sequence name:",
          "Sequence" + (MapXmlHelper.getPlayerSequenceMap().size() + 1));
      if (newSequenceName == null || newSequenceName.isEmpty()) {
        return;
      }
      if (MapXmlHelper.getPlayerSequenceMap().containsKey(newSequenceName)) {
        JOptionPane.showMessageDialog(getOwnPanel(), "Sequence '" + newSequenceName + "' already exists.",
            "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      newSequenceName = newSequenceName.trim();

      final Triple<String, String, Integer> newValue =
          Triple.of(gameSequenceNames.iterator().next(), playerNames.iterator().next(), 0);
      MapXmlHelper.putPlayerSequence(newSequenceName, newValue);

      // UI Update
      setRows((GridBagLayout) getOwnPanel().getLayout(), MapXmlHelper.getPlayerSequenceMap().size());
      addRowWith(newSequenceName, gameSequenceNames.iterator().next(), playerNames.iterator().next(), 0);
      SwingUtilities.invokeLater(() -> {
        getOwnPanel().revalidate();
        getOwnPanel().repaint();
      });
    }));
    addButton(buttonAddSequence);

    final GridBagConstraints gridBadConstButtonAddUnit = (GridBagConstraints) gridBadConstLabelSequenceName.clone();
    gridBadConstButtonAddUnit.gridx = 0;
    gridBadConstButtonAddUnit.gridy = rowIndex;
    addFinalButtonRow(gridBadConstButtonAddUnit);
  }

  private DynamicRow addRowWith(final String newSequenceName, final String gameSequenceName, final String playerName,
      final int maxCount) {
    final PlayerSequenceRow newRow = new PlayerSequenceRow(this, getOwnPanel(), newSequenceName, gameSequenceName,
        gameSequenceNames.toArray(new String[gameSequenceNames.size()]), playerName,
        playerNames.toArray(new String[playerNames.size()]), maxCount);
    addRow(newRow);
    return newRow;
  }


  @Override
  protected void initializeSpecifics() {
    gameSequenceNames.clear();
    playerNames.clear();
    gameSequenceNames.addAll(MapXmlHelper.getGamePlaySequenceMap().keySet());
    playerNames.add("");
    playerNames.addAll(MapXmlHelper.getPlayerNames());
  }

  @Override
  protected void setColumns(final GridBagLayout gblPanel) {
    gblPanel.columnWidths = new int[] {50, 60, 50, 30, 30};
    gblPanel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0};
  }
}
