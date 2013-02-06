package games.strategy.grid.ui;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.sound.SoundOptions;
import games.strategy.sound.SoundPath;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

/**
 * 
 * @author veqryn
 * 
 * @param <CustomGridGameFrame>
 */
public abstract class GridGameMenu<CustomGridGameFrame extends GridGameFrame> extends BasicGameMenuBar<GridGameFrame>
{
	private static final long serialVersionUID = -8512774312859744827L;
	
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
		menuGame.add(m_frame.getShowGameAction()).setMnemonic(KeyEvent.VK_G);
		menuGame.add(m_frame.getShowHistoryAction()).setMnemonic(KeyEvent.VK_H);
		addSoundsToMenu(menuGame);
		menuGame.addSeparator();
		addGameOptionsMenu(menuGame);
		addAISleepDuration(menuGame);
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
	
	protected abstract void addHowToPlayHelpMenu(final JMenu parentMenu);
}
