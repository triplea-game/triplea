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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik von der Osten
 * 
 */
class TechnologyDefinitionsRow extends DynamicRow
{	
	private JTextField m_tTechnologyName;
	private JComboBox<String> m_tPlayerName;
	private JComboBox<String> m_tAlreadyEnabled;
	public static String[] selectionTrueFalse = {"false","true"};
	
	public TechnologyDefinitionsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String technologyName, final String playerName, final String[] playerNames, final String alreadyEnabled)
	{
		super(technologyName+"_"+playerName, parentRowPanel, stepActionPanel);
		
		m_tTechnologyName = new JTextField(technologyName);	
		m_tPlayerName = new JComboBox<String>(playerNames);
		m_tAlreadyEnabled = new JComboBox<String>(selectionTrueFalse);
		
		Dimension dimension = m_tTechnologyName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_LARGE;
		m_tTechnologyName.setPreferredSize(dimension);
		m_tTechnologyName.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tTechnologyName.getText().trim();
				final String curr_playerName = (String) m_tPlayerName.getSelectedItem();
				if (m_currentRowName.startsWith(inputText + "_"))
					return;
				final String newRowName = inputText+"_"+curr_playerName;
				if (MapXMLHelper.s_technologyDefinitions.containsKey(newRowName))
				{
					JOptionPane.showMessageDialog(stepActionPanel, "Technology '" + inputText + "' already exists for player '"+curr_playerName+"'.", "Input error", JOptionPane.ERROR_MESSAGE);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tTechnologyName.requestFocus();
							m_tTechnologyName.selectAll();
						}
					});
					return;
				}
				// everything is okay with the new technology name, lets rename everything
				final ArrayList<String> newValues = MapXMLHelper.s_technologyDefinitions.get(m_currentRowName);
				MapXMLHelper.s_technologyDefinitions.remove(m_currentRowName);
				MapXMLHelper.s_technologyDefinitions.put(newRowName,newValues);
				m_currentRowName = newRowName;
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tTechnologyName.selectAll();
			}
		});

		dimension = m_tPlayerName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_MEDIUM;
		m_tPlayerName.setPreferredSize(dimension);
		m_tPlayerName.setSelectedIndex(Arrays.binarySearch(playerNames, playerName));
		m_tPlayerName.addFocusListener(new FocusListener()
		{
			int prevSelectedIndex = m_tPlayerName.getSelectedIndex();
			@Override
			public void focusLost(FocusEvent arg0)
			{
				if (prevSelectedIndex == m_tPlayerName.getSelectedIndex())
					return;
				String techInputText = m_tTechnologyName.getText().trim();
				final String curr_playerName = (String) m_tPlayerName.getSelectedItem();
				if (m_currentRowName.endsWith("_" + curr_playerName))
					return;
				final String newRowName = techInputText+"_"+curr_playerName;
				if (MapXMLHelper.s_technologyDefinitions.containsKey(newRowName))
				{
					JOptionPane.showMessageDialog(stepActionPanel, "Technology '" + techInputText + "' already exists for player '"+curr_playerName+"'.", "Input error", JOptionPane.ERROR_MESSAGE);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tPlayerName.setSelectedIndex(prevSelectedIndex);
							m_tPlayerName.requestFocus();
						}
					});
					return;
				}
				// everything is okay with the new technology name, lets rename everything
				final ArrayList<String> newValues = MapXMLHelper.s_technologyDefinitions.get(m_currentRowName);
				MapXMLHelper.s_technologyDefinitions.remove(m_currentRowName);
				MapXMLHelper.s_technologyDefinitions.put(newRowName,newValues);
				m_currentRowName = newRowName;
				prevSelectedIndex = m_tPlayerName.getSelectedIndex();
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
			}
		});

		dimension = m_tAlreadyEnabled.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tAlreadyEnabled.setPreferredSize(dimension);
		m_tAlreadyEnabled.setSelectedIndex(Arrays.binarySearch(selectionTrueFalse, alreadyEnabled));
		m_tAlreadyEnabled.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent arg0)
			{
				// everything is okay with the new technology name, lets rename everything
				final ArrayList<String> newValues = MapXMLHelper.s_technologyDefinitions.get(m_currentRowName);
				newValues.set(1, (String)m_tAlreadyEnabled.getSelectedItem());
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
			}
		});
	}

	@Override
	protected ArrayList<JComponent> getComponentList()
	{
		final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
		componentList.add(m_tTechnologyName);
		componentList.add(m_tPlayerName);
		componentList.add(m_tAlreadyEnabled);
		return componentList;
	}
	
	@Override
	public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template)
	{
		parent.add(m_tTechnologyName, gbc_template);
		
		final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
		gbc_tClassName.gridx = 1;
		parent.add(m_tPlayerName, gbc_tClassName);
		
		final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
		gbc_tDisplayName.gridx = 2;
		parent.add(m_tAlreadyEnabled, gbc_tDisplayName);
		
		final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
		gbc_bRemove.gridx = 3;
		parent.add(m_bRemoveRow, gbc_bRemove);
	}
	
	@Override
	protected void adaptRowSpecifics(final DynamicRow newRow) {
		final TechnologyDefinitionsRow newTechnologyDefinitionsRow = (TechnologyDefinitionsRow) newRow;
		this.m_tTechnologyName.setText(newTechnologyDefinitionsRow.m_tTechnologyName.getText());
		this.m_tPlayerName.setSelectedIndex(newTechnologyDefinitionsRow.m_tPlayerName.getSelectedIndex());
		this.m_tAlreadyEnabled.setSelectedIndex(newTechnologyDefinitionsRow.m_tAlreadyEnabled.getSelectedIndex());
	}
	
	@Override
	protected void removeRowAction() {
		MapXMLHelper.s_technologyDefinitions.remove(m_currentRowName);
	}
}
