package games.strategy.engine.lobby.client.ui.action;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class RemoveGameFromLobbyAction extends AbstractAction
{

    private final InGameLobbyWatcher m_lobbyWatcher;
    
    
    public RemoveGameFromLobbyAction(InGameLobbyWatcher watcher)
    {
        super("Remove Game From Lobby");
        m_lobbyWatcher = watcher;
    }

    public void actionPerformed(ActionEvent e)
    {
        m_lobbyWatcher.shutDown();
        setEnabled(false);
    }
    
}
