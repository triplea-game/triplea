package util.triplea.mapXmlCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import games.strategy.util.Triple;


class PlayerSequenceRow extends DynamicRow {
  private JTextField textFieldSequenceName;
  private JComboBox<String> comboBoxGameSequenceName;
  private JComboBox<String> comboBoxPlayerName;
  private JTextField textFieldMaxCount;

  public PlayerSequenceRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel,
      final String sequenceName, final String gameSequenceName, final String[] gameSequenceNames,
      final String playerName, final String[] playerNames, final int maxCount) {
    super(sequenceName, parentRowPanel, stepActionPanel);

    textFieldSequenceName = new JTextField(sequenceName);
    comboBoxGameSequenceName = new JComboBox<String>(gameSequenceNames);
    comboBoxPlayerName = new JComboBox<String>(playerNames);
    final Integer maxCountInteger = Integer.valueOf(maxCount);
    textFieldMaxCount = new JTextField(maxCountInteger == null ? "0" : Integer.toString(maxCountInteger));

    Dimension dimension = textFieldSequenceName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    textFieldSequenceName.setPreferredSize(dimension);
    MapXmlUIHelper.addNewFocusListenerForTextField(textFieldMaxCount, () -> {
      final String inputText = textFieldSequenceName.getText().trim();
      if (currentRowName.equals(inputText)) {
        return;
      }
      if (MapXmlHelper.getPlayerSequenceMap().containsKey(inputText)) {
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
      MapXmlHelper.getPlayerSequenceMap().remove(currentRowName);
      final Triple<String, String, Integer> newTriple =
          Triple.of(MapXmlHelper.getGamePlaySequenceMap().keySet().iterator().next(),
              MapXmlHelper.getPlayerNames().get(0), 0);
      MapXmlHelper.getPlayerSequenceMap().put(inputText, newTriple);
      parentRowPanel.setDataIsConsistent(true);
    });

    dimension = comboBoxGameSequenceName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    comboBoxGameSequenceName.setPreferredSize(dimension);
    comboBoxGameSequenceName.setSelectedIndex(Arrays.binarySearch(gameSequenceNames, gameSequenceName));
    comboBoxGameSequenceName.addFocusListener(FocusListenerFocusLost.withAction(() -> {
      final Triple<String, String, Integer> oldTriple = MapXmlHelper.getPlayerSequenceMap().get(currentRowName);
      MapXmlHelper.getPlayerSequenceMap().put(currentRowName,
          Triple.of((String) comboBoxGameSequenceName.getSelectedItem(), oldTriple.getSecond(), oldTriple.getThird()));
    }));

    dimension = comboBoxPlayerName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    comboBoxPlayerName.setPreferredSize(dimension);
    comboBoxPlayerName.setSelectedIndex(Arrays.binarySearch(playerNames, playerName));
    comboBoxPlayerName.addFocusListener(FocusListenerFocusLost.withAction(() -> {
      final Triple<String, String, Integer> oldTriple = MapXmlHelper.getPlayerSequenceMap().get(currentRowName);
      MapXmlHelper.getPlayerSequenceMap().put(currentRowName,
          Triple.of(oldTriple.getFirst(), (String) comboBoxPlayerName.getSelectedItem(), oldTriple.getThird()));
    }));

    dimension = textFieldMaxCount.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldMaxCount.setPreferredSize(dimension);
    MapXmlUIHelper.addNewFocusListenerForTextField(textFieldMaxCount, () -> {
      final String inputText = textFieldMaxCount.getText().trim();
      try {
        final Integer newValue = Integer.parseInt(inputText);
        if (newValue < 0) {
          throw new NumberFormatException();
        }
        final Triple<String, String, Integer> oldTriple = MapXmlHelper.getPlayerSequenceMap().get(currentRowName);
        MapXmlHelper.getPlayerSequenceMap().put(currentRowName,
            Triple.of(oldTriple.getFirst(), oldTriple.getSecond(), newValue));
      } catch (final NumberFormatException e) {
        textFieldMaxCount.setText("0");
        JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        parentRowPanel.setDataIsConsistent(false);
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            textFieldMaxCount.updateUI();
            textFieldMaxCount.requestFocus();
            textFieldMaxCount.selectAll();
          }
        });
        return;
      }
      parentRowPanel.setDataIsConsistent(true);
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(textFieldSequenceName);
    componentList.add(comboBoxGameSequenceName);
    componentList.add(comboBoxPlayerName);
    componentList.add(textFieldMaxCount);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(textFieldSequenceName, gbc_template);

    final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
    gbc_tClassName.gridx = 1;
    parent.add(comboBoxGameSequenceName, gbc_tClassName);

    final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
    gbc_tDisplayName.gridx = 2;
    parent.add(comboBoxPlayerName, gbc_tDisplayName);

    final GridBagConstraints gbc_tMaxCount = (GridBagConstraints) gbc_template.clone();
    gbc_tMaxCount.gridx = 3;
    parent.add(textFieldMaxCount, gbc_tMaxCount);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 4;
    parent.add(buttonRemovePerRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final PlayerSequenceRow newRowPlayerSequenceRow = (PlayerSequenceRow) newRow;
    this.textFieldSequenceName.setText(newRowPlayerSequenceRow.textFieldSequenceName.getText());
    this.comboBoxGameSequenceName.setSelectedIndex(newRowPlayerSequenceRow.comboBoxGameSequenceName.getSelectedIndex());
    this.comboBoxPlayerName.setSelectedIndex(newRowPlayerSequenceRow.comboBoxPlayerName.getSelectedIndex());
    this.textFieldMaxCount.setText(newRowPlayerSequenceRow.textFieldMaxCount.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXmlHelper.getPlayerSequenceMap().remove(currentRowName);
  }
}
