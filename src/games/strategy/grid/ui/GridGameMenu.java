package games.strategy.grid.ui;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.sound.SoundOptions;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.DefaultMapSelectionListener;
import games.strategy.triplea.ui.MapSelectionListener;
import games.strategy.triplea.ui.MouseDetails;
import games.strategy.triplea.ui.PlayerChooser;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * 
 * @author veqryn
 * 
 * @param <CustomGridGameFrame>
 */
public abstract class GridGameMenu<CustomGridGameFrame extends GridGameFrame> extends BasicGameMenuBar<GridGameFrame>
{
	private static final long serialVersionUID = -8512774312859744827L;
	protected CurrentEditAction m_currentEditAction = CurrentEditAction.None;
	protected JMenu m_editOptionsMenu;
	
	
	protected enum CurrentEditAction
	{
		None, AddUnits, RemoveUnits, ChangeTerritoryOwnership
	}
	
	public GridGameMenu(final CustomGridGameFrame frame)
	{
		super(frame);
	}
	
	@Override
	protected void createGameSpecificMenus(final JMenuBar menuBar)
	{
		createViewMenu(menuBar);
		createGameMenu(menuBar);
		createExportMenu(menuBar);
	}
	
	@Override
	protected void addGameSpecificHelpMenus(final JMenu helpMenu)
	{
		addHowToPlayHelpMenu(helpMenu);
	}
	
	protected void createGameMenu(final JMenuBar menuBar)
	{
		final JMenu menuGame = new JMenu("Game");
		menuGame.setMnemonic(KeyEvent.VK_G);
		menuBar.add(menuGame);
		addEditMode(menuGame);
		menuGame.add(m_frame.getShowGameAction()).setMnemonic(KeyEvent.VK_G);
		menuGame.add(m_frame.getShowHistoryAction()).setMnemonic(KeyEvent.VK_H);
		addSoundsToMenu(menuGame);
		menuGame.addSeparator();
		addGameOptionsMenu(menuGame);
		addAISleepDuration(menuGame);
	}
	
	protected void addEditMode(final JMenu parentMenu)
	{
		// checkbox for edit mode
		final JCheckBoxMenuItem editMode = new JCheckBoxMenuItem("Enable Edit Mode");
		editMode.setModel(m_frame.getEditModeButtonModel());
		parentMenu.add(editMode).setMnemonic(KeyEvent.VK_E);
		
		// and edit mode actions:
		m_frame.getMapPanel().addMapSelectionListener(getMapSelectionListener());
		m_editOptionsMenu = new JMenu();
		m_editOptionsMenu.setText("Edit...");
		addEditAddUnitsMode(m_editOptionsMenu);
		addEditRemoveUnitsMode(m_editOptionsMenu);
		// addEditChangeTerritoryOwnershipMode(editOptionsMenu);
		parentMenu.add(m_editOptionsMenu);
	}
	
	public void enableEditOptionsMenu()
	{
		m_editOptionsMenu.setEnabled(true);
		for (final Component item : m_editOptionsMenu.getComponents())
		{
			item.setEnabled(true);
		}
	}
	
	public void disableEditOptionsMenu()
	{
		m_editOptionsMenu.setEnabled(false);
		for (final Component item : m_editOptionsMenu.getComponents())
		{
			item.setEnabled(false);
		}
	}
	
