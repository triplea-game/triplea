package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


class UnitDefinitionsRow extends DynamicRow {
  private JTextField tUnitName;
  private JTextField tBuyCost;
  private JTextField tBuyQuantity;

  public UnitDefinitionsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String unitName,
      final int buyCost, final int buyQuantity) {
    super(unitName, parentRowPanel, stepActionPanel);

    tUnitName = new JTextField(unitName);
    final Integer buyCostInteger = Integer.valueOf(buyCost);
    tBuyCost = new JTextField(buyCostInteger == null ? "0" : Integer.toString(buyCostInteger));
    final Integer buyQuantityInteger = Integer.valueOf(buyQuantity);
    tBuyQuantity = new JTextField(buyQuantityInteger == null ? "1" : Integer.toString(buyQuantityInteger));

    Dimension dimension = tUnitName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    tUnitName.setPreferredSize(dimension);
    tUnitName.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tUnitName.getText().trim();
        if (currentRowName.equals(inputText))
          return;
        if (MapXMLHelper.unitDefinitions.containsKey(inputText)) {
          tUnitName.selectAll();
          JOptionPane.showMessageDialog(stepActionPanel, "Unit '" + inputText + "' already exists.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          parentRowPanel.setDataIsConsistent(false);
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              tUnitName.updateUI();
              tUnitName.requestFocus();
              tUnitName.selectAll();
            }
          });
          return;
        }
        // everything is okay with the new player namer, lets rename everything
        final List<Integer> values = MapXMLHelper.unitDefinitions.get(currentRowName);
        MapXMLHelper.unitDefinitions.put(inputText, values);
        if (!MapXMLHelper.productionFrontiers.isEmpty()) {
          for (final Entry<String, List<String>> productionFrontier : MapXMLHelper.productionFrontiers
              .entrySet()) {
            final List<String> frontierValues = productionFrontier.getValue();
            final int index = frontierValues.indexOf(currentRowName);
            if (index >= 0)
              frontierValues.set(index, inputText);
          }
        }
        currentRowName = inputText;
        parentRowPanel.setDataIsConsistent(true);
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tUnitName.selectAll();
      }
    });

    dimension = tBuyCost.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tBuyCost.setPreferredSize(dimension);
    tBuyCost.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tBuyCost.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          MapXMLHelper.unitDefinitions.get(unitName).set(0, newValue);
        } catch (NumberFormatException e) {
          tBuyCost.setText("0");
          MapXMLHelper.unitDefinitions.get(unitName).set(0, 0);
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
        }
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tBuyCost.selectAll();
      }
    });

    dimension = tBuyQuantity.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    tBuyQuantity.setPreferredSize(dimension);
    tBuyQuantity.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        String inputText = tBuyQuantity.getText().trim();
        try {
          final Integer newValue = Integer.parseInt(inputText);
          MapXMLHelper.unitDefinitions.get(unitName).set(1, newValue);
        } catch (NumberFormatException e) {
          tBuyQuantity.setText("1");
          MapXMLHelper.unitDefinitions.get(unitName).set(1, 1);
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
        }
      }

      @Override
      public void focusGained(FocusEvent arg0) {
        tBuyQuantity.selectAll();
      }
    });

  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
    componentList.add(tUnitName);
    componentList.add(tBuyCost);
    componentList.add(tBuyQuantity);
    return componentList;
  }

  @Override
  public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(tUnitName, gbc_template);

    final GridBagConstraints gbc_tBuyCost = (GridBagConstraints) gbc_template.clone();
    gbc_tBuyCost.gridx = 1;
    parent.add(tBuyCost, gbc_tBuyCost);

    final GridBagConstraints gbc_tBuyQuantity = (GridBagConstraints) gbc_template.clone();
    gbc_tBuyQuantity.gridx = 2;
    parent.add(tBuyQuantity, gbc_tBuyQuantity);

    final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
    gbc_bRemove.gridx = 3;
    parent.add(bRemoveRow, gbc_bRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final UnitDefinitionsRow newRowPlayerAndAlliancesRow = (UnitDefinitionsRow) newRow;
    this.tUnitName.setText(newRowPlayerAndAlliancesRow.tUnitName.getText());
    this.tBuyCost.setText(newRowPlayerAndAlliancesRow.tBuyCost.getText());
    this.tBuyQuantity.setText(newRowPlayerAndAlliancesRow.tBuyQuantity.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXMLHelper.unitDefinitions.remove(currentRowName);
    if (!MapXMLHelper.productionFrontiers.isEmpty()) {
      for (final Entry<String, List<String>> productionFrontier : MapXMLHelper.productionFrontiers.entrySet()) {
        final List<String> frontierValues = productionFrontier.getValue();
        final int index = frontierValues.indexOf(currentRowName);
        if (index >= 0)
          frontierValues.remove(index);
      }
    }
  }
}
