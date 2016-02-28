package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik von der Osten
 * 
 */
class ProductionFrontiersRow extends DynamicRow
{	

	private JComboBox<String> m_tUnitName;
	private String m_playerName;
	
	public ProductionFrontiersRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String playerName, final String unitName, final String[] unitNames)
	{
		super(unitName, parentRowPanel, stepActionPanel);		
		
		m_playerName = playerName;

		m_tUnitName = new JComboBox<String>(unitNames);	
		Dimension dimension = m_tUnitName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_MEDIUM;
		m_tUnitName.setPreferredSize(dimension);
		m_tUnitName.setSelectedIndex(Arrays.binarySearch(unitNames, unitName));	
		m_tUnitName.addFocusListener(new FocusListener() {
			
			String currentValue = unitName;
			
			@Override
			public void focusLost(FocusEvent e) {
				String newUnitValue = (String) m_tUnitName.getSelectedItem();
				if (!currentValue.equals(newUnitValue))
				{
					final ArrayList<String> playerUnitNames = MapXMLHelper.s_productionFrontiers.get(m_playerName);
					if (playerUnitNames.contains(newUnitValue))
					{
						JOptionPane.showMessageDialog(stepActionPanel, "Unit '" + newUnitValue + "' already selected fpr player '"+m_playerName+"'.", "Input error", JOptionPane.ERROR_MESSAGE);
						m_tUnitName.setSelectedItem(currentValue);
						// UI Update
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								stepActionPanel.revalidate();
								stepActionPanel.repaint();
							}
						});
					}
					else
					{
						playerUnitNames.add(newUnitValue);
						currentValue = newUnitValue;
						m_currentRowName = unitName;
					}
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
	}

	@Override
	protected ArrayList<JComponent> getComponentList()
	{
		final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
		componentList.add(m_tUnitName);
		return componentList;
	}
	
	@Override
	public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template)
	{
		parent.add(m_tUnitName, gbc_template);
		
		final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
		gbc_bRemove.gridx = 1;
		parent.add(m_bRemoveRow, gbc_bRemove);
	}
	
	@Override
	protected void adaptRowSpecifics(final DynamicRow newRow) {
		final ProductionFrontiersRow newRowPlayerAndAlliancesRow = (ProductionFrontiersRow) newRow;
		this.m_tUnitName.setSelectedIndex(newRowPlayerAndAlliancesRow.m_tUnitName.getSelectedIndex());
	}
	
	@Override
	protected void removeRowAction() {
		MapXMLHelper.s_productionFrontiers.get(m_playerName).remove(m_currentRowName);
	}
}
