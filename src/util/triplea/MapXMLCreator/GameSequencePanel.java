package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

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
public class GameSequencePanel extends DynamicRowsPanel {
	
	public GameSequencePanel(final JPanel stepActionPanel)
	{
		super(stepActionPanel);
	}
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		if (s_me == null || !(s_me instanceof GameSequencePanel))
			s_me = new GameSequencePanel(stepActionPanel);
		DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
	}
	
	protected ActionListener getAutoFillAction()
	{
		return new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(m_ownPanel, "Are you sure you want to use the  Auto-Fill feature?\r\nIt will remove any information you have entered in this step and propose commonly used choices.", "Auto-Fill Overwrite Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
					MapXMLHelper.s_gamePlaySequence.clear();
					MapXMLHelper.s_gamePlaySequence.put("bid", new ArrayList<String>(Arrays.asList("BidPurchaseDelegate", "Bid Purchase")));
					MapXMLHelper.s_gamePlaySequence.put("placeBid", new ArrayList<String>(Arrays.asList("BidPlaceDelegate", "Bid Placement")));
					MapXMLHelper.s_gamePlaySequence.put("tech", new ArrayList<String>(Arrays.asList("TechnologyDelegate", "Research Technology")));
					MapXMLHelper.s_gamePlaySequence.put("tech_Activation", new ArrayList<String>(Arrays.asList("TechActivationDelegate", "Activate Technology")));
					MapXMLHelper.s_gamePlaySequence.put("purchase", new ArrayList<String>(Arrays.asList("PurchaseDelegate", "Purchase Units")));
					MapXMLHelper.s_gamePlaySequence.put("move", new ArrayList<String>(Arrays.asList("MoveDelegate", "Combat Move")));
					MapXMLHelper.s_gamePlaySequence.put("battle", new ArrayList<String>(Arrays.asList("BattleDelegate", "Combat")));
					MapXMLHelper.s_gamePlaySequence.put("place", new ArrayList<String>(Arrays.asList("PlaceDelegate", "Place Units")));
					MapXMLHelper.s_gamePlaySequence.put("endTurn", new ArrayList<String>(Arrays.asList("BidPurchaseDelegate", "Turn Complete")));
					// Update UI
					SwingUtilities.invokeLater(new Runnable()
					{
						
						@Override
						public void run()
						{
							resetRows();
							m_ownPanel.revalidate();
							m_ownPanel.repaint();
							m_ownPanel.requestFocus();
						}
					});
				}
			}
		};
	}
	
	protected void layoutComponents()
	{
		
		final JLabel lSequenceName = new JLabel("Sequence Name");
		Dimension dimension = lSequenceName.getPreferredSize();
		dimension.width = 140;
		lSequenceName.setPreferredSize(dimension);
		final JLabel lClassName = new JLabel("Class Name");
		lClassName.setPreferredSize(dimension);
		final JLabel lDisplayName = new JLabel("Display Name");
		lDisplayName.setPreferredSize(dimension);
		
		// <1> Set panel layout
		GridBagLayout gbl_stepActionPanel = new GridBagLayout();
		setColumns(gbl_stepActionPanel);
		setRows(gbl_stepActionPanel, MapXMLHelper.s_gamePlaySequence.size());
		m_ownPanel.setLayout(gbl_stepActionPanel);
		
		// <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
		GridBagConstraints gbc_lSequenceName = new GridBagConstraints();
		gbc_lSequenceName.insets = new Insets(0, 0, 5, 5);
		gbc_lSequenceName.gridy = 0;
		gbc_lSequenceName.gridx = 0;
		gbc_lSequenceName.anchor = GridBagConstraints.WEST;
		m_ownPanel.add(lSequenceName, gbc_lSequenceName);

		GridBagConstraints gbc_lClassName = (GridBagConstraints) gbc_lSequenceName.clone();
		gbc_lClassName.gridx = 1;
		m_ownPanel.add(lClassName, gbc_lClassName);
		
		GridBagConstraints gbc_lDisplayName = (GridBagConstraints) gbc_lSequenceName.clone();
		gbc_lDisplayName.gridx = 2;
		m_ownPanel.add(lDisplayName, gbc_lDisplayName);
		
		// <3> Add Main Input Rows
		int yValue = 1;
		for (final Entry<String, ArrayList<String>> delegate : MapXMLHelper.s_gamePlaySequence.entrySet())
		{
			GridBagConstraints gbc_tSequenceName = (GridBagConstraints) gbc_lSequenceName.clone();
			gbc_tSequenceName.gridx = 0;
			gbc_lSequenceName.gridy = yValue;
			final ArrayList<String> defintionValues = delegate.getValue();
			final GameSequenceRow newRow = new GameSequenceRow(this,m_ownPanel, delegate.getKey(), defintionValues.get(0), defintionValues.get(1));
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
				String newSequenceName = JOptionPane.showInputDialog(m_ownPanel, "Enter a new sequence name:", "Sequence" + (MapXMLHelper.s_gamePlaySequence.size() + 1));
				if (newSequenceName == null || newSequenceName.isEmpty())
					return;
				if (MapXMLHelper.s_gamePlaySequence.containsKey(newSequenceName))
				{
					JOptionPane.showMessageDialog(m_ownPanel, "Sequence '" + newSequenceName + "' already exists.", "Input error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				newSequenceName = newSequenceName.trim();
				
				final ArrayList<String> newValue = new ArrayList<String>();
				newValue.add("");
				newValue.add("");
				MapXMLHelper.s_gamePlaySequence.put(newSequenceName,newValue);
				
				// UI Update
				setRows((GridBagLayout) m_ownPanel.getLayout(), MapXMLHelper.s_gamePlaySequence.size());
				addRowWith(newSequenceName, "", "");
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

	private DynamicRow addRowWith(final String newSequenceName, final String className, final String displayName)
	{
		final GameSequenceRow newRow = new GameSequenceRow(this,m_ownPanel, newSequenceName, className, displayName);
		addRow(newRow);
		return newRow;
	}
	
	
	protected void initializeSpecifics()
	{
	}

	protected void setColumns(GridBagLayout gbl_panel)
	{
		gbl_panel.columnWidths = new int[] { 50, 60, 50, 30 };
		gbl_panel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
	}
}
