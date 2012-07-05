/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * PurchasePanel.java
 * 
 * Created on December 4, 2001, 7:00 PM
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class PurchasePanel extends ActionPanel
{
	private static final long serialVersionUID = -6121756876868623355L;
	private final JLabel actionLabel = new JLabel();
	private IntegerMap<ProductionRule> m_purchase;
	private boolean m_bid;
	private final SimpleUnitPanel m_purchasedPreviousRoundsUnits;
	private final JLabel m_purchasedPreviousRoundsLabel;
	private final SimpleUnitPanel m_purhcasedUnits;
	private final JLabel m_purchasedLabel = new JLabel();
	private final JButton m_buyButton;
	// if this is set Purchase will use the tabbedProductionPanel - this is modifyable through the View Menu
	private static boolean m_tabbedProduction = true;
	private final String BUY = "Buy...";
	private final String CHANGE = "Change...";
	
	/** Creates new PurchasePanel */
	public PurchasePanel(final GameData data, final MapPanel map)
	{
		super(data, map);
		m_purchasedPreviousRoundsUnits = new SimpleUnitPanel(map.getUIContext());
		m_purhcasedUnits = new SimpleUnitPanel(map.getUIContext());
		m_buyButton = new JButton(BUY);
		m_buyButton.addActionListener(PURCHASE_ACTION);
		m_purchasedPreviousRoundsLabel = new JLabel("Unplaced from previous rounds");
	}
	
	@Override
	public void display(final PlayerID id)
	{
		super.display(id);
		m_purchase = new IntegerMap<ProductionRule>();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				removeAll();
				actionLabel.setText(id.getName() + " production");
				m_buyButton.setText(BUY);
				add(actionLabel);
				add(m_buyButton);
				add(new JButton(DoneAction));
				m_purchasedLabel.setText("");
				add(Box.createVerticalStrut(9));
				add(m_purchasedLabel);
				add(Box.createVerticalStrut(4));
				m_purhcasedUnits.setUnitsFromProductionRuleMap(new IntegerMap<ProductionRule>(), id, getData());
				add(m_purhcasedUnits);
				getData().acquireReadLock();
				try
				{
					m_purchasedPreviousRoundsUnits.setUnitsFromCategories(UnitSeperator.categorize(id.getUnits().getUnits()), getData());
					add(Box.createVerticalStrut(4));
					if (!id.getUnits().isEmpty())
					{
						add(m_purchasedPreviousRoundsLabel);
					}
					add(m_purchasedPreviousRoundsUnits);
				} finally
				{
					getData().releaseReadLock();
				}
				add(Box.createVerticalGlue());
				SwingUtilities.invokeLater(REFRESH);
			}
		});
	}
	
	private void refreshActionLabelText()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				actionLabel.setText(getCurrentPlayer().getName() + " production " + (m_bid ? " for bid" : ""));
			}
		});
	}
	
	public IntegerMap<ProductionRule> waitForPurchase(final boolean bid)
	{
		m_bid = bid;
		refreshActionLabelText();
		// automatically "click" the buy button for us!
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				PURCHASE_ACTION.actionPerformed(null);
			}
		});
		waitForRelease();
		return m_purchase;
	}
	
	private final AbstractAction PURCHASE_ACTION = new AbstractAction("Buy")
	{
		private static final long serialVersionUID = -2931438906267249990L;
		
		public void actionPerformed(final ActionEvent e)
		{
			final PlayerID player = getCurrentPlayer();
			final GameData data = getData();
			if (isTabbedProduction())
				m_purchase = TabbedProductionPanel.getProduction(player, (JFrame) getTopLevelAncestor(), data, m_bid, m_purchase, getMap().getUIContext());
			else
				m_purchase = ProductionPanel.getProduction(player, (JFrame) getTopLevelAncestor(), data, m_bid, m_purchase, getMap().getUIContext());
			m_purhcasedUnits.setUnitsFromProductionRuleMap(m_purchase, player, data);
			if (m_purchase.totalValues() == 0)
			{
				m_purchasedLabel.setText("");
				m_buyButton.setText(BUY);
			}
			else
			{
				m_buyButton.setText(CHANGE);
				m_purchasedLabel.setText(totalUnitNumberPurchased(m_purchase) + MyFormatter.pluralize(" unit", totalUnitNumberPurchased(m_purchase)) + " to be produced:");
			}
		}
	};
	
	private int totalUnitNumberPurchased(final IntegerMap<ProductionRule> purchase)
	{
		int totalUnits = 0;
		final Collection<ProductionRule> rules = purchase.keySet();
		for (final ProductionRule current : rules)
		{
			totalUnits += purchase.getInt(current) * current.getResults().totalValues();
		}
		return totalUnits;
	}
	
	private final Action DoneAction = new AbstractAction("Done")
	{
		private static final long serialVersionUID = -209781523508962628L;
		
		public void actionPerformed(final ActionEvent event)
		{
			final boolean hasPurchased = m_purchase.totalValues() != 0;
			if (!hasPurchased)
			{
				final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(PurchasePanel.this), "Are you sure you dont want to buy anything?", "End Purchase",
							JOptionPane.YES_NO_OPTION);
				if (rVal != JOptionPane.YES_OPTION)
				{
					return;
				}
			}
			// give a warning if the
			// player tries to produce too much
			if (isWW2V2() || isRestrictedPurchase() || isSBRAffectsUnitProduction())
			{
				int totalProd = 0;
				getData().acquireReadLock();
				try
				{
					for (final Territory t : Match.getMatches(getData().getMap().getTerritories(), Matches.territoryHasOwnedIsFactoryOrCanProduceUnits(getData(), getCurrentPlayer())))
					{
						totalProd += TripleAUnit.getProductionPotentialOfTerritory(t.getUnits().getUnits(), t, getCurrentPlayer(), getData(), true, true);
					}
				} finally
				{
					getData().releaseReadLock();
				}
				// sum production for all units except factories
				int totalProduced = 0;
				for (final ProductionRule rule : m_purchase.keySet())
				{
					if (!Matches.UnitTypeIsConstruction.match((UnitType) rule.getResults().keySet().iterator().next()))
					{
						totalProduced += m_purchase.getInt(rule) * rule.getResults().totalValues();
					}
				}
				final PlayerID player = getCurrentPlayer();
				final Collection<Unit> unitsNeedingFactory = Match.getMatches(player.getUnits().getUnits(), Matches.UnitIsNotConstruction);
				if (!m_bid && totalProduced + unitsNeedingFactory.size() > totalProd && !isUnlimitedProduction(player))
				{
					final String text = "You have purchased " + (totalProduced + unitsNeedingFactory.size()) + " units, and can only place " + totalProd + " of them. Continue with purchase?";
					final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(PurchasePanel.this), text, "End Purchase", JOptionPane.YES_NO_OPTION);
					if (rVal != JOptionPane.YES_OPTION)
					{
						return;
					}
				}
			}
			release();
		}
	};
	
	/*private boolean isIncreasedFactoryProduction(PlayerID player)
	{
		TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.hasIncreasedFactoryProduction();
	}*/
	private boolean isUnlimitedProduction(final PlayerID player)
	{
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (ra == null)
			return false;
		return ra.getUnlimitedProduction();
	}
	
	@Override
	public String toString()
	{
		return "PurchasePanel";
	}
	
	public static void setTabbedProduction(final boolean tabbedProduction)
	{
		m_tabbedProduction = tabbedProduction;
	}
	
	public static boolean isTabbedProduction()
	{
		return m_tabbedProduction;
	}
}
