package games.strategy.engine.data.properties;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PropertiesUi extends JPanel {
  private static final long serialVersionUID = 3870459799384582310L;
  private int nextRow;
  private int labelColumn;

  public PropertiesUi(final GameProperties gameProperties, final boolean editable) {
    this(gameProperties.getEditableProperties(), editable);
  }

  public PropertiesUi(final List<? extends IEditableProperty> properties, final boolean editable) {
    init();
    for (final IEditableProperty property : properties) {
      // Limit it to 14 rows then start a new column
      // Don't know if this is the most elegant solution, but it works.
      if (nextRow >= 15) {
        labelColumn += 2;
        nextRow = 0;
      }
      if (editable) {
        addItem(property.getName(), property.getEditorComponent(), property.getDescription(), property.getRowsNeeded());
      } else {
        addItem(property.getName(), property.getViewComponent(), property.getDescription(), property.getRowsNeeded());
      }
    }
  }

  private void init() {
    setLayout(new GridBagLayout());
    // Create a blank label to use as a vertical fill so that the
    // label/item pairs are aligned to the top of the panel and are not
    // grouped in the centre if the parent component is taller than
    // the preferred size of the panel.
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 99;
    constraints.insets = new Insets(10, 0, 0, 0);
    constraints.weighty = 1.0;
    constraints.fill = GridBagConstraints.VERTICAL;
    final JLabel verticalFillLabel = new JLabel();
    add(verticalFillLabel, constraints);
  }

  private void addItem(final String labelText, final JComponent item, final String tooltip, final int rowsNeeded) {
    // Create the label and its constraints
    final JLabel label = new JLabel(labelText);
    final GridBagConstraints labelConstraints = new GridBagConstraints();
    labelConstraints.gridx = labelColumn;
    labelConstraints.gridy = nextRow;
    labelConstraints.gridheight = rowsNeeded;
    labelConstraints.insets = new Insets(10, 10, 0, 0);
    labelConstraints.anchor = GridBagConstraints.NORTHEAST;
    labelConstraints.fill = GridBagConstraints.NONE;
    add(label, labelConstraints);
    // Add the component with its constraints
    final GridBagConstraints itemConstraints = new GridBagConstraints();
    itemConstraints.gridx = labelColumn + 1;
    itemConstraints.gridy = nextRow;
    itemConstraints.gridheight = rowsNeeded;
    itemConstraints.insets = new Insets(10, 10, 0, 10);
    itemConstraints.weightx = 1.0;
    itemConstraints.anchor = GridBagConstraints.WEST;
    itemConstraints.fill = GridBagConstraints.NONE;
    add(item, itemConstraints);
    if ((tooltip != null) && (tooltip.length() > 0)) {
      label.setToolTipText(tooltip);
      item.setToolTipText(tooltip);
    }
    nextRow += rowsNeeded;
  }
}
