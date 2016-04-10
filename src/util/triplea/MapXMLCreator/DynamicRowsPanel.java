package util.triplea.MapXMLCreator;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Base class for *Panel classes based on DynamicRow class with which it is interlinked.
 * Subclasses list row entries after a header line with labels and ends with a button row which
 * mostly consists only of an Add-button.
 * Each subclass can override the main "Auto-Fill" button with an own action.
 * Furthermore, this class contains a boolean variable describing whether the inputed data is consistent.
 * 
 * @see DynamicRow
 * @author Erik von der Osten
 * 
 */
public abstract class DynamicRowsPanel {

  protected static DynamicRowsPanel me = null;

  protected JPanel ownPanel;
  protected JPanel stepActionPanel;
  private ArrayList<JButton> finalRowButtons = new ArrayList<JButton>();
  boolean dataIsConsistent = true;

  public LinkedHashSet<DynamicRow> rows = new LinkedHashSet<DynamicRow>();

  protected static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    me.resetRows();
    me.setAutoFillAction(mapXMLCreator.autoFillButton);
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
    JScrollPane js = new JScrollPane(ownPanel);
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
      if (!(stepActionPanel.getLayout() instanceof BorderLayout))
        stepActionPanel.setLayout(new BorderLayout());
      stepActionPanel.add(scrollPane, BorderLayout.CENTER);
    }
    layoutComponents();
    dataIsConsistent = true;
  }

  private void initialize() {
    finalRowButtons.clear();
    rows.clear();
    me.initializeSpecifics();
  }

  private void setAutoFillAction(final JButton bAutoFill) {
    final ActionListener autoFillAction = me.getAutoFillAction();
    if (autoFillAction == null)
      bAutoFill.setEnabled(false);
    else {
      bAutoFill.setEnabled(true);
      for (final ActionListener curr_actionListener : bAutoFill.getActionListeners())
        bAutoFill.removeActionListener(curr_actionListener);
      bAutoFill.addActionListener(autoFillAction);
    }
  }

  public void removeComponents(final ArrayList<JComponent> componentList) {
    for (final JComponent component : componentList) {
      ownPanel.remove(component);
    }
  }

  protected void addButton(final JButton newButton) {
    finalRowButtons.add(newButton);
  }

  protected void setRows(GridBagLayout gbl_panel, final int inputRows) {
    final int totalRows = inputRows + 3; // header row, button row, remaining space row
    gbl_panel.rowHeights = new int[totalRows];
    gbl_panel.rowWeights = new double[totalRows];
    for (int i = 0; i < totalRows; ++i) {
      gbl_panel.rowHeights[i] = 32;
      gbl_panel.rowWeights[i] = 0.0;
    }
    gbl_panel.rowHeights[totalRows - 1] = 0;
    gbl_panel.rowWeights[totalRows - 1] = Double.MIN_VALUE;
  }


  protected void addRow(final DynamicRow newRow) {
    removeFinalButtonRow();

    final int countPlayers = rows.size() + 1;
    GridBagConstraints gbc_templateRow = new GridBagConstraints();
    gbc_templateRow.insets = new Insets(0, 0, 5, 5);
    gbc_templateRow.gridy = countPlayers;
    gbc_templateRow.gridx = 0;
    gbc_templateRow.anchor = GridBagConstraints.WEST;
    newRow.addToComponent(ownPanel, countPlayers, gbc_templateRow);
    rows.add(newRow);

    GridBagConstraints gbc_templateButton = (GridBagConstraints) gbc_templateRow.clone();
    gbc_templateButton.gridx = 0;
    gbc_templateButton.gridy = countPlayers + 1;
    addFinalButtonRow(gbc_templateButton);
  }

  protected void addFinalButtonRow(final GridBagConstraints gbc_template) {
    int xValue = 0;
    for (final JButton button : finalRowButtons) {
      final GridBagConstraints gbc_currentButton = (GridBagConstraints) gbc_template.clone();
      gbc_currentButton.gridx = xValue;
      ++xValue;
      ownPanel.add(button, gbc_currentButton);
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

  abstract protected void setColumns(GridBagLayout gbl_panel);


}