	protected void addEditAddUnitsMode(final JMenu parentMenu)
	{
		final JMenuItem addUnitsItem = new JMenuItem("Add Units...");
		addUnitsItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (m_frame.getEditModeButtonModel().isSelected())
					m_currentEditAction = CurrentEditAction.AddUnits;
			}
		});
		addUnitsItem.setMnemonic(KeyEvent.VK_A);
		parentMenu.add(addUnitsItem);
	}
	
	protected void addEditRemoveUnitsMode(final JMenu parentMenu)
	{
		final JMenuItem removeUnitsItem = new JMenuItem("Remove Units...");
		removeUnitsItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (m_frame.getEditModeButtonModel().isSelected())
					m_currentEditAction = CurrentEditAction.RemoveUnits;
			}
		});
		removeUnitsItem.setMnemonic(KeyEvent.VK_R);
		parentMenu.add(removeUnitsItem);
	}
	
	protected void addEditChangeTerritoryOwnershipMode(final JMenu parentMenu)
	{
		final JMenuItem changeTerritoryOwnerItem = new JMenuItem("Change Territory Owner...");
		changeTerritoryOwnerItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (m_frame.getEditModeButtonModel().isSelected())
					m_currentEditAction = CurrentEditAction.ChangeTerritoryOwnership;
			}
		});
		changeTerritoryOwnerItem.setMnemonic(KeyEvent.VK_T);
		parentMenu.add(changeTerritoryOwnerItem);
	}
	
	protected final MapSelectionListener getMapSelectionListener()
	{
		return new DefaultMapSelectionListener()
		{
			@Override
			public void territorySelected(final Territory territory, final MouseDetails md)
			{
				if (territory == null)
					return;
				if (m_currentEditAction == CurrentEditAction.AddUnits)
				{
					final PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), territory.getOwner(), null, false);
					final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select owner for new units");
					dialog.setVisible(true);
					
					final PlayerID player = playerChooser.getSelected();
					if (player != null)
					{
						final UnitType ut = m_frame.selectUnit(null, getData().getUnitTypeList().getAllUnitTypes(), territory, player, getData(), "Select Unit to Add");
						final Collection<Unit> units = new ArrayList<Unit>();
						units.addAll(ut.create(1, player));
						final String result = m_frame.getEditDelegate().addUnits(territory, units);
						if (result != null)
							JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit", JOptionPane.ERROR_MESSAGE);
					}
					m_currentEditAction = CurrentEditAction.None;
				}
				else if (m_currentEditAction == CurrentEditAction.RemoveUnits)
				{
					final Collection<Unit> selectedUnits = territory.getUnits().getUnits();
					final String result = m_frame.getEditDelegate().removeUnits(territory, selectedUnits);
					if (result != null)
						JOptionPane.showMessageDialog(getTopLevelAncestor(), result, MyFormatter.pluralize("Could not remove unit", selectedUnits.size()), JOptionPane.ERROR_MESSAGE);
					m_currentEditAction = CurrentEditAction.None;
				}
				else if (m_currentEditAction == CurrentEditAction.ChangeTerritoryOwnership)
				{
					final TerritoryAttachment ta = TerritoryAttachment.get(territory, true);
					if (ta == null)
					{
						JOptionPane.showMessageDialog(getTopLevelAncestor(), "No TerritoryAttachment for " + territory + ".", "Could not perform edit", JOptionPane.ERROR_MESSAGE);
						return;
					}
					final PlayerID defaultPlayer = ta.getOriginalOwner();
					final PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), defaultPlayer, null, true);
					final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select new owner for territory for " + territory.getName());
					dialog.setVisible(true);
					
					final PlayerID player = playerChooser.getSelected();
					if (player != null)
					{
						final String result = m_frame.getEditDelegate().changeTerritoryOwner(territory, player);
						if (result != null)
							JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit", JOptionPane.ERROR_MESSAGE);
					}
					m_currentEditAction = CurrentEditAction.None;
				}
			}
		};
	}
	
	protected void addSoundsToMenu(final JMenu parentMenu)
	{
		SoundOptions.addGlobalSoundSwitchMenu(parentMenu);
		SoundOptions.addToMenu(parentMenu, SoundPath.SoundType.GENERAL);
	}
	
	protected void createExportMenu(final JMenuBar menuBar)
	{
		final JMenu menuGame = new JMenu("Export");
		menuGame.setMnemonic(KeyEvent.VK_E);
		menuBar.add(menuGame);
		addExportXML(menuGame);
		addSaveScreenshot(menuGame);
	}
	
	protected void createViewMenu(final JMenuBar menuBar)
	{
		final JMenu menuView = new JMenu("View");
		menuView.setMnemonic(KeyEvent.VK_V);
		menuBar.add(menuView);
		addChatTimeMenu(menuView);
		addShowGameUuid(menuView);
		addSetLookAndFeel(menuView);
	}
	
	protected void addSaveScreenshot(final JMenu parentMenu)
	{
		parentMenu.add(m_frame.getSaveScreenshotAction()).setMnemonic(KeyEvent.VK_E);
	}
	
	protected abstract void addHowToPlayHelpMenu(final JMenu parentMenu);
}
