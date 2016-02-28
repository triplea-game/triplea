package util.triplea.MapXMLCreator;

import games.strategy.util.Triple;

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
class PlayerSequenceRow extends DynamicRow
{	
	private JTextField m_tSequenceName;
	private JComboBox<String> m_tGameSequenceName;
	private JComboBox<String> m_tPlayerName;
	private JTextField m_tMaxCount;
	
	public PlayerSequenceRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String sequenceName, final String gameSequenceName, final String[] gameSequenceNames, final String playerName, final String[] playerNames, final int maxCount)
	{
		super(sequenceName, parentRowPanel, stepActionPanel);
		
		m_tSequenceName = new JTextField(sequenceName);	
		m_tGameSequenceName = new JComboBox<String>(gameSequenceNames);
		m_tPlayerName = new JComboBox<String>(playerNames);
		final Integer maxCountInteger = Integer.valueOf(maxCount);
		m_tMaxCount = new JTextField(maxCountInteger == null ? "0" : Integer.toString(maxCountInteger));	
		
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
				if (MapXMLHelper.s_playerSequence.containsKey(inputText))
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
				MapXMLHelper.s_playerSequence.remove(m_currentRowName);
				Triple<String, String, Integer> newTriple = new Triple<String, String, Integer>(MapXMLHelper.s_gamePlaySequence.keySet().iterator().next(), MapXMLHelper.s_playerName.get(0), 0);
				MapXMLHelper.s_playerSequence.put(inputText,newTriple);
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tSequenceName.selectAll();
			}
		});


		dimension = m_tGameSequenceName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_MEDIUM;
		m_tGameSequenceName.setPreferredSize(dimension);
		m_tGameSequenceName.setSelectedIndex(Arrays.binarySearch(gameSequenceNames, gameSequenceName));
		m_tGameSequenceName.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				final Triple<String, String, Integer> oldTriple = MapXMLHelper.s_playerSequence.get(m_currentRowName);
				MapXMLHelper.s_playerSequence.put(m_currentRowName, new Triple<String, String, Integer> ((String )m_tGameSequenceName.getSelectedItem(), oldTriple.getSecond(), oldTriple.getThird()));
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
			}
		});
		

		dimension = m_tPlayerName.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_MEDIUM;
		m_tPlayerName.setPreferredSize(dimension);
		m_tPlayerName.setSelectedIndex(Arrays.binarySearch(playerNames, playerName));
		m_tPlayerName.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				final Triple<String, String, Integer> oldTriple = MapXMLHelper.s_playerSequence.get(m_currentRowName);
				MapXMLHelper.s_playerSequence.put(m_currentRowName, new Triple<String, String, Integer> (oldTriple.getFirst(), (String )m_tPlayerName.getSelectedItem(), oldTriple.getThird()));
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
			}
		});


		dimension = m_tMaxCount.getPreferredSize();
		dimension.width = INPUT_FIELD_SIZE_SMALL;
		m_tMaxCount.setPreferredSize(dimension);
		m_tMaxCount.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				String inputText = m_tMaxCount.getText().trim();
				try
				{
					final Integer newValue = Integer.parseInt(inputText);
					if (newValue < 0)
						throw new NumberFormatException();
					final Triple<String, String, Integer> oldTriple = MapXMLHelper.s_playerSequence.get(m_currentRowName);
					MapXMLHelper.s_playerSequence.put(m_currentRowName, new Triple<String, String, Integer> (oldTriple.getFirst(), oldTriple.getSecond(), newValue));
				} catch (NumberFormatException e)
				{
					m_tMaxCount.setText("0");
					JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
					parentRowPanel.setDataIsConsistent(false);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							m_tMaxCount.updateUI();
							m_tMaxCount.requestFocus();
							m_tMaxCount.selectAll();
						}
					});
					return;
				}
				parentRowPanel.setDataIsConsistent(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
				m_tMaxCount.selectAll();
			}
		});
	}

	@Override
	protected ArrayList<JComponent> getComponentList()
	{
		final ArrayList<JComponent> componentList = new ArrayList<JComponent>();
		componentList.add(m_tSequenceName);
		componentList.add(m_tGameSequenceName);
		componentList.add(m_tPlayerName);
		componentList.add(m_tMaxCount);
		return componentList;
	}
	
	@Override
	public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template)
	{
		parent.add(m_tSequenceName, gbc_template);
		
		final GridBagConstraints gbc_tClassName = (GridBagConstraints) gbc_template.clone();
		gbc_tClassName.gridx = 1;
		parent.add(m_tGameSequenceName, gbc_tClassName);
		
		final GridBagConstraints gbc_tDisplayName = (GridBagConstraints) gbc_template.clone();
		gbc_tDisplayName.gridx = 2;
		parent.add(m_tPlayerName, gbc_tDisplayName);

		final GridBagConstraints gbc_tMaxCount = (GridBagConstraints) gbc_template.clone();
		gbc_tMaxCount.gridx = 3;
		parent.add(m_tMaxCount, gbc_tMaxCount);
		
		final GridBagConstraints gbc_bRemove = (GridBagConstraints) gbc_template.clone();
		gbc_bRemove.gridx = 4;
		parent.add(m_bRemoveRow, gbc_bRemove);
	}
	
	@Override
	protected void adaptRowSpecifics(final DynamicRow newRow) {
		final PlayerSequenceRow newRowPlayerSequenceRow = (PlayerSequenceRow) newRow;
		this.m_tSequenceName.setText(newRowPlayerSequenceRow.m_tSequenceName.getText());
		this.m_tGameSequenceName.setSelectedIndex(newRowPlayerSequenceRow.m_tGameSequenceName.getSelectedIndex());
		this.m_tPlayerName.setSelectedIndex(newRowPlayerSequenceRow.m_tPlayerName.getSelectedIndex());
		this.m_tMaxCount.setText(newRowPlayerSequenceRow.m_tMaxCount.getText());
	}
	
	@Override
	protected void removeRowAction() {
		MapXMLHelper.s_playerSequence.remove(m_currentRowName);
	}
}
