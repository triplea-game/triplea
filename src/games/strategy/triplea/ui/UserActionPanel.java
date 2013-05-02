package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.sound.DefaultSoundChannel;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attatchments.UserActionAttachment;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Similar to PoliticsPanel, but for UserActionAttachment/Delegate.
 * 
 * @author veqryn
 * 
 */
public class UserActionPanel extends ActionPanel
{
	private static final long serialVersionUID = -2735582890226625860L;
	private final JLabel m_actionLabel = new JLabel();
	private JButton m_selectUserActionButton = null;
	private JButton m_doneButton = null;
	private UserActionAttachment m_choice = null;
	private final TripleAFrame m_parent;
	private boolean m_firstRun = true;
	protected List<UserActionAttachment> m_validUserActions = null;
	
	public UserActionPanel(final GameData data, final MapPanel map, final TripleAFrame parent)
	{
		super(data, map);
		m_parent = parent;
	}
	
	@Override
	public String toString()
	{
		return "Actions and Operations Panel";
	}
	
	@Override
	public void display(final PlayerID id)
	{
		super.display(id);
		m_choice = null;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				removeAll();
				m_actionLabel.setText(id.getName() + " Actions and Operations");
				add(m_actionLabel);
				m_selectUserActionButton = new JButton(SelectUserActionAction);
				m_selectUserActionButton.setEnabled(false);
				add(m_selectUserActionButton);
				m_doneButton = new JButton(DontBotherAction);
				m_doneButton.setEnabled(false);
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						m_doneButton.requestFocusInWindow();
					}
				});
				add(m_doneButton);
			}
		});
	}
	
	/**
	 * waits till someone calls release() and then returns the action chosen
	 * 
	 * @param firstRun
	 * 
	 * @return the choice of action
	 */
	public UserActionAttachment waitForUserActionAction(final boolean firstRun, final IUserActionDelegate iUserActionsDelegate)
	{
		m_firstRun = firstRun;
		// NEVER EVER ACCESS A DELEGATE OR BRIDGE FROM A UI!!!! (or the game won't work in multiplayer) (in other words, do not use the DelegateFinder in any way, or access local delegates, or pass the bridge)
		m_validUserActions = new ArrayList<UserActionAttachment>(iUserActionsDelegate.getValidActions()); // reset each time, we need to retest the conditions...
		Collections.sort(m_validUserActions, new UserActionComparator(getCurrentPlayer(), getData()));
		if (m_validUserActions.isEmpty())// change to (m_firstRun && m_validUserActions.isEmpty()) if you want to have it show the panel again after making an action even if no actions are left.
		{
			return null; // No Valid User actions, do nothing
		}
		else
		{
			if (m_firstRun)
			{
				// play a sound for this phase
				DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_PHASE_USER_ACTIONS, getCurrentPlayer().getName());
			}
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					m_selectUserActionButton.setEnabled(true);
					m_doneButton.setEnabled(true);
					// press the user action button for us.
					SelectUserActionAction.actionPerformed(null);
				}
			});
		}
		waitForRelease();
		return m_choice;
	}
	
	/**
	 * Fires up a JDialog showing valid actions,
	 * choosing an action will release this model and trigger waitForRelease()
	 */
	private final Action SelectUserActionAction = new AbstractAction("Take Action...")
	{
		private static final long serialVersionUID = 2389485901611958851L;
		
		public void actionPerformed(final ActionEvent event)
		{
			final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
			final int availHeight = screenResolution.height - 96;
			final int availWidth = screenResolution.width - 30;
			final int availHeightOverview = (int) ((float) availHeight * 2 / 3);
			final int availHeightChoice = (int) ((float) availHeight / 3);
			final JDialog userChoiceDialog = new JDialog(m_parent, "Actions and Operations", true);
			final Insets insets = new Insets(1, 1, 1, 1);
			int row = 0;
			final JPanel userChoicePanel = new JPanel();
			userChoicePanel.setLayout(new GridBagLayout());
			final JPanel overview = new JPanel();
			overview.setLayout(new GridBagLayout());
			overview.add(new JLabel("You have the following Actions and Operations at your disposal:"),
						new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(25, 10, 25, 30), 0, 0));
			final JScrollPane overviewScroll = new JScrollPane(overview);
			overviewScroll.setBorder(BorderFactory.createEmptyBorder());
			// add 26 to height when the actions are empty, because for some stupid reason java calculates the pack size wrong (again)...
			// add 20 to either when the opposite needs scroll bars, because that is how big scroll bars are..
			overviewScroll.setPreferredSize(new Dimension((overviewScroll.getPreferredSize().width > availWidth ? availWidth : (overviewScroll.getPreferredSize().width +
						(overviewScroll.getPreferredSize().height > availHeightOverview ? 20 : 0))),
						(overviewScroll.getPreferredSize().height > availHeightOverview ? availHeightOverview :
									(overviewScroll.getPreferredSize().height + (m_validUserActions.isEmpty() ? 26 : 0)
												+ (overviewScroll.getPreferredSize().width > availWidth ? 20 : 0)))));
			final JScrollPane choiceScroll = new JScrollPane(getUserActionButtonPanel(userChoiceDialog));
			choiceScroll.setBorder(BorderFactory.createEtchedBorder());
			choiceScroll.setPreferredSize(new Dimension((choiceScroll.getPreferredSize().width > availWidth ? availWidth : (choiceScroll.getPreferredSize().width +
						(choiceScroll.getPreferredSize().height > availHeightChoice ? 20 : 0))),
						(choiceScroll.getPreferredSize().height > availHeightChoice ? availHeightChoice : (choiceScroll.getPreferredSize().height) +
									(choiceScroll.getPreferredSize().width > availWidth ? 20 : 0))));
			
			final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, overviewScroll, choiceScroll);
			splitPane.setOneTouchExpandable(false);
			splitPane.setDividerSize(8);
			userChoicePanel.add(splitPane, new GridBagConstraints(0, row++, 1, 1, 100.0, 100.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
			
			final JButton noActionButton = new JButton(new AbstractAction("No Actions")
			{
				private static final long serialVersionUID = -807175594221278068L;
				
				public void actionPerformed(final ActionEvent arg0)
				{
					userChoiceDialog.setVisible(false);
				}
			});
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					noActionButton.requestFocusInWindow();
				}
			});
			userChoicePanel.add(noActionButton, new GridBagConstraints(0, row, 20, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insets, 0, 0));
			userChoiceDialog.setMinimumSize(new Dimension(600, 300));
			userChoiceDialog.add(userChoicePanel);
			userChoiceDialog.pack();
			userChoiceDialog.setLocationRelativeTo(m_parent);
			userChoiceDialog.setVisible(true);
			userChoiceDialog.dispose();
		}
	};
	
	private JPanel getUserActionButtonPanel(final JDialog parent)
	{
		final JPanel userActionButtonPanel = new JPanel();
		userActionButtonPanel.setLayout(new GridBagLayout());
		int row = 0;
		final Insets insets = new Insets(1, 1, 1, 1);
		for (final UserActionAttachment uaa : m_validUserActions)
		{
			userActionButtonPanel.add(getOtherPlayerFlags(uaa), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
			final JButton button = new JButton(getActionButtonText(uaa));
			button.addActionListener(new ActionListener()
			{
				public void actionPerformed(final ActionEvent ae)
				{
					m_selectUserActionButton.setEnabled(false);
					m_doneButton.setEnabled(false);
					m_validUserActions = null;
					m_choice = uaa;
					parent.setVisible(false);
					release();
				}
			});
			userActionButtonPanel.add(button, new GridBagConstraints(1, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
			userActionButtonPanel.add(getActionDescriptionLabel(uaa), new GridBagConstraints(2, row, 1, 1, 5.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0));
			row++;
		}
		return userActionButtonPanel;
	}
	
	/**
	 * This will stop the user action Phase
	 * 
	 */
	private final Action DontBotherAction = new AbstractAction("Done")
	{
		private static final long serialVersionUID = 2835948679299520899L;
		
		public void actionPerformed(final ActionEvent event)
		{
			if (!m_firstRun || youSureDoNothing())
			{
				m_choice = null;
				release();
			}
		}
		
		private boolean youSureDoNothing()
		{
			final int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(UserActionPanel.this), "Are you sure you dont want to do anything?", "End Actions",
						JOptionPane.YES_NO_OPTION);
			return rVal == JOptionPane.YES_OPTION;
		}
	};
	
	/**
	 * Convenient method to get a JCompenent showing the flags involved in this
	 * action.
	 * 
	 * @param uaa
	 *            the User action attachment to get the "otherflags" for
	 * @return a JComponent with the flags involved.
	 */
	private JPanel getOtherPlayerFlags(final UserActionAttachment uaa)
	{
		final JPanel panel = new JPanel();
		for (final PlayerID p : uaa.getOtherPlayers())
		{
			panel.add(new JLabel(new ImageIcon(this.getMap().getUIContext().getFlagImageFactory().getFlag(p))));
		}
		return panel;
	}
	
	private String getActionButtonText(final UserActionAttachment paa)
	{
		final String costString = paa.getCostPU() == 0 ? "" : "[" + paa.getCostPU() + " PU] ";
		return costString + UserActionText.getInstance().getButtonText(paa.getText());
	}
	
	private JLabel getActionDescriptionLabel(final UserActionAttachment paa)
	{
		final String chanceString = paa.getChanceToHit() == paa.getChanceDiceSides() ? "" : "[" + paa.getChanceToHit() + "/" + paa.getChanceDiceSides() + "] ";
		return new JLabel(chanceString + UserActionText.getInstance().getDescription(paa.getText()));
	}
}


class UserActionComparator implements Comparator<UserActionAttachment>
{
	public UserActionComparator(final PlayerID currentPlayer, final GameData data)
	{
	}
	
	public int compare(final UserActionAttachment uaa1, final UserActionAttachment uaa2)
	{
		if (uaa1.equals(uaa2))
			return 0;
		return uaa1.getName().compareTo(uaa2.getName());
	}
}
