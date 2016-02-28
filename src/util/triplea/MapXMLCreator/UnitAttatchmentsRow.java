package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik von der Osten
 * 
 */
class UnitAttatchmentsRow extends DynamicRow
{	

	private JTextField m_tAttatchmentName;
	private JTextField m_tValue;
	private String m_unitName;
	
	public UnitAttatchmentsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String unitName, final String attatchmentName, final String value)
	{
		super(attatchmentName+"_"+unitName, parentRowPanel, stepActionPanel);		
		
		m_unitName = unitName;

		m_tAttatchmentName = new JTextField(attatchmentName);	
		Dimension dimension = m_tAttatchmentName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_LARGE;
		m_tAttatchmentName.setPreferredSize(dimension);	
		m_tAttatchmentName.addFocusListener(new FocusListener() {
			
			String currentValue = attatchmentName;
			
			@Override
			public void focusLost(FocusEvent e) {
				String newAttatchmentName = (String) m_tAttatchmentName.getText().trim();
				if (!currentValue.equals(newAttatchmentName))
				{
					final String newUnitAttatchmentKey = newAttatchmentName+"_"+m_unitName;
					if (MapXMLHelper.s_unitAttatchments.containsKey(newUnitAttatchmentKey))
					{
						JOptionPane.showMessageDialog(stepActionPanel, "Attatchment '" + newAttatchmentName + "' already exists for unit '"+m_unitName+"'.", "Input error", JOptionPane.ERROR_MESSAGE);
						parentRowPanel.setDataIsConsistent(false);
						// UI Update
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								stepActionPanel.revalidate();
								stepActionPanel.repaint();
								m_tAttatchmentName.requestFocus();
							}
						});
						return;
					}
					else
					{
						final String oldUnitAttatchmentKey = currentValue+"_"+m_unitName;
						MapXMLHelper.s_unitAttatchments.put(newAttatchmentName, MapXMLHelper.s_unitAttatchments.get(oldUnitAttatchmentKey));
						MapXMLHelper.s_unitAttatchments.remove(oldUnitAttatchmentKey);
						currentValue = newAttatchmentName;
					}
					parentRowPanel.setDataIsConsistent(true);
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				m_tAttatchmentName.selectAll();
			}
		});
		

		m_tValue = new JTextField(value);	
		dimension = m_tValue.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tValue.setPreferredSize(dimension);	
		m_tValue.addFocusListener(new FocusListener() {
			
			String prevValue = value;

			@Override
			public void focusLost(FocusEvent e) {
				String inputText = m_tValue.getText().trim().toLowerCase();
				try
				{
					if (Integer.parseInt(inputText) < 0)
						throw new NumberFormatException();
				} catch (NumberFormatException nfe)
				{
					JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
					m_tValue.setText(prevValue);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tValue.updateUI();
							m_tValue.requestFocus();
							m_tValue.selectAll();
						}
					});
					return;
				}
				prevValue = inputText;
				
				// everything is okay with the new value name, lets rename everything
				final ArrayList<String> newValues = MapXMLHelper.s_unitAttatchments.get(m_currentRowName);
				newValues.set(1, inputText);
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				m_tValue.selectAll();
			}
		});
	}

	@Override
	protected ArrayList<JComponent> getComponentList()
	{
		final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
		componentList.add(m_tAttatchmentName);
		componentList.add(m_tValue);
		return componentList;
	}
	
	@Override
	public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template)
	{
		parent.add(m_tAttatchmentName, gbc_template);

		final GridBagConstraints gbc_tValue = (GridBagConstraints) gbc_template.clone();
		gbc_tValue.gridx = 1;
		parent.add(m_tValue, gbc_tValue);
		
		final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
		gbc_bRemove.gridx = 2;
		parent.add(m_bRemoveRow, gbc_bRemove);
	}
	
	@Override
	protected void adaptRowSpecifics(final DynamicRow newRow) {
		final UnitAttatchmentsRow newRowPlayerAndAlliancesRow = (UnitAttatchmentsRow) newRow;
		this.m_tAttatchmentName.setText(newRowPlayerAndAlliancesRow.m_tAttatchmentName.getText());
		this.m_tValue.setText(newRowPlayerAndAlliancesRow.m_tValue.getText());
	}
	
	@Override
	protected void removeRowAction() {
		MapXMLHelper.s_unitAttatchments.remove(m_currentRowName);
	}
}
