package util.triplea.MapXMLCreator;

import games.strategy.util.Triple;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik von der Osten
 * 
 */
public class PlayerSequencePanel extends DynamicRowsPanel {

	private TreeSet<String> m_gameSequenceNames = new TreeSet<String>();
	private TreeSet<String> m_playerNames = new TreeSet<String>();
	
	public PlayerSequencePanel(final JPanel stepActionPanel)
	{
		super(stepActionPanel);
	}
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		if (s_me == null || !(s_me instanceof PlayerSequencePanel))
			s_me = new PlayerSequencePanel(stepActionPanel);
		DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
	}
	
	protected ActionListener getAutoFillAction()
	{
		return null;
	}
	
	protected void layoutComponents()
	{
		
		final JLabel lSequenceName = new JLabel("Sequence Name");
		Dimension dimension = lSequenceName.getPreferredSize();
		dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
		lSequenceName.setPreferredSize(dimension);
		final JLabel lGameSequenceName = new JLabel("Game Sequence");
		lGameSequenceName.setPreferredSize(dimension);
		final JLabel lPlayerName = new JLabel("Player Name");
		lPlayerName.setPreferredSize(dimension);
		final JLabel lMaxRunCount = new JLabel("Max Run Count");
		dimension = (Dimension) dimension.clone();
		dimension.width = 90;
		lMaxRunCount.setPreferredSize(dimension);
		
		// <1> Set panel layout
		GridBagLayout gbl_stepActionPanel = new GridBagLayout();
		setColumns(gbl_stepActionPanel);
		setRows(gbl_stepActionPanel, MapXMLHelper.s_playerSequence.size());
		m_ownPanel.setLayout(gbl_stepActionPanel);
		
		// <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
		GridBagConstraints gbc_lSequenceName = new GridBagConstraints();
		gbc_lSequenceName.insets = new Insets(0, 0, 5, 5);
		gbc_lSequenceName.gridy = 0;
		gbc_lSequenceName.gridx = 0;
		gbc_lSequenceName.anchor = GridBagConstraints.WEST;
		m_ownPanel.add(lSequenceName, gbc_lSequenceName);

		GridBagConstraints gbc_lGameSequenceName = (GridBagConstraints) gbc_lSequenceName.clone();
		gbc_lGameSequenceName.gridx = 1;
		m_ownPanel.add(lGameSequenceName, gbc_lGameSequenceName);
		
		GridBagConstraints gbc_lPlayerName = (GridBagConstraints) gbc_lSequenceName.clone();
		gbc_lPlayerName.gridx = 2;
		m_ownPanel.add(lPlayerName, gbc_lPlayerName);
		
		GridBagConstraints gbc_lMaxRunCount = (GridBagConstraints) gbc_lSequenceName.clone();
		gbc_lMaxRunCount.gridx = 3;
		m_ownPanel.add(lMaxRunCount, gbc_lMaxRunCount);
		
		// <3> Add Main Input Rows
		int yValue = 1;

		final String[] gameSequenceNames = m_gameSequenceNames.toArray(new String[m_gameSequenceNames.size()]);
		final String[] playerNames = m_playerNames.toArray(new String[m_playerNames.size()]);
		for (final Entry<String, Triple<String, String, Integer>> playerSequence : MapXMLHelper.s_playerSequence.entrySet())
		{
			GridBagConstraints gbc_tSequenceName = (GridBagConstraints) gbc_lSequenceName.clone();
			gbc_tSequenceName.gridx = 0;
			gbc_lSequenceName.gridy = yValue;
			final Triple<String, String, Integer> defintionValues = playerSequence.getValue();
			final PlayerSequenceRow newRow = new PlayerSequenceRow(this,m_ownPanel, playerSequence.getKey(), defintionValues.getFirst(), gameSequenceNames, defintionValues.getSecond(), playerNames, defintionValues.getThird());
			newRow.addToComponent(m_ownPanel, yValue, gbc_tSequenceName);
			m_rows.add(newRow);
			++yValue;
		}
		
		// <4> Add Final Button Row
		final JButton bAddSequence = new JButton("Add Sequence");
		
		bAddSequence.setFont(new Font("Tahoma", Font.PLAIN, 11));
		bAddSequence.addActionListener(new AbstractAction("Add Sequence")
		{
			private static final long serialVersionUID = 6322566373692205163L;
			
			public void actionPerformed(final ActionEvent e)
			{
				String newSequenceName = JOptionPane.showInputDialog(m_ownPanel, "Enter a new sequence name:", "Sequence" + (MapXMLHelper.s_playerSequence.size() + 1));
				if (newSequenceName == null || newSequenceName.isEmpty())
					return;
				if (MapXMLHelper.s_playerSequence.containsKey(newSequenceName))
				{
					JOptionPane.showMessageDialog(m_ownPanel, "Sequence '" + newSequenceName + "' already exists.", "Input error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				newSequenceName = newSequenceName.trim();
				
				final Triple<String, String, Integer> newValue = new Triple<String, String, Integer>(m_gameSequenceNames.iterator().next(), m_playerNames.iterator().next(), 0);
				MapXMLHelper.s_playerSequence.put(newSequenceName,newValue);
				
				// UI Update
				setRows((GridBagLayout) m_ownPanel.getLayout(), MapXMLHelper.s_playerSequence.size());
				addRowWith(newSequenceName, m_gameSequenceNames.iterator().next(), m_playerNames.iterator().next(), 0);
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						m_ownPanel.revalidate();
						m_ownPanel.repaint();
					}
				});
			}
		});
		addButton(bAddSequence);
		
		GridBagConstraints gbc_bAddUnit = (GridBagConstraints) gbc_lSequenceName.clone();
		gbc_bAddUnit.gridx = 0;
		gbc_bAddUnit.gridy = yValue;
		addFinalButtonRow(gbc_bAddUnit);
	}

	private DynamicRow addRowWith(final String newSequenceName, final String gameSequenceName, final String playerName, final int maxCount)
	{
		final PlayerSequenceRow newRow = new PlayerSequenceRow(this,m_ownPanel, newSequenceName, gameSequenceName, m_gameSequenceNames.toArray(new String[m_gameSequenceNames.size()]), playerName, m_playerNames.toArray(new String[m_playerNames.size()]), maxCount);
		addRow(newRow);
		return newRow;
	}
	
	
	protected void initializeSpecifics()
	{
		m_gameSequenceNames.clear();
		m_playerNames.clear();
		m_gameSequenceNames.addAll(MapXMLHelper.s_gamePlaySequence.keySet());
		m_playerNames.add("");
		m_playerNames.addAll(MapXMLHelper.s_playerName);
	}

	protected void setColumns(GridBagLayout gbl_panel)
	{
		gbl_panel.columnWidths = new int[] { 50, 60, 50, 30, 30 };
		gbl_panel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 };
	}
}
