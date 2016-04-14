package util.triplea.mapXmlCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;

import games.strategy.util.Triple;


class GameSequenceRow extends DynamicRow {
  private JTextField textFieldSequenceName;
  private JTextField textFieldClassName;
  private JTextField textFieldDisplayName;

  public GameSequenceRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String sequenceName,
      final String className, final String displayName) {
    super(sequenceName, parentRowPanel, stepActionPanel);

    textFieldSequenceName = new JTextField(sequenceName);
    textFieldClassName = new JTextField(className);
    textFieldDisplayName = new JTextField(displayName);

    Dimension dimension = textFieldSequenceName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    textFieldSequenceName.setPreferredSize(dimension);
    MapXmlUIHelper.addNewFocusListenerForTextField(textFieldSequenceName, () -> {
      final String inputText = textFieldSequenceName.getText().trim();
      if (currentRowName.equals(inputText)) {
        return;
      }
      if (MapXmlHelper.getGamePlaySequenceMap().containsKey(inputText)) {
        textFieldSequenceName.selectAll();
        JOptionPane.showMessageDialog(stepActionPanel, "Sequence '" + inputText + "' already exists.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        parentRowPanel.setDataIsConsistent(false);
        SwingUtilities.invokeLater(new Runnable() {

          @Override
          public void run() {
            textFieldSequenceName.requestFocus();
          }
        });
        return;
      }
      // everything is okay with the new player namer, lets rename everything
      final List<String> values = MapXmlHelper.getGamePlaySequenceMap().get(currentRowName);
      MapXmlHelper.getGamePlaySequenceMap().put(inputText, values);
      if (!MapXmlHelper.getPlayerSequenceMap().isEmpty()) {
        // Replace Game Sequence for Player Sequence
        final Map<String, Triple<String, String, Integer>> updatesPlayerSequence =
            Maps.newLinkedHashMap();
        for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXmlHelper.getPlayerSequenceMap()
            .entrySet()) {
          final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
          if (currentRowName.equals(oldTriple.getFirst())) {
            updatesPlayerSequence.put(playerSequence.getKey(),
                Triple.of(inputText, oldTriple.getSecond(), oldTriple.getThird()));
          }
        }
        for (final Entry<String, Triple<String, String, Integer>> playerSequence : updatesPlayerSequence.entrySet()) {
          MapXmlHelper.getPlayerSequenceMap().put(playerSequence.getKey(), playerSequence.getValue());
        }
      }
      currentRowName = inputText;
      parentRowPanel.setDataIsConsistent(true);
    });

    dimension = textFieldClassName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    textFieldClassName.setPreferredSize(dimension);
    MapXmlUIHelper.addNewFocusListenerForTextField(textFieldClassName, () -> {
      final String inputText = textFieldClassName.getText().trim();
      MapXmlHelper.getGamePlaySequenceMap().get(sequenceName).set(0, inputText);
    });

    dimension = textFieldDisplayName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    textFieldDisplayName.setPreferredSize(dimension);
    MapXmlUIHelper.addNewFocusListenerForTextField(textFieldDisplayName, () -> {
      final String inputText = textFieldDisplayName.getText().trim();
      MapXmlHelper.getGamePlaySequenceMap().get(sequenceName).set(1, inputText);
    });

  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(textFieldSequenceName);
    componentList.add(textFieldClassName);
    componentList.add(textFieldDisplayName);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(textFieldSequenceName, gbc_template);

    final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
    gbc_tClassName.gridx = 1;
    parent.add(textFieldClassName, gbc_tClassName);

    final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
    gbc_tDisplayName.gridx = 2;
    parent.add(textFieldDisplayName, gbc_tDisplayName);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 3;
    parent.add(buttonRemovePerRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final GameSequenceRow newRowPlayerAndAlliancesRow = (GameSequenceRow) newRow;
    this.textFieldSequenceName.setText(newRowPlayerAndAlliancesRow.textFieldSequenceName.getText());
    this.textFieldClassName.setText(newRowPlayerAndAlliancesRow.textFieldClassName.getText());
    this.textFieldDisplayName.setText(newRowPlayerAndAlliancesRow.textFieldDisplayName.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXmlHelper.getGamePlaySequenceMap().remove(currentRowName);

    if (!MapXmlHelper.getPlayerSequenceMap().isEmpty()) {
      // Replace Player Sequences using the deleted Game Sequence
      final ArrayList<String> deleteKeys = new ArrayList<String>();
      for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXmlHelper.getPlayerSequenceMap()
          .entrySet()) {
        final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
        if (currentRowName.equals(oldTriple.getFirst())) {
          deleteKeys.add(playerSequence.getKey());
        }
      }
      for (final String deleteKey : deleteKeys) {
        MapXmlHelper.getPlayerSequenceMap().remove(deleteKey);
      }
    }
  }
}
