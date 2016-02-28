package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Map.Entry;

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
class UnitDefinitionsRow extends DynamicRow
{	
	private JTextField m_tUnitName;
	private JTextField m_tBuyCost;
	private JTextField m_tBuyQuantity;
	
	public UnitDefinitionsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String unitName, final int buyCost, final int buyQuantity)
	{
		super(unitName, parentRowPanel, stepActionPanel);
		
		m_tUnitName = new JTextField(unitName);	
		final Integer buyCostInteger = Integer.valueOf(buyCost);
		m_tBuyCost = new JTextField(buyCostInteger == null ? "0" : Integer.toString(buyCostInteger));	
		final Integer buyQuantityInteger = Integer.valueOf(buyQuantity);
		m_tBuyQuantity = new JTextField(buyQuantityInteger == null ? "1" : Integer.toString(buyQuantityInteger));
		
		Dimension dimension = m_tUnitName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_MEDIUM;
		m_tUnitName.setPreferredSize(dimension);
		m_tUnitName.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tUnitName.getText().trim();
				if (m_currentRowName.equals(inputText))
					return;
				if (MapXMLHelper.s_unitDefinitions.containsKey(inputText))
				{
					m_tUnitName.selectAll();
					JOptionPane.showMessageDialog(stepActionPanel, "Unit '" + inputText + "' already exists.", "Input error", JOptionPane.ERROR_MESSAGE);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tUnitName.updateUI();
							m_tUnitName.requestFocus();
							m_tUnitName.selectAll();
						}
					});
					return;
				}
				// everything is okay with the new player namer, lets rename everything
				final ArrayList<Integer> values = MapXMLHelper.s_unitDefinitions.get(m_currentRowName);
				MapXMLHelper.s_unitDefinitions.put(inputText, values);
				if (!MapXMLHelper.s_productionFrontiers.isEmpty())
				{
					for (final Entry<String, ArrayList<String>> productionFrontier : MapXMLHelper.s_productionFrontiers.entrySet())
					{
						final ArrayList<String> frontierValues = productionFrontier.getValue();
						final int index = frontierValues.indexOf(m_currentRowName);
						if (index >= 0)
							frontierValues.set(index,inputText);
					}
				}
				m_currentRowName = inputText;
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tUnitName.selectAll();
			}
		});

		dimension = m_tBuyCost.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tBuyCost.setPreferredSize(dimension);
		m_tBuyCost.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tBuyCost.getText().trim();
				try
				{
					final Integer newValue = Integer.parseInt(inputText);
					MapXMLHelper.s_unitDefinitions.get(unitName).set(0, newValue);
				} catch (NumberFormatException e)
				{
					m_tBuyCost.setText("0");
					MapXMLHelper.s_unitDefinitions.get(unitName).set(0, 0);
					JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tBuyCost.selectAll();
			}
		});
	
		dimension = m_tBuyQuantity.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tBuyQuantity.setPreferredSize(dimension);
		m_tBuyQuantity.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tBuyQuantity.getText().trim();
				try
				{
					final Integer newValue = Integer.parseInt(inputText);
					MapXMLHelper.s_unitDefinitions.get(unitName).set(1, newValue);
				} catch (NumberFormatException e)
				{
					m_tBuyQuantity.setText("1");
					MapXMLHelper.s_unitDefinitions.get(unitName).set(1, 1);
					JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tBuyQuantity.selectAll();
			}
		});

	}

	@Override
	protected ArrayList<JComponent> getComponentList()
	{
		final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
		componentList.add(m_tUnitName);
		componentList.add(m_tBuyCost);
		componentList.add(m_tBuyQuantity);
		return componentList;
	}
	
	@Override
	public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template)
	{
		parent.add(m_tUnitName, gbc_template);
		
		final GridBagConstraints gbc_tBuyCost = (GridBagConstraints) gbc_template.clone();
		gbc_tBuyCost.gridx = 1;
		parent.add(m_tBuyCost, gbc_tBuyCost);
		
		final GridBagConstraints gbc_tBuyQuantity = (GridBagConstraints) gbc_template.clone();
		gbc_tBuyQuantity.gridx = 2;
		parent.add(m_tBuyQuantity, gbc_tBuyQuantity);
		
		final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
		gbc_bRemove.gridx = 3;
		parent.add(m_bRemoveRow, gbc_bRemove);
	}
	
	@Override
	protected void adaptRowSpecifics(final DynamicRow newRow) {
		final UnitDefinitionsRow newRowPlayerAndAlliancesRow = (UnitDefinitionsRow) newRow;
		this.m_tUnitName.setText(newRowPlayerAndAlliancesRow.m_tUnitName.getText());
		this.m_tBuyCost.setText(newRowPlayerAndAlliancesRow.m_tBuyCost.getText());
		this.m_tBuyQuantity.setText(newRowPlayerAndAlliancesRow.m_tBuyQuantity.getText());
	}
	
	@Override
	protected void removeRowAction() {
		MapXMLHelper.s_unitDefinitions.remove(m_currentRowName);
		if (!MapXMLHelper.s_productionFrontiers.isEmpty())
		{
			for (final Entry<String, ArrayList<String>> productionFrontier : MapXMLHelper.s_productionFrontiers.entrySet())
			{
				final ArrayList<String> frontierValues = productionFrontier.getValue();
				final int index = frontierValues.indexOf(m_currentRowName);
				if (index >= 0)
					frontierValues.remove(index);
			}
		}
	}
}
