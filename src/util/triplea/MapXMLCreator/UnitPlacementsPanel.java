package util.triplea.MapXMLCreator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik von der Osten
 * 
 */
public class UnitPlacementsPanel extends ImageScrollPanePanel
{

	private final String[] m_players = MapXMLHelper.getPlayersListInclNeutral();
	
	private UnitPlacementsPanel()
	{
		s_instance = this;
	}
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		s_mapXMLCreator = mapXMLCreator;
		final UnitPlacementsPanel panel = new UnitPlacementsPanel();
		panel.layout(stepActionPanel);
		mapXMLCreator.setAutoFillAction(new AbstractAction()
		{
			private static final long serialVersionUID = -8508734371454749752L;

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				panel.paintPreparation(null);
				panel.repaint();
			}
		});
	}

	protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics, final Point item, final int x_text_start)
	{
		final HashMap<String, LinkedHashMap<String, Integer>> placements = MapXMLHelper.s_unitPlacements.get(centerName);
		String placementString = "";
		for (final Entry<String, LinkedHashMap<String, Integer>> placementEntry : placements.entrySet())
		{
			int totalPlacements = 0;
			for (final Entry<String, Integer> playerPlacement : placementEntry.getValue().entrySet())
				totalPlacements += playerPlacement.getValue();
			if (totalPlacements > 0)
			{
				if (placementString.length() > 0)
					placementString += " / ";
				placementString += (placementEntry.getKey() == null ? "Neutral" : placementEntry.getKey()) + " " + totalPlacements;
				
			}
		}
		if (placementString.length() > 0)
		{
			final Rectangle2D placementStringBounds = fontMetrics.getStringBounds(placementString, g);
			final Rectangle2D centerStringBounds = fontMetrics.getStringBounds(centerName, g);
			double wDiff = (centerStringBounds.getWidth() - placementStringBounds.getWidth())/2;
			g.setColor(Color.yellow);
			g.fillRect(Math.max(0, x_text_start - 2 + (int)wDiff), item.y+6, (int) placementStringBounds.getWidth() + 4, (int) placementStringBounds.getHeight());
			g.setColor(Color.red);
			g.drawString(placementString, Math.max(0, x_text_start + (int) wDiff), item.y + 17);
		}
		g.setColor(Color.red);
	}
	
	protected void paintPreparation(final Map<String, Point> centers)
	{
		for (final String centerName : centers.keySet())
		{
			if (MapXMLHelper.s_unitPlacements.get(centerName) == null)
				MapXMLHelper.s_unitPlacements.put(centerName, new HashMap<String, LinkedHashMap<String, Integer>>());
		}
	}
	
	@Override
	protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers)
	{
	}
	
	protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e)
	{		
		final Point point = e.getPoint();
		final String territoryName = findTerritoryName(point, s_polygons);
		
		if (territoryName == null)
			return;
		
		final HashMap<String, LinkedHashMap<String, Integer>> placements = MapXMLHelper.s_unitPlacements.get(territoryName);
		String suggestedPlayer;
		if (placements.isEmpty())
			suggestedPlayer = MapXMLHelper.s_territoyOwnerships.get(territoryName);
		else
			suggestedPlayer = placements.keySet().iterator().next();
		String inputText = (String) JOptionPane.showInputDialog(null, "For which player you want to place units in territory '" + territoryName + "'?", "Choose Unit Owner for placement in "
					+ territoryName,
					JOptionPane.QUESTION_MESSAGE, null, m_players, // Array of choices
					(suggestedPlayer == null ? m_players[0] : suggestedPlayer)); // Initial choice
		if (inputText == null)
			return;
		LinkedHashMap<String, Integer> playerPlacements = placements.get(inputText);
		if (playerPlacements == null)
			playerPlacements = new LinkedHashMap<String, Integer>();
		// TODO: show unit panel and get new playerPlacements
		
		final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
		final int availHeight = screenResolution.height - 120;
		final int availWidth = screenResolution.width - 40;
		final TerritoryPlacementPanel territoryPlacementPanel = new TerritoryPlacementPanel(playerPlacements, MapXMLHelper.s_productionFrontiers.get(inputText), territoryName, inputText);
		final JScrollPane scroll = new JScrollPane(territoryPlacementPanel);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setPreferredSize(new Dimension((scroll.getPreferredSize().width > availWidth ? availWidth
					: (scroll.getPreferredSize().width + (scroll.getPreferredSize().height > availHeight ? 20 : 0))), (scroll.getPreferredSize().height > availHeight ? availHeight : (scroll
					.getPreferredSize().height + (scroll.getPreferredSize().width > availWidth ? 26 : 0)))));
		final int option = JOptionPane.showOptionDialog(null, scroll, "Enter Unit Placements of player '" + inputText + "' in territory '" + territoryName + "'", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, null, null);
		if (option == JOptionPane.OK_OPTION)
		{
			if (territoryPlacementPanel.placementsExist())
			{
				placements.put(inputText, territoryPlacementPanel.getPlayerPlacements());
			}
			else
			{
				placements.remove(inputText);
			}
		}
		
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				imagePanel.repaint();
			}
		});
	}
	
	
	class TerritoryPlacementPanel extends JPanel
	{
		private static final long serialVersionUID = 6152898248749261730L;
		private LinkedHashMap<String, Integer> m_playerPlacements = null;
		
		public boolean placementsExist()
		{
			for (final Integer value : m_playerPlacements.values())
			{
				if (value > 0)
					return true;
			}
			return false;
		}

		@SuppressWarnings("unchecked")
		public TerritoryPlacementPanel(final LinkedHashMap<String, Integer> playerPlacements, final ArrayList<String> playerUnitTypes, final String territory, final String player)
		{
			super();
			final JPanel me = this;
			if (playerPlacements == null)
				throw new NullPointerException();
		final JTextField[] countFields = new JTextField[playerUnitTypes.size()];
		// 	@SuppressWarnings("unchecked") Reason
			m_playerPlacements = (LinkedHashMap<String, Integer>) playerPlacements.clone();
			this.setLayout(new GridBagLayout());
		final JTextArea title = new JTextArea("Choose units");
			title.setBackground(this.getBackground());
		title.setEditable(false);
		// m_title.setColumns(15);
		title.setWrapStyleWord(true);
		final Insets nullInsets = new Insets(0, 0, 0, 0);
			this.add(title, new GridBagConstraints(0, 0, 7, 1, 0, 0.5, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
			// Buttons
			final Dimension buttonDim = new Dimension(75, 20);
		final JButton bPlaceNone = new JButton("Place None");
			bPlaceNone.setPreferredSize(buttonDim);
		bPlaceNone.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				for (final JTextField countField : countFields)
				{
					if (!countField.getText().equals("0"))
					{
						countField.setText("0");
						countField.requestFocus();
					}
				}
					me.requestFocus();
					me.updateUI();
			}
		});
			
			final LinkedHashMap<String, Integer> allPlayerPlacements = new LinkedHashMap<String, Integer>(m_playerPlacements);
		final ArrayList<String> emtpyPlayerUnitTypes = new ArrayList<String>(playerUnitTypes);
			emtpyPlayerUnitTypes.removeAll(m_playerPlacements.keySet());
			for (final String unitType : emtpyPlayerUnitTypes)
				allPlayerPlacements.put(unitType, 0);
		
			final JButton bReset = new JButton("Reset");
			bReset.setPreferredSize(buttonDim);
			bReset.addActionListener(new ActionListener()
			{
				public void actionPerformed(final ActionEvent e)
			{
					int fieldIndex = 0;
					for (final Entry<String, Integer> placement : allPlayerPlacements.entrySet())
				{
						countFields[fieldIndex].setText(placement.getValue().toString());
						countFields[fieldIndex].requestFocus();
						++fieldIndex;
				}
					m_playerPlacements = (LinkedHashMap<String, Integer>) playerPlacements.clone();
					me.requestFocus();
					me.updateUI();
				}
			});

			// Input lines
		int yIndex = 1;
			final Dimension textFieldDim = new Dimension(25, 20);
		for (final Entry<String, Integer> placement : allPlayerPlacements.entrySet())
		{
			final String unitType = placement.getKey();
				this.add(new JLabel(unitType), new GridBagConstraints(1, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
			final JTextField tCount = new JTextField(placement.getValue().toString());
				tCount.setPreferredSize(textFieldDim);
			countFields[yIndex - 1] = tCount;
			tCount.addFocusListener(new FocusListener()
			{
				final String m_unitType = unitType;
				String m_prevValue = tCount.getText();
				
				@Override
				public void focusLost(FocusEvent arg0)
				{
					final String newValue = tCount.getText().trim();
					if (newValue.equals(m_prevValue))
						return;
					final Integer newValueInteger;
					try
					{
						newValueInteger = Integer.valueOf(newValue);
						if (newValueInteger < 0)
							throw new NumberFormatException();
					} catch (NumberFormatException nfe)
					{
							JOptionPane.showMessageDialog(me, "'" + newValue + "' is no valid integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
						tCount.setText(m_prevValue);
						SwingUtilities.invokeLater(new Runnable()
						{
							
							@Override
							public void run()
							{
								tCount.requestFocus();
							}
						});
						return;
					}
						// LinkedHashMap<String, Integer> playerPlacements = MapXMLHelper.s_unitPlacements.get(territory).get(player);
						if (m_playerPlacements == null)
					{
							m_playerPlacements = new LinkedHashMap<String, Integer>();
							// MapXMLHelper.s_unitPlacements.get(territory).put(player, playerPlacements);
					}
						m_playerPlacements.put(m_unitType, newValueInteger);
				}
				
				@Override
				public void focusGained(FocusEvent arg0)
				{
					tCount.selectAll();
				}
			});
				this.add(tCount, new GridBagConstraints(2, yIndex, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0));
			yIndex++;
		}
			this.add(bPlaceNone, new GridBagConstraints(0, yIndex, 7, 1, 0, 0.5, GridBagConstraints.EAST, GridBagConstraints.NONE, nullInsets, 0, 0));
			this.add(bReset, new GridBagConstraints(3, yIndex, 7, 1, 0, 0.5, GridBagConstraints.EAST, GridBagConstraints.NONE, nullInsets, 0, 0));
			// return territoryPlacementPanel;
		}
		
		public LinkedHashMap<String, Integer> getPlayerPlacements()
		{
			return m_playerPlacements;
		}

	}


}
