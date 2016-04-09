package util.triplea.mapXmlCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


class UnitAttatchmentsRow extends DynamicRow {

  private JTextField textFieldAttatchmentName;
  private JTextField textFieldValue;

  public UnitAttatchmentsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String unitName,
      final String attatchmentName, final String value) {
    super(attatchmentName + "_" + unitName, parentRowPanel, stepActionPanel);

    textFieldAttatchmentName = new JTextField(attatchmentName);
    Dimension dimension = textFieldAttatchmentName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    textFieldAttatchmentName.setPreferredSize(dimension);
    textFieldAttatchmentName.addFocusListener(new FocusListener() {

      String currentValue = attatchmentName;

      @Override
      public void focusLost(FocusEvent e) {
        String newAttatchmentName = (String) textFieldAttatchmentName.getText().trim();
        if (!currentValue.equals(newAttatchmentName)) {
          final String newUnitAttatchmentKey = newAttatchmentName + "_" + unitName;
          if (MapXmlHelper.getUnitAttatchmentsMap().containsKey(newUnitAttatchmentKey)) {
            JOptionPane.showMessageDialog(stepActionPanel,
                "Attatchment '" + newAttatchmentName + "' already exists for unit '" + unitName + "'.", "Input error",
                JOptionPane.ERROR_MESSAGE);
            parentRowPanel.setDataIsConsistent(false);
            // UI Update
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                stepActionPanel.revalidate();
                stepActionPanel.repaint();
                textFieldAttatchmentName.requestFocus();
              }
            });
            return;
          } else {
            final String oldUnitAttatchmentKey = currentValue + "_" + unitName;
            MapXmlHelper.getUnitAttatchmentsMap().put(newAttatchmentName,
                MapXmlHelper.getUnitAttatchmentsMap().get(oldUnitAttatchmentKey));
            MapXmlHelper.getUnitAttatchmentsMap().remove(oldUnitAttatchmentKey);
            currentValue = newAttatchmentName;
          }
          parentRowPanel.setDataIsConsistent(true);
        }
      }

      @Override
      public void focusGained(FocusEvent e) {
        textFieldAttatchmentName.selectAll();
      }
    });


    textFieldValue = new JTextField(value);
    dimension = textFieldValue.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldValue.setPreferredSize(dimension);
    textFieldValue.addFocusListener(new FocusListener() {

      String prevValue = value;

      @Override
      public void focusLost(FocusEvent e) {
        String inputText = textFieldValue.getText().trim().toLowerCase();
        try {
          if (Integer.parseInt(inputText) < 0)
            throw new NumberFormatException();
        } catch (NumberFormatException nfe) {
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          textFieldValue.setText(prevValue);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              textFieldValue.updateUI();
              textFieldValue.requestFocus();
              textFieldValue.selectAll();
            }
          });
          return;
        }
        prevValue = inputText;

        // everything is okay with the new value name, lets rename everything
        final List<String> newValues = MapXmlHelper.getUnitAttatchmentsMap().get(currentRowName);
        newValues.set(1, inputText);
      }

      @Override
      public void focusGained(FocusEvent e) {
        textFieldValue.selectAll();
      }
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(textFieldAttatchmentName);
    componentList.add(textFieldValue);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(textFieldAttatchmentName, gbc_template);

    final GridBagConstraints gbc_tValue = (GridBagConstraints) gbc_template.clone();
    gbc_tValue.gridx = 1;
    parent.add(textFieldValue, gbc_tValue);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 2;
    parent.add(buttonRemovePerRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final UnitAttatchmentsRow newRowPlayerAndAlliancesRow = (UnitAttatchmentsRow) newRow;
    this.textFieldAttatchmentName.setText(newRowPlayerAndAlliancesRow.textFieldAttatchmentName.getText());
    this.textFieldValue.setText(newRowPlayerAndAlliancesRow.textFieldValue.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXmlHelper.getUnitAttatchmentsMap().remove(currentRowName);
  }
}
