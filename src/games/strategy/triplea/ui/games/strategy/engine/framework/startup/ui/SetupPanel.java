package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.startup.launcher.ILauncher;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.*;

public abstract class SetupPanel extends JPanel
{
    private final List<Observer> m_listeners = new CopyOnWriteArrayList<Observer>();
    
    
    public void addObserver(Observer observer)
    {
        m_listeners.add(observer);
    }
    
    public void removeObserver(Observer observer)
    {
        m_listeners.add(observer);
    }
    
    protected void notifyObservers()
    {
        for(Observer observer : m_listeners)
        {
            observer.update(null, null);
        }
    }
    
    
    /**
     * Subclasses that have chat override this.
     */
    public ChatPanel getChatPanel()
    {
        return null;
    }
    
    /**
     * Cleanup should occur here that occurs when we cancel
     */
    public abstract void cancel();
    
    /**
     * Can we start the game?
     */
    public abstract boolean canGameStart();
    
    public void preStartGame()
    {}    
    
    public void postStartGame()
    {}
    
    public ILauncher getLauncher()
    {
        throw new IllegalStateException("NOt implemented");
    }
    
    public List<Action> getUserActions()
    {
        return null;
    }
    
}
