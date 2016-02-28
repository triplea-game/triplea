package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
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
public class TechnologyDefinitionsPanel extends DynamicRowsPanel {

	private TreeSet<String> m_playerNames = new TreeSet<String>();
	
	public TechnologyDefinitionsPanel(final JPanel stepActionPanel)
	{
		super(stepActionPanel);
	}
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		if (s_me == null || !(s_me instanceof TechnologyDefinitionsPanel))
			s_me = new TechnologyDefinitionsPanel(stepActionPanel);
		DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
	}
	
	protected ActionListener getAutoFillAction()
	{
		return null;
	}
	
	protected void layoutComponents()
	{
		
		final JLabel lTechnologyName = new JLabel("Technology Name");
		Dimension dimension = lTechnologyName.getPreferredSize();
		dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
		lTechnologyName.setPreferredSize(dimension);
		final JLabel lPlayerName = new JLabel("Player Name");
		lPlayerName.setPreferredSize(dimension);
		final JLabel lAlreadyEnabled = new JLabel("Already Enabled");
		dimension = (Dimension) dimension.clone();
		dimension.width = 85;
		lAlreadyEnabled.setPreferredSize(dimension);
		
		// <1> Set panel layout
		GridBagLayout gbl_stepActionPanel = new GridBagLayout();
		setColumns(gbl_stepActionPanel);
		setRows(gbl_stepActionPanel, MapXMLHelper.s_technologyDefinitions.size());
		m_ownPanel.setLayout(gbl_stepActionPanel);
		
		// <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
		GridBagConstraints gbc_lTechnologyName = new GridBagConstraints();
		gbc_lTechnologyName.insets = new Insets(0, 0, 5, 5);
		gbc_lTechnologyName.gridy = 0;
		gbc_lTechnologyName.gridx = 0;
		gbc_lTechnologyName.anchor = GridBagConstraints.WEST;
		m_ownPanel.add(lTechnologyName, gbc_lTechnologyName);

		GridBagConstraints gbc_lPlayerName = (GridBagConstraints) gbc_lTechnologyName.clone();
		gbc_lPlayerName.gridx = 1;
		m_ownPanel.add(lPlayerName, gbc_lPlayerName);
		
		GridBagConstraints gbc_lAlreadyEnabled = (GridBagConstraints) gbc_lTechnologyName.clone();
		gbc_lAlreadyEnabled.gridx = 2;
		m_ownPanel.add(lAlreadyEnabled, gbc_lAlreadyEnabled);
		
		// <3> Add Main Input Rows
		int yValue = 1;

		final String[] playerNames = m_playerNames.toArray(new String[m_playerNames.size()]);
		for (final Entry<String, ArrayList<String>> technologyDefinition : MapXMLHelper.s_technologyDefinitions.entrySet())
		{
			GridBagConstraints gbc_tTechnologyName = (GridBagConstraints) gbc_lTechnologyName.clone();
			gbc_tTechnologyName.gridx = 0;
			gbc_lTechnologyName.gridy = yValue;
			final ArrayList<String> definition = technologyDefinition.getValue();
			final String techKey = technologyDefinition.getKey();
			final TechnologyDefinitionsRow newRow = new TechnologyDefinitionsRow(this,m_ownPanel, techKey.substring(0, techKey.lastIndexOf(definition.get(0))-1), definition.get(0), playerNames, definition.get(1));
			newRow.addToComponent(m_ownPanel, yValue, gbc_tTechnologyName);
			m_rows.add(newRow);
			++yValue;
		}
		
		// <4> Add Final Button Row
		final JButton bAddTechnology = new JButton("Add Technology");
		
		bAddTechnology.setFont(new Font("Tahoma", Font.PLAIN, 11));
		bAddTechnology.addActionListener(new AbstractAction("Add Technology")
		{
			private static final long serialVersionUID = 6322566373692205163L;
			
			public void actionPerformed(final ActionEvent e)
			{
				String newTechnologyName = JOptionPane.showInputDialog(m_ownPanel, "Enter a new technology name:", "Technology" + (MapXMLHelper.s_technologyDefinitions.size() + 1));
				if (newTechnologyName == null || newTechnologyName.isEmpty())
					return;
				newTechnologyName = newTechnologyName.trim();
				String suggestedPlayerName = null;
				for (final String playerName : MapXMLHelper.s_playerName)
				{
					if (!MapXMLHelper.s_technologyDefinitions.containsKey(newTechnologyName+"_"+playerName))
						suggestedPlayerName = playerName;
				}
				if (suggestedPlayerName == null)
				{
					JOptionPane.showMessageDialog(m_ownPanel, "Technology '" + newTechnologyName + "' already exists for all players.", "Input error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				final String newRowName = newTechnologyName+"_"+suggestedPlayerName;
				
				final ArrayList<String> newValue = new ArrayList<String>();
				newValue.add(suggestedPlayerName);
				newValue.add("false");
				MapXMLHelper.s_technologyDefinitions.put(newRowName,newValue);
				
				// UI Update
				setRows((GridBagLayout) m_ownPanel.getLayout(), MapXMLHelper.s_technologyDefinitions.size());
				addRowWith(newTechnologyName, suggestedPlayerName, "false");
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
		addButton(bAddTechnology);
		
		GridBagConstraints gbc_bAddUnit = (GridBagConstraints) gbc_lTechnologyName.clone();
		gbc_bAddUnit.gridx = 0;
		gbc_bAddUnit.gridy = yValue;
		addFinalButtonRow(gbc_bAddUnit);
	}

	private DynamicRow addRowWith(final String newTechnologyName, final String playerName, final String alreadyEnabled)
	{
		final TechnologyDefinitionsRow newRow = new TechnologyDefinitionsRow(this,m_ownPanel, newTechnologyName, playerName, m_playerNames.toArray(new String[m_playerNames.size()]), alreadyEnabled);
		addRow(newRow);
		return newRow;
	}
	
	
	protected void initializeSpecifics()
	{
		m_playerNames.clear();
		m_playerNames.addAll(MapXMLHelper.s_playerName);
	}

	protected void setColumns(GridBagLayout gbl_panel)
	{
		gbl_panel.columnWidths = new int[] { 50, 60, 50, 30 };
		gbl_panel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
	}
}
