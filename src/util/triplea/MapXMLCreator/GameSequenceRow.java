package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
  private JTextField tSequenceName;
  private JTextField tClassName;
  private JTextField tDisplayName;

  public GameSequenceRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String sequenceName,
      final String className, final String displayName) {
    super(sequenceName, parentRowPanel, stepActionPanel);

    tSequenceName = new JTextField(sequenceName);
    tClassName = new JTextField(className);
    tDisplayName = new JTextField(displayName);

    Dimension dimension = tSequenceName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    tSequenceName.setPreferredSize(dimension);
    tSequenceName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tSequenceName.getText().trim();
        if (currentRowName.equals(inputText))
          return;
        if (MapXMLHelper.gamePlaySequence.containsKey(inputText)) {
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
        final List<String> values = MapXMLHelper.gamePlaySequence.get(currentRowName);
        MapXMLHelper.gamePlaySequence.put(inputText, values);
        if (!MapXMLHelper.playerSequence.isEmpty()) {
          // Replace Game Sequence for Player Sequence
          final Map<String, Triple<String, String, Integer>> updatesPlayerSequence =
              Maps.newLinkedHashMap();
          for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.playerSequence
              .entrySet()) {
            final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
            if (currentRowName.equals(oldTriple.getFirst())) {
              updatesPlayerSequence.put(playerSequence.getKey(),
                  Triple.of(inputText, oldTriple.getSecond(), oldTriple.getThird()));
            }
          }
          for (final Entry<String, Triple<String, String, Integer>> playerSequence : updatesPlayerSequence.entrySet()) {
            MapXMLHelper.playerSequence.put(playerSequence.getKey(), playerSequence.getValue());
          }
        }
        currentRowName = inputText;
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tSequenceName.selectAll();
      }
    });

    dimension = tClassName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    tClassName.setPreferredSize(dimension);
    tClassName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tClassName.getText().trim();
        MapXMLHelper.gamePlaySequence.get(sequenceName).set(0, inputText);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tClassName.selectAll();
      }
    });

    dimension = tDisplayName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    tDisplayName.setPreferredSize(dimension);
    tDisplayName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tDisplayName.getText().trim();
        MapXMLHelper.gamePlaySequence.get(sequenceName).set(1, inputText);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tDisplayName.selectAll();
      }
    });

  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(tSequenceName);
    componentList.add(tClassName);
    componentList.add(tDisplayName);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(tSequenceName, gbc_template);

    final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
    gbc_tClassName.gridx = 1;
    parent.add(tClassName, gbc_tClassName);

    final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
    gbc_tDisplayName.gridx = 2;
    parent.add(tDisplayName, gbc_tDisplayName);

    final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
    gbc_bRemove.gridx = 3;
    parent.add(bRemoveRow, gbc_bRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final GameSequenceRow newRowPlayerAndAlliancesRow = (GameSequenceRow) newRow;
    this.tSequenceName.setText(newRowPlayerAndAlliancesRow.tSequenceName.getText());
    this.tClassName.setText(newRowPlayerAndAlliancesRow.tClassName.getText());
    this.tDisplayName.setText(newRowPlayerAndAlliancesRow.tDisplayName.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.gamePlaySequence.remove(currentRowName);

    if (!MapXMLHelper.playerSequence.isEmpty()) {
      // Replace Player Sequences using the deleted Game Sequence
      final ArrayList<String> deleteKeys = new ArrayList<String>();
      for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.playerSequence
          .entrySet()) {
        final Triple<String, String, Integer> oldTriple = playerSequence.getValue();
        if (currentRowName.equals(oldTriple.getFirst())) {
          deleteKeys.add(playerSequence.getKey());
        }
      }
      for (final String deleteKey : deleteKeys)
        MapXMLHelper.playerSequence.remove(deleteKey);
    }
  }
}
