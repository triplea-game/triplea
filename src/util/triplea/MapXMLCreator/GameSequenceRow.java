package util.triplea.MapXMLCreator;

import games.strategy.util.Triple;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
class GameSequenceRow extends DynamicRow
{	
	private JTextField m_tSequenceName;
	private JTextField m_tClassName;
	private JTextField m_tDisplayName;
	
	public GameSequenceRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String sequenceName, final String className, final String displayName)
	{
		super(sequenceName, parentRowPanel, stepActionPanel);
		
		m_tSequenceName = new JTextField(sequenceName);	
		m_tClassName = new JTextField(className);	
		m_tDisplayName = new JTextField(displayName);
		
		Dimension dimension = m_tSequenceName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_MEDIUM;
		m_tSequenceName.setPreferredSize(dimension);
		m_tSequenceName.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tSequenceName.getText().trim();
				if (m_currentRowName.equals(inputText))
					return;
				if (MapXMLHelper.s_gamePlaySequence.containsKey(inputText))
				{
					m_tSequenceName.selectAll();
					JOptionPane.showMessageDialog(stepActionPanel, "Sequence '" + inputText + "' already exists.", "Input error", JOptionPane.ERROR_MESSAGE);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						
						@Override
						public void run()
						{
							m_tSequenceName.requestFocus();
						}
					});
					return;
				}
				// everything is okay with the new player namer, lets rename everything
				final ArrayList<String> values = MapXMLHelper.s_gamePlaySequence.get(m_currentRowName);
				MapXMLHelper.s_gamePlaySequence.put(inputText, values);
				if (!MapXMLHelper.s_playerSequence.isEmpty())
				{
					// Replace Game Sequence for Player Sequence
					final LinkedHashMap<String, Triple<String, String, Integer>> updatesPlayerSequence = new LinkedHashMap<String, Triple<String, String, Integer>>();
					for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.s_playerSequence.entrySet())
					{
						final Triple<String, String, Integer> oldTriple = playerSequence.getValue(); 
						if (m_currentRowName.equals(oldTriple.getFirst())) {
							updatesPlayerSequence.put(playerSequence.getKey(), new Triple<String, String, Integer>(inputText, oldTriple.getSecond(), oldTriple.getThird()));
						}
					}
					for (final Entry<String, Triple<String, String, Integer>> playerSequence : updatesPlayerSequence.entrySet())
					{
						MapXMLHelper.s_playerSequence.put(playerSequence.getKey(), playerSequence.getValue());
					}
				}
				m_currentRowName = inputText;
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tSequenceName.selectAll();
			}
		});

		dimension = m_tClassName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_LARGE;
		m_tClassName.setPreferredSize(dimension);
		m_tClassName.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tClassName.getText().trim();
				MapXMLHelper.s_gamePlaySequence.get(sequenceName).set(0, inputText);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tClassName.selectAll();
			}
		});
	
		dimension = m_tDisplayName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_LARGE;
		m_tDisplayName.setPreferredSize(dimension);
		m_tDisplayName.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tDisplayName.getText().trim();
				MapXMLHelper.s_gamePlaySequence.get(sequenceName).set(1, inputText);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tDisplayName.selectAll();
			}
		});

	}

	@Override
	protected ArrayList<JComponent> getComponentList()
	{
		final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
		componentList.add(m_tSequenceName);
		componentList.add(m_tClassName);
		componentList.add(m_tDisplayName);
		return componentList;
	}
	
	@Override
	public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template)
	{
		parent.add(m_tSequenceName, gbc_template);
		
		final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
		gbc_tClassName.gridx = 1;
		parent.add(m_tClassName, gbc_tClassName);
		
		final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
		gbc_tDisplayName.gridx = 2;
		parent.add(m_tDisplayName, gbc_tDisplayName);
		
		final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
		gbc_bRemove.gridx = 3;
		parent.add(m_bRemoveRow, gbc_bRemove);
	}
	
	@Override
	protected void adaptRowSpecifics(final DynamicRow newRow) {
		final GameSequenceRow newRowPlayerAndAlliancesRow = (GameSequenceRow) newRow;
		this.m_tSequenceName.setText(newRowPlayerAndAlliancesRow.m_tSequenceName.getText());
		this.m_tClassName.setText(newRowPlayerAndAlliancesRow.m_tClassName.getText());
		this.m_tDisplayName.setText(newRowPlayerAndAlliancesRow.m_tDisplayName.getText());
	}
	
	@Override
	protected void removeRowAction() {
		MapXMLHelper.s_gamePlaySequence.remove(m_currentRowName);

		if (!MapXMLHelper.s_playerSequence.isEmpty())
		{
			// Replace Player Sequences using the deleted Game Sequence
			final ArrayList<String> deleteKeys = new ArrayList<String>();
			for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.s_playerSequence.entrySet())
			{
				final Triple<String, String, Integer> oldTriple = playerSequence.getValue(); 
				if (m_currentRowName.equals(oldTriple.getFirst())) {
					deleteKeys.add(playerSequence.getKey());
				}
			}
			for (final String deleteKey : deleteKeys)
				MapXMLHelper.s_playerSequence.remove(deleteKey);
		}
	}
}
