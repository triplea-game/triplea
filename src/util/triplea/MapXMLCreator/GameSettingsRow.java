package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import games.strategy.util.Triple;


class GameSettingsRow extends DynamicRow {
  private boolean isBoolean;
  private JComboBox<String> tSettingName;
  private JTextField tValue;
  private JComboBox<String> tEditable;
  public static String[] selectionTrueFalse = {"false", "true"};
  private JTextField tMinNumber;
  private JTextField tMaxNumber;

  public GameSettingsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String settingName,
      final String[] settingNames, final String value, final String editable,
      final int minNumber, final int maxNumber) {
    super(settingName, parentRowPanel, stepActionPanel);

    isBoolean = GameSettingsPanel.isBoolean(settingName);
    tSettingName = new JComboBox<String>(settingNames);
    tEditable = new JComboBox<String>(selectionTrueFalse);
    tValue = new JTextField(value);
    final Integer minCountInteger = Integer.valueOf(minNumber);
    tMinNumber = new JTextField(minCountInteger == null ? "0" : Integer.toString(minCountInteger));
    final Integer maxCountInteger = Integer.valueOf(maxNumber);
    tMaxNumber = new JTextField(maxCountInteger == null ? "0" : Integer.toString(maxCountInteger));

    Dimension dimension = tSettingName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    tSettingName.setPreferredSize(dimension);
    try {
      tSettingName.setSelectedIndex(Arrays.binarySearch(settingNames, settingName));
    } catch (IllegalArgumentException e) {
      System.out.println(settingName + " is not known (yet)!");
    }
    tSettingName.addFocusListener(new FocusListener() {
      int prevSelectedIndex = tSettingName.getSelectedIndex();

      @Override
      public void focusLost(FocusEvent arg0) {
        if (prevSelectedIndex == tSettingName.getSelectedIndex())
          return;
        final String curr_settingName = (String) tSettingName.getSelectedItem();
        if (currentRowName.equals(curr_settingName))
          return;
        if (MapXMLHelper.gameSettings.containsKey(curr_settingName)) {
          JOptionPane.showMessageDialog(stepActionPanel, "Game setting '" + curr_settingName + "' already exists.",
              "Input error", JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(() -> {
            tSettingName.setSelectedIndex(prevSelectedIndex);
            tSettingName.requestFocus();
          });
          return;
        }
        // everything is okay with the new value name, lets rename everything
        final List<String> newValues = MapXMLHelper.gameSettings.get(currentRowName);
        MapXMLHelper.gameSettings.remove(currentRowName);
        final boolean newIsBoolean = GameSettingsPanel.isBoolean(curr_settingName);
        if (newIsBoolean != isBoolean) {
          if (newIsBoolean) {
            newValues.set(0, "false");
            tValue.setText("false");
            tMinNumber.setEnabled(false);
            tMaxNumber.setEnabled(false);
          } else {
            newValues.set(0, "0");
            tValue.setText("0");
            tMinNumber.setEnabled(true);
            tMaxNumber.setEnabled(true);
          }
          newValues.set(2, "0");
          tMinNumber.setText("0");
          newValues.set(3, "0");
          tMaxNumber.setText("0");
          isBoolean = newIsBoolean;
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tValue.updateUI();
              tMinNumber.updateUI();
              tMaxNumber.updateUI();
              tValue.requestFocus();
              tValue.selectAll();
            }
          });
        }
        MapXMLHelper.gameSettings.put(curr_settingName, newValues);
        currentRowName = curr_settingName;
        prevSelectedIndex = tSettingName.getSelectedIndex();
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });


    dimension = tValue.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tValue.setPreferredSize(dimension);
    tValue.addFocusListener(new FocusListener() {
      String prevValue = value;

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tValue.getText().trim().toLowerCase();
        boolean isInputOkay = true;
        if (isBoolean) {
          final String parsedInputText = Boolean.toString(Boolean.parseBoolean(inputText));
          isInputOkay = parsedInputText.equals(inputText);
          if (!isInputOkay)
            JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is not a boolean value.", "Input error",
                JOptionPane.ERROR_MESSAGE);
        } else {
          try {
            Integer.parseInt(inputText);
          } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
                JOptionPane.ERROR_MESSAGE);
            isInputOkay = false;
          }
        }
        if (!isInputOkay) {
          tValue.setText(prevValue);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tValue.updateUI();
              tValue.requestFocus();
              tValue.selectAll();
            }
          });
          return;
        } else {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tValue.updateUI();
            }
          });
        }
        prevValue = inputText;

        // everything is okay with the new value name, lets rename everything
        final List<String> newValues = MapXMLHelper.gameSettings.get(currentRowName);
        newValues.set(0, inputText);
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tValue.selectAll();
      }
    });

    dimension = tEditable.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tEditable.setPreferredSize(dimension);
    tEditable.setSelectedIndex(Arrays.binarySearch(selectionTrueFalse, editable));
    tEditable.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent arg0) {
        // everything is okay with the new value name, lets rename everything
        final List<String> newValues = MapXMLHelper.gameSettings.get(currentRowName);
        newValues.set(1, (String) tEditable.getSelectedItem());
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });

    dimension = tMinNumber.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tMinNumber.setPreferredSize(dimension);
    tMinNumber.setEnabled(!isBoolean);
    tMinNumber.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tMinNumber.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          if (newValue < 0)
            throw new NumberFormatException();
          final Triple<String, String, Integer> oldTriple = MapXMLHelper.playerSequence.get(currentRowName);
          MapXMLHelper.playerSequence.put(currentRowName,
              Triple.of(oldTriple.getFirst(), oldTriple.getSecond(), newValue));
        } catch (NumberFormatException e) {
          tMinNumber.setText("0");
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tMinNumber.updateUI();
              tMinNumber.requestFocus();
              tMinNumber.selectAll();
            }
          });
          return;
        }
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tMinNumber.selectAll();
      }
    });

    dimension = tMaxNumber.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tMaxNumber.setPreferredSize(dimension);
    tMaxNumber.setEnabled(!isBoolean);
    tMaxNumber.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tMaxNumber.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          if (newValue < 0)
            throw new NumberFormatException();
          final Triple<String, String, Integer> oldTriple = MapXMLHelper.playerSequence.get(currentRowName);
          MapXMLHelper.playerSequence.put(currentRowName,
              Triple.of(oldTriple.getFirst(), oldTriple.getSecond(), newValue));
        } catch (NumberFormatException e) {
          tMaxNumber.setText("0");
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tMaxNumber.updateUI();
              tMaxNumber.requestFocus();
              tMaxNumber.selectAll();
            }
          });
          return;
        }
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tMaxNumber.selectAll();
      }
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(tValue);
    componentList.add(tSettingName);
    componentList.add(tEditable);
    componentList.add(tMinNumber);
    componentList.add(tMaxNumber);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(tSettingName, gbc_template);

    final GridBagConstraints gbc_tValue = (GridBagConstraints) gbc_template.clone();
    gbc_tValue.gridx = 1;
    parent.add(tValue, gbc_tValue);

    final GridBagConstraints gbc_tEditable = (GridBagConstraints) gbc_template.clone();
    gbc_tEditable.gridx = 2;
    parent.add(tEditable, gbc_tEditable);

    final GridBagConstraints gbc_tMinNumber = (GridBagConstraints) gbc_template.clone();
    gbc_tMinNumber.gridx = 3;
    parent.add(tMinNumber, gbc_tMinNumber);

    final GridBagConstraints gbc_tMaxNumber = (GridBagConstraints) gbc_template.clone();
    gbc_tMaxNumber.gridx = 4;
    parent.add(tMaxNumber, gbc_tMaxNumber);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 5;
    parent.add(buttonRemoveRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final GameSettingsRow newGameSettingsRow = (GameSettingsRow) newRow;
    this.tSettingName.setSelectedIndex(newGameSettingsRow.tSettingName.getSelectedIndex());
    this.tValue.setText(newGameSettingsRow.tValue.getText());
    this.tEditable.setSelectedIndex(newGameSettingsRow.tEditable.getSelectedIndex());
    this.tMinNumber.setText(newGameSettingsRow.tMinNumber.getText());
    this.tMaxNumber.setText(newGameSettingsRow.tMaxNumber.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.gameSettings.remove(currentRowName);
  }
}
