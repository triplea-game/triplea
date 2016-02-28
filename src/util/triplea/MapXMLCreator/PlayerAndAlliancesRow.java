package util.triplea.MapXMLCreator;

import games.strategy.util.Triple;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

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
class PlayerAndAlliancesRow extends DynamicRow
{	
	JTextField m_tPlayerName;
	JComboBox<String> m_tPlayerAlliance;
	JTextField m_tInitialResource;
	
	public PlayerAndAlliancesRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String playerName, final String allianceName, final String[] alliances, final int initialResource)
	{
		super(playerName, parentRowPanel, stepActionPanel);
		
		m_tPlayerName = new JTextField(playerName);
		m_tPlayerAlliance = new JComboBox<String>(alliances);		
		final Integer initialResourceInteger = Integer.valueOf(initialResource);
		m_tInitialResource = new JTextField(initialResourceInteger == null ? "0" : Integer.toString(initialResourceInteger));
		
		Dimension dimension = m_tPlayerName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_MEDIUM;
		m_tPlayerName.setPreferredSize(dimension);
		m_tPlayerName.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tPlayerName.getText().trim();
				if (m_currentRowName.equals(inputText))
					return;
				if (MapXMLHelper.s_playerName.contains(inputText))
				{
					m_tPlayerName.selectAll();
					JOptionPane.showMessageDialog(stepActionPanel, "Player '" + inputText + "' already exists.", "Input error", JOptionPane.ERROR_MESSAGE);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						
						@Override
						public void run()
						{
							m_tPlayerName.requestFocus();
						}
					});
					return;
				}
				// everything is okay with the new player namer, lets rename everything
				MapXMLHelper.s_playerName.remove(m_currentRowName);
				MapXMLHelper.s_playerName.add(inputText);
				MapXMLHelper.s_playerAlliance.remove(m_currentRowName);
				MapXMLHelper.s_playerAlliance.put(inputText, MapXMLHelper.s_playerAlliance.get(m_currentRowName));
				MapXMLHelper.s_playerInitResources.remove(m_currentRowName);
				MapXMLHelper.s_playerInitResources.put(inputText, MapXMLHelper.s_playerInitResources.get(m_currentRowName));
				if (!MapXMLHelper.s_playerSequence.isEmpty())
				{
					// Replace Player Names for Player Sequence
					final LinkedHashMap<String, Triple<String, String, Integer>> updatesPlayerSequence = new LinkedHashMap<String, Triple<String, String, Integer>>();
					for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.s_playerSequence.entrySet())
					{
						final Triple<String, String, Integer> oldTriple = playerSequence.getValue(); 
						if (m_currentRowName.equals(oldTriple.getSecond())) {
							updatesPlayerSequence.put(playerSequence.getKey(), new Triple<String, String, Integer>(oldTriple.getFirst(), inputText, oldTriple.getThird()));
						}
					}
					for (final Entry<String, Triple<String, String, Integer>> playerSequence : updatesPlayerSequence.entrySet())
					{
						MapXMLHelper.s_playerSequence.put(playerSequence.getKey(), playerSequence.getValue());
					}
				}
				if (!MapXMLHelper.s_productionFrontiers.isEmpty())
				{
					final ArrayList<String> productionFrontier = MapXMLHelper.s_productionFrontiers.get(m_currentRowName);
					if (productionFrontier!=null)
					{
						MapXMLHelper.s_productionFrontiers.remove(m_currentRowName);
						MapXMLHelper.s_productionFrontiers.put(inputText, productionFrontier);
					}
				}

				if (!MapXMLHelper.s_technologyDefinitions.isEmpty())
				{
					// Delete Technology Definitions for this Player Name (techKey ending with '_' + PlayerName)
					final LinkedHashMap<String, ArrayList<String>> newEntryMap = new LinkedHashMap<String, ArrayList<String>>();
					final String compareValue = "_" + m_currentRowName;
					for (final Entry<String, ArrayList<String>> technologyDefinition : MapXMLHelper.s_technologyDefinitions.entrySet())
					{
						final String techKey = technologyDefinition.getKey();
						if (techKey.endsWith(compareValue)) {
							final ArrayList<String> techValues = technologyDefinition.getValue();
							techValues.set(0, inputText);
							newEntryMap.put(techKey.substring(0,techKey.lastIndexOf(compareValue)) + "_" + inputText, techValues);
						}
						else
							newEntryMap.put(techKey, technologyDefinition.getValue());
					}
					MapXMLHelper.s_technologyDefinitions = newEntryMap;
				}
				m_currentRowName = inputText;
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tPlayerName.selectAll();
			}
		});

		dimension = m_tPlayerAlliance.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_MEDIUM;
		m_tPlayerAlliance.setPreferredSize(dimension);
		m_tPlayerAlliance.setSelectedIndex(Arrays.binarySearch(alliances, allianceName));
		m_tPlayerAlliance.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent arg0)
			{
				// everything is okay with the new technology name, lets rename everything
				MapXMLHelper.s_playerAlliance.put(playerName, (String)m_tPlayerAlliance.getSelectedItem());
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
			}
		});
	
		dimension = m_tInitialResource.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tInitialResource.setPreferredSize(dimension);
		m_tInitialResource.addFocusListener(new FocusListener()
		{
			String prevValue = Integer.toString(initialResource);
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tInitialResource.getText().trim();
				try
				{
					final Integer newValue = Integer.parseInt(inputText);
					MapXMLHelper.s_playerInitResources.put(playerName, newValue);
				} catch (NumberFormatException e)
				{
					m_tInitialResource.setText(prevValue);
					JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
					parentRowPanel.setDataIsConsistent(false);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tInitialResource.updateUI();
							m_tInitialResource.requestFocus();
							m_tInitialResource.selectAll();
						}
					});
					return;
				}
				prevValue = inputText;
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tInitialResource.selectAll();
			}
		});

	}
	
	public boolean isAllianceSelected(final String removeAllianceName)
	{
		return m_tPlayerAlliance.getSelectedItem().equals(removeAllianceName);
	}

	public void removeFromComboBoxesAlliance(String removeAlliance)
	{
		m_tPlayerAlliance.removeItem(removeAlliance);
	}

	public void updateComboBoxesAlliance(final String newAlliance)
	{
		m_tPlayerAlliance.addItem(newAlliance);
	}

	@Override
	protected ArrayList<JComponent> getComponentList()
	{
		final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
		componentList.add(m_tPlayerName);
		componentList.add(m_tPlayerAlliance);
		componentList.add(m_tInitialResource);
		return componentList;
	}
	
	@Override
	public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template)
	{
		parent.add(m_tPlayerName, gbc_template);
		
		final GridBagConstraints gbc_tPlayerAlliance = (GridBagConstraints) gbc_template.clone();
		gbc_tPlayerAlliance.gridx = 1;
		parent.add(m_tPlayerAlliance, gbc_tPlayerAlliance);
		
		final GridBagConstraints gbc_tInitialResource = (GridBagConstraints) gbc_template.clone();
		gbc_tInitialResource.gridx = 2;
		parent.add(m_tInitialResource, gbc_tInitialResource);
		
		final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
		gbc_bRemove.gridx = 3;
		parent.add(m_bRemoveRow, gbc_bRemove);
	}
	
	@Override
	protected void adaptRowSpecifics(final DynamicRow newRow) {
		final PlayerAndAlliancesRow newRowPlayerAndAlliancesRow = (PlayerAndAlliancesRow) newRow;
		this.m_tPlayerName.setText(newRowPlayerAndAlliancesRow.m_tPlayerName.getText());
		this.m_tPlayerAlliance.setSelectedIndex(newRowPlayerAndAlliancesRow.m_tPlayerAlliance.getSelectedIndex());
		this.m_tInitialResource.setText(newRowPlayerAndAlliancesRow.m_tInitialResource.getText());
	}
	
	@Override
	protected void removeRowAction() {
		MapXMLHelper.s_playerName.remove(m_currentRowName);
		MapXMLHelper.s_playerAlliance.remove(m_currentRowName);
		MapXMLHelper.s_playerInitResources.remove(m_currentRowName);
		if (!MapXMLHelper.s_playerSequence.isEmpty())
		{
			// Replace Player Sequences using the deleted Player Name
			final ArrayList<String> deleteKeys = new ArrayList<String>();
			for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.s_playerSequence.entrySet())
			{
				final Triple<String, String, Integer> oldTriple = playerSequence.getValue(); 
				if (m_currentRowName.equals(oldTriple.getSecond())) {
					deleteKeys.add(playerSequence.getKey());
				}
			}
			for (final String deleteKey : deleteKeys)
				MapXMLHelper.s_playerSequence.remove(deleteKey);
		}
		if (!MapXMLHelper.s_technologyDefinitions.isEmpty())
		{
			// Replace Technology Definitions using the deleted Player Name
			final ArrayList<String> deleteKeys = new ArrayList<String>();
			final String compareValue = "_" + m_currentRowName;
			for (final Entry<String, ArrayList<String>> technologyDefinition : MapXMLHelper.s_technologyDefinitions.entrySet())
			{
				final String techKey = technologyDefinition.getKey();
				if (techKey.endsWith(compareValue)) {
					deleteKeys.add(techKey);
				}
			}
			for (final String deleteKey : deleteKeys)
				MapXMLHelper.s_technologyDefinitions.remove(deleteKey);
		}
	}
}
