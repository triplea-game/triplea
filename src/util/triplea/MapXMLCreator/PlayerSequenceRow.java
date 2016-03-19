package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
    textFieldSequenceName.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldSequenceName.getText().trim();
        if (currentRowName.equals(inputText))
          return;
        if (MapXMLHelper.playerSequence.containsKey(inputText)) {
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
        MapXMLHelper.playerSequence.remove(currentRowName);
        Triple<String, String, Integer> newTriple =
            Triple.of(MapXMLHelper.gamePlaySequence.keySet().iterator().next(), MapXMLHelper.playerName.get(0), 0);
        MapXMLHelper.playerSequence.put(inputText, newTriple);
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldSequenceName.selectAll();
      }
    });


    dimension = comboBoxGameSequenceName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    comboBoxGameSequenceName.setPreferredSize(dimension);
    comboBoxGameSequenceName.setSelectedIndex(Arrays.binarySearch(gameSequenceNames, gameSequenceName));
    comboBoxGameSequenceName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        final Triple<String, String, Integer> oldTriple = MapXMLHelper.playerSequence.get(currentRowName);
        MapXMLHelper.playerSequence.put(currentRowName,
            Triple.of((String) comboBoxGameSequenceName.getSelectedItem(), oldTriple.getSecond(), oldTriple.getThird()));
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });


    dimension = comboBoxPlayerName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    comboBoxPlayerName.setPreferredSize(dimension);
    comboBoxPlayerName.setSelectedIndex(Arrays.binarySearch(playerNames, playerName));
    comboBoxPlayerName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        final Triple<String, String, Integer> oldTriple = MapXMLHelper.playerSequence.get(currentRowName);
        MapXMLHelper.playerSequence.put(currentRowName,
            Triple.of(oldTriple.getFirst(), (String) comboBoxPlayerName.getSelectedItem(), oldTriple.getThird()));
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });


    dimension = textFieldMaxCount.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldMaxCount.setPreferredSize(dimension);
    textFieldMaxCount.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldMaxCount.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          if (newValue < 0)
            throw new NumberFormatException();
          final Triple<String, String, Integer> oldTriple = MapXMLHelper.playerSequence.get(currentRowName);
          MapXMLHelper.playerSequence.put(currentRowName,
              Triple.of(oldTriple.getFirst(), oldTriple.getSecond(), newValue));
        } catch (NumberFormatException e) {
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
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldMaxCount.selectAll();
      }
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
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
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
    parent.add(buttonRemoveRow, gridBadConstButtonRemove);
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
    MapXMLHelper.playerSequence.remove(currentRowName);
  }
}
