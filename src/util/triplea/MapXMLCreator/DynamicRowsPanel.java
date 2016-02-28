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

	protected static DynamicRowsPanel s_me = null;
	
	protected JPanel m_ownPanel;
	protected JPanel m_stepActionPanel;
	private ArrayList<JButton> m_finalRowButtons = new ArrayList<JButton>();
	boolean m_dataIsConsistent = true;

	public LinkedHashSet<DynamicRow> m_rows = new LinkedHashSet<DynamicRow>();

	protected static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		s_me.resetRows();
		s_me.setAutoFillAction(mapXMLCreator.m_bAuto);
	}
	
	public boolean dataIsConsistent()
	{
		return m_dataIsConsistent;
	}
	
	void setDataIsConsistent(final boolean dataIsConsistent)
	{
		m_dataIsConsistent = dataIsConsistent;
	}

	protected DynamicRowsPanel(final JPanel stepActionPanel)
	{
		m_stepActionPanel = stepActionPanel;
		m_ownPanel = new JPanel();
		final Dimension size = stepActionPanel.getSize();
		JScrollPane js = new JScrollPane(m_ownPanel);
		js.setBorder(null);
		stepActionPanel.setLayout(new BorderLayout());
		stepActionPanel.add(js,BorderLayout.CENTER);
		stepActionPanel.setPreferredSize(size);
	}
	
	protected void resetRows()
	{
		initialize();
		m_ownPanel.removeAll();
		// re-register scollPane on stepActionPanel
		final Container viewPort = m_ownPanel.getParent();
		final Container scrollPane = viewPort.getParent();
		if (scrollPane.getParent() == null)
		{
			if (!(m_stepActionPanel.getLayout() instanceof BorderLayout))
				m_stepActionPanel.setLayout(new BorderLayout());
			m_stepActionPanel.add(scrollPane,BorderLayout.CENTER);
		}
		layoutComponents();
		m_dataIsConsistent = true;
	}
	
	private void initialize() {
		m_finalRowButtons.clear();
		m_rows.clear();
		s_me.initializeSpecifics();
	}

	private void setAutoFillAction(final JButton bAutoFill) {
		final ActionListener autoFillAction = s_me.getAutoFillAction();
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
			m_ownPanel.remove(component);
		}
	}
	
	protected void addButton(final JButton newButton)
	{
		m_finalRowButtons.add(newButton);
	}

	protected void setRows(GridBagLayout gbl_panel, final int inputRows) {
		final int totalRows = inputRows + 3; // header row, button row, remaining space row
		gbl_panel.rowHeights = new int[totalRows];
		gbl_panel.rowWeights = new double[totalRows];
		for (int i = 0; i < totalRows; ++i)
		{
			gbl_panel.rowHeights[i] = 32;
			gbl_panel.rowWeights[i] = 0.0;
		}
		gbl_panel.rowHeights[totalRows - 1] = 0;
		gbl_panel.rowWeights[totalRows - 1] = Double.MIN_VALUE;
	}

	
	protected void addRow(final DynamicRow newRow)
	{
		removeFinalButtonRow();

		final int countPlayers = m_rows.size() + 1;
		GridBagConstraints gbc_templateRow = new GridBagConstraints();
		gbc_templateRow.insets = new Insets(0, 0, 5, 5);
		gbc_templateRow.gridy = countPlayers;
		gbc_templateRow.gridx = 0;
		gbc_templateRow.anchor = GridBagConstraints.WEST;
		newRow.addToComponent(m_ownPanel, countPlayers, gbc_templateRow);
		m_rows.add(newRow);
		
		GridBagConstraints gbc_templateButton = (GridBagConstraints) gbc_templateRow.clone();
		gbc_templateButton.gridx = 0;
		gbc_templateButton.gridy = countPlayers + 1;
		addFinalButtonRow(gbc_templateButton);
	}

	protected void addFinalButtonRow(final GridBagConstraints gbc_template)
	{
		int xValue = 0;
		for (final JButton button : m_finalRowButtons) {
			final GridBagConstraints gbc_currentButton = (GridBagConstraints) gbc_template.clone();
			gbc_currentButton.gridx = xValue;
			++xValue;
			m_ownPanel.add(button, gbc_currentButton);
		}
	}
	
	public void removeFinalButtonRow()
	{
		for (final JButton button : m_finalRowButtons) {
			m_ownPanel.remove(button);
		}
	}

	public LinkedHashSet<DynamicRow> getRows() {
		return m_rows;
	}

	abstract protected ActionListener getAutoFillAction();
	abstract protected void layoutComponents();
	abstract protected void initializeSpecifics();
	abstract protected void setColumns(GridBagLayout gbl_panel);


}
