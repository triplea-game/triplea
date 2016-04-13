package util.triplea.mapXmlCreator;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.Sets;

/**
 * Base class for *Panel classes based on DynamicRow class with which it is interlinked.
 * Subclasses list row entries after a header line with labels and ends with a button row which
 * mostly consists only of an Add-button.
 * Each subclass can override the main "Auto-Fill" button with an own action.
 * Furthermore, this class contains a boolean variable describing whether the inputed data is consistent.
 *
 * @see DynamicRow
 *
 */
public abstract class DynamicRowsPanel {

  protected static Optional<DynamicRowsPanel> me = Optional.empty();

  private final JPanel ownPanel;

  protected JPanel getOwnPanel() {
    return ownPanel;
  }

  private final JPanel stepActionPanel;
  private final ArrayList<JButton> finalRowButtons = new ArrayList<JButton>();
  boolean dataIsConsistent = true;

  public LinkedHashSet<DynamicRow> rows = Sets.newLinkedHashSet();

  protected static void layout(final MapXmlCreator mapXmlCreator) {
    if (me.isPresent()) {
      me.get().resetRows();
      mapXmlCreator.setAutoFillActionListener(me.get().getAutoFillAction());
    }
  }

  public boolean dataIsConsistent() {
    return dataIsConsistent;
  }

  void setDataIsConsistent(final boolean dataIsConsistent) {
    this.dataIsConsistent = dataIsConsistent;
  }

  protected DynamicRowsPanel(final JPanel stepActionPanel) {
    this.stepActionPanel = stepActionPanel;
    ownPanel = new JPanel();
    final Dimension size = stepActionPanel.getSize();
    final JScrollPane js = new JScrollPane(ownPanel);
    js.setBorder(null);
    stepActionPanel.setLayout(new BorderLayout());
    stepActionPanel.add(js, BorderLayout.CENTER);
    stepActionPanel.setPreferredSize(size);
  }

  protected void resetRows() {
    initialize();
    ownPanel.removeAll();
    // re-register scollPane on stepActionPanel
    final Container viewPort = ownPanel.getParent();
    final Container scrollPane = viewPort.getParent();
    if (scrollPane.getParent() == null) {
      if (!(stepActionPanel.getLayout() instanceof BorderLayout)) {
        stepActionPanel.setLayout(new BorderLayout());
      }
      stepActionPanel.add(scrollPane, BorderLayout.CENTER);
    }
    layoutComponents();
    dataIsConsistent = true;
  }

  private void initialize() {
    finalRowButtons.clear();
    rows.clear();
    me.get().initializeSpecifics();
  }

  public void removeComponents(final ArrayList<JComponent> componentList) {
    for (final JComponent component : componentList) {
      ownPanel.remove(component);
    }
  }

  protected void addButton(final JButton newButton) {
    finalRowButtons.add(newButton);
  }

  protected void setRows(final GridBagLayout gblPanel, final int inputRows) {
    final int totalRows = inputRows + 3; // header row, button row, remaining space row
    gblPanel.rowHeights = new int[totalRows];
    gblPanel.rowWeights = new double[totalRows];
    for (int i = 0; i < totalRows; ++i) {
      gblPanel.rowHeights[i] = 32;
      gblPanel.rowWeights[i] = 0.0;
    }
    gblPanel.rowHeights[totalRows - 1] = 0;
    gblPanel.rowWeights[totalRows - 1] = Double.MIN_VALUE;
  }

  /**
   * @return number of rows
   */
  public int countRows() {
    return rows.size() + 1;
  }

  protected void addRow(final DynamicRow newRow) {
    removeFinalButtonRow();

    final int countPlayers = countRows();
    newRow.addToParentComponentWithGbc(ownPanel, countPlayers,
        MapXmlUIHelper.getGbcDefaultTemplateWith(0, countPlayers));
    rows.add(newRow);

    final int finalButtonGridY = countPlayers + 1;
    addFinalButtonRow(MapXmlUIHelper.getGbcDefaultTemplateWith(0, finalButtonGridY));
  }

  protected void addFinalButtonRow(final GridBagConstraints gbcTemplate) {
    int xValue = 0;
    for (final JButton button : finalRowButtons) {
      final GridBagConstraints gbcCurrentButton = (GridBagConstraints) gbcTemplate.clone();
      gbcCurrentButton.gridx = xValue;
      ++xValue;
      ownPanel.add(button, gbcCurrentButton);
    }
  }

  public void removeFinalButtonRow() {
    for (final JButton button : finalRowButtons) {
      ownPanel.remove(button);
    }
  }

  public LinkedHashSet<DynamicRow> getRows() {
    return rows;
  }

  abstract protected ActionListener getAutoFillAction();

  abstract protected void layoutComponents();

  abstract protected void initializeSpecifics();

  abstract protected void setColumns(GridBagLayout gblPanel);

  protected void repaintOwnPanel() {
    SwingUtilities.invokeLater(() -> {
      resetRows();
      ownPanel.revalidate();
      ownPanel.repaint();
      ownPanel.requestFocus();
    });
  }


}
