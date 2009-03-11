package games.strategy.engine.lobby.client.ui.action;

import games.strategy.engine.framework.startup.ui.*;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.*;

public class EditGameCommentAction extends AbstractAction
{

    private final InGameLobbyWatcher m_lobbyWatcher;
    private Component m_parent;
    
    public EditGameCommentAction(InGameLobbyWatcher watcher, Component parent)
    {
        super("Set Lobby Comment...");
        m_parent = parent;
        m_lobbyWatcher = watcher;
    }

    public void actionPerformed(ActionEvent e)
    {
        if(!m_lobbyWatcher.isActive())
        {
            setEnabled(false);
            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(m_parent), "Not connected to Lobby" );
            return;
        }
        
        String current = m_lobbyWatcher.getComments();
        String rVal = JOptionPane.showInputDialog( JOptionPane.getFrameForComponent(m_parent), "Edit the comments for the game", current );
        if(rVal != null)
        {
            m_lobbyWatcher.setGameComments(rVal);
        }
        
    }

    
}
