package tools.map.xml.creator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


class UnitDefinitionsRow extends DynamicRow {
  private JTextField textFieldUnitName;
  private JTextField textFieldBuyCost;
  private JTextField textFieldBuyQuantity;

  public UnitDefinitionsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String unitName,
      final int buyCost, final int buyQuantity) {
    super(unitName, parentRowPanel, stepActionPanel);

    textFieldUnitName = new JTextField(unitName);
    final Integer buyCostInteger = buyCost;
    textFieldBuyCost = new JTextField(buyCostInteger == null ? "0" : Integer.toString(buyCostInteger));
    final Integer buyQuantityInteger = buyQuantity;
    textFieldBuyQuantity = new JTextField(buyQuantityInteger == null ? "1" : Integer.toString(buyQuantityInteger));

    Dimension dimension = textFieldUnitName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_MEDIUM;
    textFieldUnitName.setPreferredSize(dimension);
    MapXmlUIHelper.addNewFocusListenerForTextField(textFieldUnitName, () -> {
      final String inputText = textFieldUnitName.getText().trim();
      if (currentRowName.equals(inputText)) {
        return;
      }
      if (MapXmlHelper.getUnitDefinitionsMap().containsKey(inputText)) {
        textFieldUnitName.selectAll();
        JOptionPane.showMessageDialog(stepActionPanel, "Unit '" + inputText + "' already exists.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        parentRowPanel.setDataIsConsistent(false);
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            textFieldUnitName.updateUI();
            textFieldUnitName.requestFocus();
            textFieldUnitName.selectAll();
          }
        });
        return;
      }
      // everything is okay with the new player namer, lets rename everything
      final List<Integer> values = MapXmlHelper.getUnitDefinitionsMap().get(currentRowName);
      MapXmlHelper.getUnitDefinitionsMap().put(inputText, values);
      if (!MapXmlHelper.getProductionFrontiersMap().isEmpty()) {
        for (final Entry<String, List<String>> productionFrontier : MapXmlHelper.getProductionFrontiersMap()
            .entrySet()) {
          final List<String> frontierValues = productionFrontier.getValue();
          final int index = frontierValues.indexOf(currentRowName);
          if (index >= 0) {
            frontierValues.set(index, inputText);
          }
        }
      }
      currentRowName = inputText;
      parentRowPanel.setDataIsConsistent(true);
    });

    dimension = textFieldBuyCost.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldBuyCost.setPreferredSize(dimension);
    MapXmlUIHelper.addNewFocusListenerForTextField(textFieldBuyCost, () -> {
      final String inputText = textFieldBuyCost.getText().trim();
      try {
        final Integer newValue = Integer.parseInt(inputText);
        MapXmlHelper.getUnitDefinitionsMap().get(unitName).set(0, newValue);
      } catch (final NumberFormatException e) {
        textFieldBuyCost.setText("0");
        MapXmlHelper.getUnitDefinitionsMap().get(unitName).set(0, 0);
        JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
            JOptionPane.ERROR_MESSAGE);
      }
    });

    dimension = textFieldBuyQuantity.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldBuyQuantity.setPreferredSize(dimension);
    MapXmlUIHelper.addNewFocusListenerForTextField(textFieldBuyQuantity, () -> {
      final String inputText = textFieldBuyQuantity.getText().trim();
      try {
        final Integer newValue = Integer.parseInt(inputText);
        MapXmlHelper.getUnitDefinitionsMap().get(unitName).set(1, newValue);
      } catch (final NumberFormatException e) {
        textFieldBuyQuantity.setText("1");
        MapXmlHelper.getUnitDefinitionsMap().get(unitName).set(1, 1);
        JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
            JOptionPane.ERROR_MESSAGE);
      }
    });

  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<>();
    componentList.add(textFieldUnitName);
    componentList.add(textFieldBuyCost);
    componentList.add(textFieldBuyQuantity);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(textFieldUnitName, gbc_template);

    final GridBagConstraints gbc_tBuyCost = (GridBagConstraints) gbc_template.clone();
    gbc_tBuyCost.gridx = 1;
    parent.add(textFieldBuyCost, gbc_tBuyCost);

    final GridBagConstraints gbc_tBuyQuantity = (GridBagConstraints) gbc_template.clone();
    gbc_tBuyQuantity.gridx = 2;
    parent.add(textFieldBuyQuantity, gbc_tBuyQuantity);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 3;
    parent.add(buttonRemovePerRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final UnitDefinitionsRow newRowPlayerAndAlliancesRow = (UnitDefinitionsRow) newRow;
    this.textFieldUnitName.setText(newRowPlayerAndAlliancesRow.textFieldUnitName.getText());
    this.textFieldBuyCost.setText(newRowPlayerAndAlliancesRow.textFieldBuyCost.getText());
    this.textFieldBuyQuantity.setText(newRowPlayerAndAlliancesRow.textFieldBuyQuantity.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXmlHelper.getUnitDefinitionsMap().remove(currentRowName);
    if (!MapXmlHelper.getProductionFrontiersMap().isEmpty()) {
      for (final Entry<String, List<String>> productionFrontier : MapXmlHelper.getProductionFrontiersMap().entrySet()) {
        final List<String> frontierValues = productionFrontier.getValue();
        final int index = frontierValues.indexOf(currentRowName);
        if (index >= 0) {
          frontierValues.remove(index);
        }
      }
    }
  }
}
