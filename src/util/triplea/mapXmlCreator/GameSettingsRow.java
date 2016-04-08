package util.triplea.mapXmlCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import games.strategy.util.Triple;


class GameSettingsRow extends DynamicRow {
  private boolean isBoolean;
  private JComboBox<String> comboBoxSettingName;
  private JTextField textFieldValue;
  private JComboBox<String> comboBoxEditable;
  public static String[] selectionTrueFalse = {"false", "true"};
  private JTextField textFieldMinNumber;
  private JTextField textFieldMaxNumber;

  public GameSettingsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String settingName,
      final String[] settingNames, final String value, final String editable,
      final int minNumber, final int maxNumber) {
    super(settingName, parentRowPanel, stepActionPanel);

    isBoolean = GameSettingsPanel.isBoolean(settingName);
    comboBoxSettingName = new JComboBox<String>(settingNames);
    comboBoxEditable = new JComboBox<String>(selectionTrueFalse);
    textFieldValue = new JTextField(value);
    final Integer minCountInteger = Integer.valueOf(minNumber);
    textFieldMinNumber = new JTextField(minCountInteger == null ? "0" : Integer.toString(minCountInteger));
    final Integer maxCountInteger = Integer.valueOf(maxNumber);
    textFieldMaxNumber = new JTextField(maxCountInteger == null ? "0" : Integer.toString(maxCountInteger));

    Dimension dimension = comboBoxSettingName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    comboBoxSettingName.setPreferredSize(dimension);
    try {
      comboBoxSettingName.setSelectedIndex(Arrays.binarySearch(settingNames, settingName));
    } catch (IllegalArgumentException e) {
      Logger.getLogger(MapXmlCreator.MAP_XML_CREATOR_LOGGER_NAME).log(Level.WARNING,
          settingName + " is not known (yet)!");
    }
    comboBoxSettingName.addFocusListener(new FocusListener() {
      int prevSelectedIndex = comboBoxSettingName.getSelectedIndex();

      @Override
      public void focusLost(FocusEvent arg0) {
        if (prevSelectedIndex == comboBoxSettingName.getSelectedIndex())
          return;
        final String curr_settingName = (String) comboBoxSettingName.getSelectedItem();
        if (currentRowName.equals(curr_settingName))
          return;
        if (MapXmlHelper.getGameSettingsMap().containsKey(curr_settingName)) {
          JOptionPane.showMessageDialog(stepActionPanel, "Game setting '" + curr_settingName + "' already exists.",
              "Input error", JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(() -> {
            comboBoxSettingName.setSelectedIndex(prevSelectedIndex);
            comboBoxSettingName.requestFocus();
          });
          return;
        }
        // everything is okay with the new value name, lets rename everything
        final List<String> newValues = MapXmlHelper.getGameSettingsMap().get(currentRowName);
        MapXmlHelper.getGameSettingsMap().remove(currentRowName);
        final boolean newIsBoolean = GameSettingsPanel.isBoolean(curr_settingName);
        if (newIsBoolean != isBoolean) {
          if (newIsBoolean) {
            newValues.set(0, "false");
            textFieldValue.setText("false");
            textFieldMinNumber.setEnabled(false);
            textFieldMaxNumber.setEnabled(false);
          } else {
            newValues.set(0, "0");
            textFieldValue.setText("0");
            textFieldMinNumber.setEnabled(true);
            textFieldMaxNumber.setEnabled(true);
          }
          newValues.set(2, "0");
          textFieldMinNumber.setText("0");
          newValues.set(3, "0");
          textFieldMaxNumber.setText("0");
          isBoolean = newIsBoolean;
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              textFieldValue.updateUI();
              textFieldMinNumber.updateUI();
              textFieldMaxNumber.updateUI();
              textFieldValue.requestFocus();
              textFieldValue.selectAll();
            }
          });
        }
        MapXmlHelper.getGameSettingsMap().put(curr_settingName, newValues);
        currentRowName = curr_settingName;
        prevSelectedIndex = comboBoxSettingName.getSelectedIndex();
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });


    dimension = textFieldValue.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldValue.setPreferredSize(dimension);
    textFieldValue.addFocusListener(new FocusListener() {
      String prevValue = value;

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldValue.getText().trim().toLowerCase();
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
          textFieldValue.setText(prevValue);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              textFieldValue.updateUI();
              textFieldValue.requestFocus();
              textFieldValue.selectAll();
            }
          });
          return;
        } else {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              textFieldValue.updateUI();
            }
          });
        }
        prevValue = inputText;

        // everything is okay with the new value name, lets rename everything
        final List<String> newValues = MapXmlHelper.getGameSettingsMap().get(currentRowName);
        newValues.set(0, inputText);
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldValue.selectAll();
      }
    });

    dimension = comboBoxEditable.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    comboBoxEditable.setPreferredSize(dimension);
    comboBoxEditable.setSelectedIndex(Arrays.binarySearch(selectionTrueFalse, editable));
    comboBoxEditable.addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent arg0) {
        // everything is okay with the new value name, lets rename everything
        final List<String> newValues = MapXmlHelper.getGameSettingsMap().get(currentRowName);
        newValues.set(1, (String) comboBoxEditable.getSelectedItem());
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });

    dimension = textFieldMinNumber.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldMinNumber.setPreferredSize(dimension);
    textFieldMinNumber.setEnabled(!isBoolean);
    textFieldMinNumber.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldMinNumber.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          if (newValue < 0)
            throw new NumberFormatException();
          final Triple<String, String, Integer> oldTriple = MapXmlHelper.getPlayerSequenceMap().get(currentRowName);
          MapXmlHelper.getPlayerSequenceMap().put(currentRowName,
              Triple.of(oldTriple.getFirst(), oldTriple.getSecond(), newValue));
        } catch (NumberFormatException e) {
          textFieldMinNumber.setText("0");
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              textFieldMinNumber.updateUI();
              textFieldMinNumber.requestFocus();
              textFieldMinNumber.selectAll();
            }
          });
          return;
        }
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldMinNumber.selectAll();
      }
    });

    dimension = textFieldMaxNumber.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldMaxNumber.setPreferredSize(dimension);
    textFieldMaxNumber.setEnabled(!isBoolean);
    textFieldMaxNumber.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = textFieldMaxNumber.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          if (newValue < 0)
            throw new NumberFormatException();
          final Triple<String, String, Integer> oldTriple = MapXmlHelper.getPlayerSequenceMap().get(currentRowName);
          MapXmlHelper.getPlayerSequenceMap().put(currentRowName,
              Triple.of(oldTriple.getFirst(), oldTriple.getSecond(), newValue));
        } catch (NumberFormatException e) {
          textFieldMaxNumber.setText("0");
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              textFieldMaxNumber.updateUI();
              textFieldMaxNumber.requestFocus();
              textFieldMaxNumber.selectAll();
            }
          });
          return;
        }
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        textFieldMaxNumber.selectAll();
      }
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(textFieldValue);
    componentList.add(comboBoxSettingName);
    componentList.add(comboBoxEditable);
    componentList.add(textFieldMinNumber);
    componentList.add(textFieldMaxNumber);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(comboBoxSettingName, gbc_template);

    final GridBagConstraints gbc_tValue = (GridBagConstraints) gbc_template.clone();
    gbc_tValue.gridx = 1;
    parent.add(textFieldValue, gbc_tValue);

    final GridBagConstraints gbc_tEditable = (GridBagConstraints) gbc_template.clone();
    gbc_tEditable.gridx = 2;
    parent.add(comboBoxEditable, gbc_tEditable);

    final GridBagConstraints gbc_tMinNumber = (GridBagConstraints) gbc_template.clone();
    gbc_tMinNumber.gridx = 3;
    parent.add(textFieldMinNumber, gbc_tMinNumber);

    final GridBagConstraints gbc_tMaxNumber = (GridBagConstraints) gbc_template.clone();
    gbc_tMaxNumber.gridx = 4;
    parent.add(textFieldMaxNumber, gbc_tMaxNumber);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 5;
    parent.add(buttonRemovePerRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final GameSettingsRow newGameSettingsRow = (GameSettingsRow) newRow;
    this.comboBoxSettingName.setSelectedIndex(newGameSettingsRow.comboBoxSettingName.getSelectedIndex());
    this.textFieldValue.setText(newGameSettingsRow.textFieldValue.getText());
    this.comboBoxEditable.setSelectedIndex(newGameSettingsRow.comboBoxEditable.getSelectedIndex());
    this.textFieldMinNumber.setText(newGameSettingsRow.textFieldMinNumber.getText());
    this.textFieldMaxNumber.setText(newGameSettingsRow.textFieldMaxNumber.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXmlHelper.getGameSettingsMap().remove(currentRowName);
  }
}
