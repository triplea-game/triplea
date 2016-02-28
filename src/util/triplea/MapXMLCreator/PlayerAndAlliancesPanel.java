package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
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
public class PlayerAndAlliancesPanel extends DynamicRowsPanel {
		
	private TreeSet<String> m_alliances = new TreeSet<String>();	
	
	public PlayerAndAlliancesPanel(final JPanel stepActionPanel)
	{
		super(stepActionPanel);
	}
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		if (s_me == null || !(s_me instanceof PlayerAndAlliancesPanel))
			s_me = new PlayerAndAlliancesPanel(stepActionPanel);
		DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
	}
	
	protected ActionListener getAutoFillAction()
	{
		return null;
	}
	
	protected void layoutComponents()
	{
		
		final JLabel lPlayerName = new JLabel("Player Name");
		Dimension dimension = lPlayerName.getPreferredSize();
		dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
		lPlayerName.setPreferredSize(dimension);
		final JLabel lPlayerAlliance = new JLabel("Player Alliance");
		lPlayerAlliance.setPreferredSize(dimension);
		final JLabel lInitialResource = new JLabel("Initial Resource");
		dimension = (Dimension) dimension.clone();
		dimension.width = 80;
		lInitialResource.setPreferredSize(dimension);
		
		// <1> Set panel layout
		GridBagLayout gbl_stepActionPanel = new GridBagLayout();
		setColumns(gbl_stepActionPanel);
		setRows(gbl_stepActionPanel, MapXMLHelper.s_playerName.size());
		m_ownPanel.setLayout(gbl_stepActionPanel);
		
		// <2> Add Row Labels: Player Name, Alliance Name, Initial Resource
		GridBagConstraints gbc_lPlayerName = new GridBagConstraints();
		gbc_lPlayerName.insets = new Insets(0, 0, 5, 5);
		gbc_lPlayerName.gridy = 0;
		gbc_lPlayerName.gridx = 0;
		gbc_lPlayerName.anchor = GridBagConstraints.WEST;
		m_ownPanel.add(lPlayerName, gbc_lPlayerName);

		GridBagConstraints gbc_lPlayerAlliance = (GridBagConstraints) gbc_lPlayerName.clone();
		gbc_lPlayerAlliance.gridx = 1;
		m_ownPanel.add(lPlayerAlliance, gbc_lPlayerAlliance);
		
		GridBagConstraints gbc_lInitialResource = (GridBagConstraints) gbc_lPlayerName.clone();
		gbc_lInitialResource.gridx = 2;
		m_ownPanel.add(lInitialResource, gbc_lInitialResource);
		
		// <3> Add Main Input Rows
		final String[] alliances = m_alliances.toArray(new String[m_alliances.size()]);
		int yValue = 1;
		for (final String playerName : MapXMLHelper.s_playerName)
		{
			GridBagConstraints gbc_tPlayerName = (GridBagConstraints) gbc_lPlayerName.clone();
			gbc_tPlayerName.gridx = 0;
			gbc_lPlayerName.gridy = yValue;
			final PlayerAndAlliancesRow newRow = new PlayerAndAlliancesRow(this,m_ownPanel, playerName, MapXMLHelper.s_playerAlliance.get(playerName), alliances, MapXMLHelper.s_playerInitResources.get(playerName));
			newRow.addToComponent(m_ownPanel, yValue, gbc_tPlayerName);
			m_rows.add(newRow);
			++yValue;
		}
		
		// <4> Add Final Button Row
		final JButton bAddPlayer = new JButton("Add Player");
		final JButton bAddAlliance = new JButton("Add Alliance");
		final JButton bRemoveAlliance = new JButton("Remove Alliance");
		
		bAddPlayer.setFont(new Font("Tahoma", Font.PLAIN, 11));
		bAddPlayer.addActionListener(new AbstractAction("Add Player")
		{
			private static final long serialVersionUID = 6322566373692205163L;
			
			public void actionPerformed(final ActionEvent e)
			{
				String newPlayerName = JOptionPane.showInputDialog(m_ownPanel, "Enter a new player name:", "Player" + (MapXMLHelper.s_playerName.size() + 1));
				if (newPlayerName == null || newPlayerName.isEmpty())
					return;
				if (MapXMLHelper.s_playerName.contains(newPlayerName))
				{
					JOptionPane.showMessageDialog(m_ownPanel, "Player '" + newPlayerName + "' already exists.", "Input error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				newPlayerName = newPlayerName.trim();
				
				String allianceName;
				if (m_alliances.isEmpty())
				{
					allianceName = JOptionPane.showInputDialog(m_ownPanel, "Which alliance should player '" + newPlayerName + "' join?", "Alliance1");
					if (allianceName == null)
						return;
					allianceName = allianceName.trim();
					m_alliances.add(allianceName);
				}
				else
					allianceName = (String) JOptionPane.showInputDialog(m_ownPanel, "Which alliance should player '" + newPlayerName + "' join?",
 "Choose Player's Alliance",
								JOptionPane.QUESTION_MESSAGE, null,
 m_alliances.toArray(new String[m_alliances.size()]), // Array of choices
					        m_alliances.iterator().next()); // Initial choice

				MapXMLHelper.s_playerName.add(newPlayerName);
				MapXMLHelper.s_playerAlliance.put(newPlayerName, allianceName);
				MapXMLHelper.s_playerInitResources.put(newPlayerName, 0);
				
				// UI Update
				setRows((GridBagLayout) m_ownPanel.getLayout(), MapXMLHelper.s_playerName.size());
				addRowWith(newPlayerName, allianceName, 0);
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
		addButton(bAddPlayer);
		
		bAddAlliance.setFont(new Font("Tahoma", Font.PLAIN, 11));
		bAddAlliance.addActionListener(new AbstractAction("Add Alliance")
		{
			private static final long serialVersionUID = 6322566373692205163L;
			
			public void actionPerformed(final ActionEvent e)
			{
				String newAllianceName = JOptionPane.showInputDialog(m_ownPanel, "Enter a new alliance name:", "Alliance" + (m_alliances.size() + 1));
				if (newAllianceName == null || newAllianceName.isEmpty())
					return;
				if (m_alliances.contains(newAllianceName))
				{
					JOptionPane.showMessageDialog(m_ownPanel, "Alliance '" + newAllianceName + "' already exists.", "Input error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				newAllianceName = newAllianceName.trim();
				
				m_alliances.add(newAllianceName);
				if (m_alliances.size() > 1)
					bRemoveAlliance.setEnabled(true);
				
				// UI Update
				addToComboBoxesAlliance(newAllianceName);
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
		addButton(bAddAlliance);
		
		bRemoveAlliance.setFont(new Font("Tahoma", Font.PLAIN, 11));
		bRemoveAlliance.setEnabled(m_alliances.size() > 1);
		bRemoveAlliance.addActionListener(new AbstractAction("Remove Alliance")
		{
			private static final long serialVersionUID = 6322566373692205163L;
			
			public void actionPerformed(final ActionEvent e)
			{
				String removeAllianceName = (String) JOptionPane.showInputDialog(m_ownPanel, "Which alliance should get removed?", "Remove Alliance", JOptionPane.QUESTION_MESSAGE,
							null, m_alliances.toArray(new String[m_alliances.size()]), // Array of choices
							m_alliances.iterator().next()); // Initial choice
				if (removeAllianceName == null || removeAllianceName.isEmpty())
					return;
				final ArrayList<String> playerStillUsing = new ArrayList<String>();
				for (final DynamicRow row : m_rows)
				{
					if (((PlayerAndAlliancesRow) row).isAllianceSelected(removeAllianceName))
						playerStillUsing.add(row.getRowName());
				}
				if (!playerStillUsing.isEmpty())
				{
					StringBuilder formattedPlayerList = new StringBuilder();
					final boolean plural = playerStillUsing.size() > 1;
					for (final String playerString : playerStillUsing)
						formattedPlayerList.append("\r\n - " + playerString);
					JOptionPane.showMessageDialog(m_ownPanel, "Cannot remove alliance.\r\nThe following player" + (plural ? "s are" : " is") + " still assigned to alliance '"
								+ removeAllianceName + "':"
								+ formattedPlayerList, "Input error",
								JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				m_alliances.remove(removeAllianceName);
				if (m_alliances.size() <= 1)
					bRemoveAlliance.setEnabled(false);

				// UI Update
				removeFromComboBoxesAlliance(removeAllianceName);
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
		addButton(bRemoveAlliance);
		
		GridBagConstraints gbc_bAddPlayer = (GridBagConstraints) gbc_lPlayerName.clone();
		gbc_bAddPlayer.gridx = 0;
		gbc_bAddPlayer.gridy = yValue;
		addFinalButtonRow(gbc_bAddPlayer);
	}

	protected void addToComboBoxesAlliance(final String newAlliance)
	{
		for (final DynamicRow row : m_rows)
		{
			((PlayerAndAlliancesRow) row).updateComboBoxesAlliance(newAlliance);
		}
	}
	
	protected void removeFromComboBoxesAlliance(final String removeAlliance)
	{
		for (final DynamicRow row : m_rows)
		{
			((PlayerAndAlliancesRow) row).removeFromComboBoxesAlliance(removeAlliance);
		}
	}

	private DynamicRow addRowWith(final String newPlayerName, final String allianceName, final int initialResource)
	{
		final PlayerAndAlliancesRow newRow = new PlayerAndAlliancesRow(this,m_ownPanel, newPlayerName, allianceName, m_alliances.toArray(new String[m_alliances.size()]), initialResource);
		addRow(newRow);
		return newRow;
	}
	
	
	protected void initializeSpecifics()
	{
		if (!MapXMLHelper.s_playerAlliance.isEmpty())
		{
			m_alliances.clear();
		}
		m_alliances.addAll(MapXMLHelper.s_playerAlliance.values());
	}

	protected void setColumns(GridBagLayout gbl_panel)
	{
		gbl_panel.columnWidths = new int[] { 50, 60, 50, 30 };
		gbl_panel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
	}
}
