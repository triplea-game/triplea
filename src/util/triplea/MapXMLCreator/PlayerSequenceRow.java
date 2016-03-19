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
  private JTextField tSequenceName;
  private JComboBox<String> tGameSequenceName;
  private JComboBox<String> tPlayerName;
  private JTextField tMaxCount;

  public PlayerSequenceRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel,
      final String sequenceName, final String gameSequenceName, final String[] gameSequenceNames,
      final String playerName, final String[] playerNames, final int maxCount) {
    super(sequenceName, parentRowPanel, stepActionPanel);

    tSequenceName = new JTextField(sequenceName);
    tGameSequenceName = new JComboBox<String>(gameSequenceNames);
    tPlayerName = new JComboBox<String>(playerNames);
    final Integer maxCountInteger = Integer.valueOf(maxCount);
    tMaxCount = new JTextField(maxCountInteger == null ? "0" : Integer.toString(maxCountInteger));

    Dimension dimension = tSequenceName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    tSequenceName.setPreferredSize(dimension);
    tSequenceName.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tSequenceName.getText().trim();
        if (currentRowName.equals(inputText))
          return;
        if (MapXMLHelper.playerSequence.containsKey(inputText)) {
          tSequenceName.selectAll();
          JOptionPane.showMessageDialog(stepActionPanel, "Sequence '" + inputText + "' already exists.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tSequenceName.requestFocus();
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
        tSequenceName.selectAll();
      }
    });


    dimension = tGameSequenceName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    tGameSequenceName.setPreferredSize(dimension);
    tGameSequenceName.setSelectedIndex(Arrays.binarySearch(gameSequenceNames, gameSequenceName));
    tGameSequenceName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        final Triple<String, String, Integer> oldTriple = MapXMLHelper.playerSequence.get(currentRowName);
        MapXMLHelper.playerSequence.put(currentRowName,
            Triple.of((String) tGameSequenceName.getSelectedItem(), oldTriple.getSecond(), oldTriple.getThird()));
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });


    dimension = tPlayerName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    tPlayerName.setPreferredSize(dimension);
    tPlayerName.setSelectedIndex(Arrays.binarySearch(playerNames, playerName));
    tPlayerName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        final Triple<String, String, Integer> oldTriple = MapXMLHelper.playerSequence.get(currentRowName);
        MapXMLHelper.playerSequence.put(currentRowName,
            Triple.of(oldTriple.getFirst(), (String) tPlayerName.getSelectedItem(), oldTriple.getThird()));
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });


    dimension = tMaxCount.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tMaxCount.setPreferredSize(dimension);
    tMaxCount.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tMaxCount.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          if (newValue < 0)
            throw new NumberFormatException();
          final Triple<String, String, Integer> oldTriple = MapXMLHelper.playerSequence.get(currentRowName);
          MapXMLHelper.playerSequence.put(currentRowName,
              Triple.of(oldTriple.getFirst(), oldTriple.getSecond(), newValue));
        } catch (NumberFormatException e) {
          tMaxCount.setText("0");
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tMaxCount.updateUI();
              tMaxCount.requestFocus();
              tMaxCount.selectAll();
            }
          });
          return;
        }
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tMaxCount.selectAll();
      }
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(tSequenceName);
    componentList.add(tGameSequenceName);
    componentList.add(tPlayerName);
    componentList.add(tMaxCount);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(tSequenceName, gbc_template);

    final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
    gbc_tClassName.gridx = 1;
    parent.add(tGameSequenceName, gbc_tClassName);

    final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
    gbc_tDisplayName.gridx = 2;
    parent.add(tPlayerName, gbc_tDisplayName);

    final GridBagConstraints gbc_tMaxCount = (GridBagConstraints) gbc_template.clone();
    gbc_tMaxCount.gridx = 3;
    parent.add(tMaxCount, gbc_tMaxCount);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 4;
    parent.add(buttonRemoveRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final PlayerSequenceRow newRowPlayerSequenceRow = (PlayerSequenceRow) newRow;
    this.tSequenceName.setText(newRowPlayerSequenceRow.tSequenceName.getText());
    this.tGameSequenceName.setSelectedIndex(newRowPlayerSequenceRow.tGameSequenceName.getSelectedIndex());
    this.tPlayerName.setSelectedIndex(newRowPlayerSequenceRow.tPlayerName.getSelectedIndex());
    this.tMaxCount.setText(newRowPlayerSequenceRow.tMaxCount.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.playerSequence.remove(currentRowName);
  }
}
