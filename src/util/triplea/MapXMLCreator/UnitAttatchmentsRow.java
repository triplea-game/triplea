package util.triplea.MapXMLCreator;

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

  private JTextField tAttatchmentName;
  private JTextField tValue;
  private String unitName;

  public UnitAttatchmentsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String unitName,
      final String attatchmentName, final String value) {
    super(attatchmentName + "_" + unitName, parentRowPanel, stepActionPanel);

    this.unitName = unitName;

    tAttatchmentName = new JTextField(attatchmentName);
    Dimension dimension = tAttatchmentName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    tAttatchmentName.setPreferredSize(dimension);
    tAttatchmentName.addFocusListener(new FocusListener() {

      String currentValue = attatchmentName;

      @Override
      public void focusLost(FocusEvent e) {
        String newAttatchmentName = (String) tAttatchmentName.getText().trim();
        if (!currentValue.equals(newAttatchmentName)) {
          final String newUnitAttatchmentKey = newAttatchmentName + "_" + unitName;
          if (MapXMLHelper.unitAttatchments.containsKey(newUnitAttatchmentKey)) {
            JOptionPane.showMessageDialog(stepActionPanel,
                "Attatchment '" + newAttatchmentName + "' already exists for unit '" + unitName + "'.", "Input error",
                JOptionPane.ERROR_MESSAGE);
            parentRowPanel.setDataIsConsistent(false);
            // UI Update
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                stepActionPanel.revalidate();
                stepActionPanel.repaint();
                tAttatchmentName.requestFocus();
              }
            });
            return;
          } else {
            final String oldUnitAttatchmentKey = currentValue + "_" + unitName;
            MapXMLHelper.unitAttatchments.put(newAttatchmentName,
                MapXMLHelper.unitAttatchments.get(oldUnitAttatchmentKey));
            MapXMLHelper.unitAttatchments.remove(oldUnitAttatchmentKey);
            currentValue = newAttatchmentName;
          }
          parentRowPanel.setDataIsConsistent(true);
        }
      }

      @Override
      public void focusGained(FocusEvent e) {
        tAttatchmentName.selectAll();
      }
    });


    tValue = new JTextField(value);
    dimension = tValue.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tValue.setPreferredSize(dimension);
    tValue.addFocusListener(new FocusListener() {

      String prevValue = value;

      @Override
      public void focusLost(FocusEvent e) {
        String inputText = tValue.getText().trim().toLowerCase();
        try {
          if (Integer.parseInt(inputText) < 0)
            throw new NumberFormatException();
        } catch (NumberFormatException nfe) {
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          tValue.setText(prevValue);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tValue.updateUI();
              tValue.requestFocus();
              tValue.selectAll();
            }
          });
          return;
        }
        prevValue = inputText;

        // everything is okay with the new value name, lets rename everything
        final List<String> newValues = MapXMLHelper.unitAttatchments.get(currentRowName);
        newValues.set(1, inputText);
      }

      @Override
      public void focusGained(FocusEvent e) {
        tValue.selectAll();
      }
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(tAttatchmentName);
    componentList.add(tValue);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(tAttatchmentName, gbc_template);

    final GridBagConstraints gbc_tValue = (GridBagConstraints) gbc_template.clone();
    gbc_tValue.gridx = 1;
    parent.add(tValue, gbc_tValue);

    final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
    gbc_bRemove.gridx = 2;
    parent.add(bRemoveRow, gbc_bRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final UnitAttatchmentsRow newRowPlayerAndAlliancesRow = (UnitAttatchmentsRow) newRow;
    this.tAttatchmentName.setText(newRowPlayerAndAlliancesRow.tAttatchmentName.getText());
    this.tValue.setText(newRowPlayerAndAlliancesRow.tValue.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.unitAttatchments.remove(currentRowName);
  }
}
