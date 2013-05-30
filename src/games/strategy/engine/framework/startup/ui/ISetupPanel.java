package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.framework.startup.launcher.ILauncher;

import java.util.List;
import java.util.Observer;

import javax.swing.Action;

/**
 * Made so that we can have a headless setup. (this is probably a hack, but used because i do not want to rewrite the entire setup model).
 * 
 * @author veqryn
 * 
 */
public interface ISetupPanel extends java.io.Serializable
{
	public void addObserver(final Observer observer);
	
	public void removeObserver(final Observer observer);
	
	abstract void notifyObservers();
	
	/**
	 * Subclasses that have chat override this.
	 */
	public IChatPanel getChatPanel();
	
	/**
	 * Cleanup should occur here that occurs when we cancel
	 */
	public abstract void cancel();
	
	public abstract void shutDown();
	
	/**
	 * Can we start the game?
	 */
	public abstract boolean canGameStart();
	
	public abstract void setWidgetActivation();
	
	public void preStartGame();
	
	public void postStartGame();
	
	public ILauncher getLauncher();
	
	public List<Action> getUserActions();
}
