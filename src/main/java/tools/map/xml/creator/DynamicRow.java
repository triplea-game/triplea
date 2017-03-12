package tools.map.xml.creator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.ui.SwingAction;

/**
 * Base class for the other *Row classes defining one removable input row.
 * Its functionality is highly interlinked DynamicRowsPanel.
 *
 * @see DynamicRowsPanel
 *
 */
public abstract class DynamicRow {

  final static int INPUT_FIELD_SIZE_LARGE = 150;
  final static int INPUT_FIELD_SIZE_MEDIUM = 120;
  final static int INPUT_FIELD_SIZE_SMALL = 55;

  private final DynamicRowsPanel parentRowPanel;
  protected String currentRowName;
  protected JButton buttonRemovePerRow;

  protected DynamicRow(final String rowName, final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel) {
    currentRowName = rowName;
    this.parentRowPanel = parentRowPanel;

    buttonRemovePerRow = new JButton("X");
    buttonRemovePerRow.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    final Dimension dimension = new Dimension(25, 20);
    buttonRemovePerRow.setPreferredSize(dimension);
    buttonRemovePerRow.addActionListener(SwingAction.of("Remove Row", e -> {
      removeRowAction();

      pushUpRowsTo(currentRowName);

      SwingUtilities.invokeLater(() -> {
        stepActionPanel.revalidate();
        stepActionPanel.repaint();
      });
    }));
  }

  public void addToParentComponent(final JComponent parent, final int rowIndex) {
    addToParentComponentWithGbc(parent, rowIndex, MapXmlUIHelper.getGbcDefaultTemplateWith(0, rowIndex));
  }

  public void addToParentComponentWithGbc(final JComponent parent, final int rowIndex,
      final GridBagConstraints gbcTemplate) {
    gbcTemplate.gridy = rowIndex;
    addToParentComponent(parent, gbcTemplate);
  }

  private void adaptRow(final DynamicRow newRow) {
    this.currentRowName = newRow.currentRowName;
    adaptRowSpecifics(newRow);
  }

  private void pushUpRowsTo(final String currentRowName) {
    removeRowByNameAndPushUpFollowingRows(currentRowName);

    parentRowPanel.removeFinalButtonRow();

    parentRowPanel.addFinalButtonRow(MapXmlUIHelper.getGbcDefaultTemplateWith(0, parentRowPanel.countRows()));
  }

  private void removeRowByNameAndPushUpFollowingRows(final String currentRowName) {
    // go to currentRowName row, update below rows and delete last row
    final Iterator<DynamicRow> iterRows = parentRowPanel.getRows().iterator();
    if (iterRows.hasNext()) {
      DynamicRow currRow = iterRows.next();
      while (iterRows.hasNext() && !currentRowName.equals(currRow.getRowName())) {
        currRow = iterRows.next();
      }
      while (iterRows.hasNext()) {
        final DynamicRow nextRow = iterRows.next();
        currRow.adaptRow(nextRow);
        currRow = nextRow;
      }
      currRow.removeFromStepPanel();
      iterRows.remove();
    }
  }

  private void removeFromStepPanel() {
    final ArrayList<JComponent> componentList = getComponentList();
    componentList.add(buttonRemovePerRow);

    parentRowPanel.removeComponents(componentList);
  }

  public String getRowName() {
    return currentRowName;
  }

  @Override
  public String toString() {
    return currentRowName;
  }

  abstract protected ArrayList<JComponent> getComponentList();

  abstract public void addToParentComponent(final JComponent parent, final GridBagConstraints gbc_template);

  abstract protected void adaptRowSpecifics(final DynamicRow newRow);

  abstract protected void removeRowAction();
}
