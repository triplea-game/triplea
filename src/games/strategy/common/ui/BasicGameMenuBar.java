package games.strategy.common.ui;

import games.strategy.common.ui.MacWrapper;
import games.strategy.common.ui.PlayersPanel;
import games.strategy.debug.Console;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.networkMaintenance.BootPlayerAction;
import games.strategy.engine.framework.networkMaintenance.SetPasswordAction;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.net.IServerMessenger;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

public class BasicGameMenuBar extends JMenuBar
{
    protected final MainGameFrame m_frame;
    
    public BasicGameMenuBar(MainGameFrame frame) 
    {
        m_frame = frame;
        createFileMenu(this);
        createGameSpecificMenus(this);
        createNetworkMenu(this);
        createLobbyMenu(this);
        createHelpMenu(this);
    }
    
    
    protected void createGameSpecificMenus (JMenuBar menuBar) 
    {
        
    }
    
    
    protected void createLobbyMenu(JMenuBar menuBar)
    {
        if(!(m_frame.getGame() instanceof ServerGame))
            return;
        ServerGame serverGame = (ServerGame) m_frame.getGame();
        InGameLobbyWatcher watcher = serverGame.getInGameLobbyWatcher();
        if(watcher == null || !watcher.isActive())
        {
            return;
        }
        
        JMenu lobby = new JMenu("Lobby");
        menuBar.add(lobby);
        
        lobby.add(new EditGameCommentAction( watcher, m_frame ));
        lobby.add(new RemoveGameFromLobbyAction(watcher));               
    }
    
    /**
     * @param menuBar
     */
    protected void createNetworkMenu(JMenuBar menuBar)
    {
        //revisit
        //if we are not a client or server game 
        //then this will not create the network menu
        if(getGame().getMessenger() instanceof DummyMessenger)
            return;
        
        JMenu menuNetwork = new JMenu("Network");
        addAllowObserversToJoin(menuNetwork);
        addBootPlayer(menuNetwork);
        addSetGamePassword(menuNetwork);
        addShowPlayers(menuNetwork);
        menuBar.add(menuNetwork);
    }
    
    /**
     * @param parentMenu
     */
    protected void addAllowObserversToJoin(JMenu parentMenu)
    {
        if(!getGame().getMessenger().isServer())
            return;
        
        final IServerMessenger messeneger = (IServerMessenger) getGame().getMessenger();
        
        final JCheckBoxMenuItem allowObservers = new JCheckBoxMenuItem("Allow New Observers");
        allowObservers.setSelected(messeneger.isAcceptNewConnections());
        
        
        allowObservers.addActionListener(new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                messeneger.setAcceptNewConnections(allowObservers.isSelected());
            }
        });

        parentMenu.add(allowObservers);
        return;
    }
    
    /**
     * @param parentMenu
     */
    protected void addBootPlayer(JMenu parentMenu)
    {
        if(!getGame().getMessenger().isServer())
            return;
        
        final IServerMessenger messenger = (IServerMessenger) getGame().getMessenger();
        
        Action boot =  new BootPlayerAction(this, messenger);
        
        parentMenu.add(boot);
        return;
    }
    
    /**
     * @param menuGame
     */
    protected void addSetGamePassword(JMenu parentMenu)
    {
        if(!getGame().getMessenger().isServer())
            return;
        
        final IServerMessenger messenger = (IServerMessenger) getGame().getMessenger();
        
        parentMenu.add(new SetPasswordAction(this, (ClientLoginValidator) messenger.getLoginValidator() ));
    }
    
    /**
     * @param menuGame
     */
    protected void addShowPlayers(JMenu menuGame)
    {
        if (!getGame().getData().getProperties().getEditableProperties().isEmpty())
        {
            AbstractAction optionsAction = new AbstractAction("Show Who is Who...")
            {
                public void actionPerformed(ActionEvent e)
                {
                    PlayersPanel.showPlayers(getGame(),m_frame);
                }
            };
            
            menuGame.add(optionsAction);

        }
    }
    
    
    /**
     * @param menuBar
     */
    protected void createHelpMenu(JMenuBar menuBar)
    {
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        
        addGameSpecificHelpMenus(helpMenu);
        
        addGameNotesMenu(helpMenu);
        addConsoleMenu(helpMenu);
        helpMenu.addSeparator();
        addAboutMenu(helpMenu);
    }
    
    protected void addGameSpecificHelpMenus(JMenu helpMenu) 
    {
        
    }
    
    protected void addConsoleMenu(JMenu parentMenu)
    {
        parentMenu.add(new AbstractAction("Show Console...")
                {
                    public void actionPerformed(ActionEvent e)
                    {
                       Console.getConsole().setVisible(true);   
                    }
                });
    }
    
    /**
     * @param parentMenu
     * @return
     */
    protected void addAboutMenu(JMenu parentMenu)
    {
        
        parentMenu.add(new AbstractAction("About...")
        {
            public void actionPerformed(ActionEvent e)
            {
                String text = "<h2>"+ getData().getGameName()  + "</h2>"+

                "<p><b>Engine Version:</b> " + games.strategy.engine.EngineVersion.VERSION.toString()+
        "<br><b>Game:</b> "+getData().getGameName()+
                "<br><b>Game Version:</b>" + getData().getGameVersion()+"</p>"+
        "<p>For more information please visit,<br><br>"+
                "<b><a hlink='http://triplea.sourceforge.net/'>http://triplea.sourceforge.net/</a></b><br><br>";

                JEditorPane editorPane = new JEditorPane();
                editorPane.setBorder(null);
                editorPane.setBackground(getBackground());
                editorPane.setEditable(false);
                editorPane.setContentType("text/html");
                editorPane.setText(text);

                JScrollPane scroll = new JScrollPane(editorPane);
                scroll.setBorder(null);

                JOptionPane.showMessageDialog(m_frame, editorPane, "About", JOptionPane.PLAIN_MESSAGE);
            }
        });
        
    }
    
    
    /**
     * @param parentMenu
     */
    protected void addGameNotesMenu(JMenu parentMenu)
    {
        //allow the game developer to write notes that appear in the game
        //displays whatever is in the notes field in html
        final String notes = (String) getData().getProperties().get("notes");
        if (notes != null && notes.trim().length() != 0)
        {

            parentMenu.add(new AbstractAction("Game Notes...")
            {
                public void actionPerformed(ActionEvent e)
                {

                    JEditorPane editorPane = new JEditorPane();
                    editorPane.setEditable(false);
                    editorPane.setContentType("text/html");
                    editorPane.setText(notes);

                    JScrollPane scroll = new JScrollPane(editorPane);

                    JOptionPane.showMessageDialog(m_frame, scroll, "Notes", JOptionPane.PLAIN_MESSAGE);
                }
            });

        }
    }
    

    
    /**
     * @param menuBar
     */
    protected void createFileMenu(JMenuBar menuBar)
    {
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        addSaveMenu(fileMenu);
        addExitMenu(fileMenu);
    }
    
    /**
     * @param parent
     */
    protected void addSaveMenu(JMenu parent)
    {
        // menuFileSave = new JMenuItem("Save", KeyEvent.VK_S);
        JMenuItem menuFileSave = new JMenuItem(new AbstractAction("Save...")
        {
            public void actionPerformed(ActionEvent e)
            {               
                JFileChooser fileChooser = SaveGameFileChooser.getInstance();

                int rVal = fileChooser.showSaveDialog(m_frame);
                if (rVal == JFileChooser.APPROVE_OPTION)
                {
                    File f = fileChooser.getSelectedFile();

                    //A small warning so users will not over-write a file,
                    // added by NeKromancer
                    if (f.exists())
                    {
                        int choice = JOptionPane.showConfirmDialog(m_frame,
                                "A file by that name already exists. Do you wish to over write it?", "Over-write?", JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (choice != JOptionPane.OK_OPTION)
                        {
                            return;
                        }
                    }//end if exists

                    if (!f.getName().toLowerCase().endsWith(".tsvg"))
                    {
                        f = new File(f.getParent(), f.getName() + ".tsvg");
                    }

                    
                    getGame().saveGame(f);                            
                    JOptionPane.showMessageDialog(m_frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
                }

            }
        });
        menuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        parent.add(menuFileSave);
    }

    
    /**
     * @param parentMenu
     */
    protected void addExitMenu(JMenu parentMenu)
    {   
        boolean isMac = GameRunner.isMac();
    
        JMenuItem leaveGameMenuExit = new JMenuItem(new AbstractAction("Leave Game")
        {
            public void actionPerformed(ActionEvent e)
            {
                m_frame.leaveGame();
            }
        });
        if (isMac)
        {   // On Mac OS X, the command-Q is reserved for the Quit action,
                //    so set the command-L key combo for the Leave Game action
                leaveGameMenuExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        else
        {   // On non-Mac operating systems, set the Ctrl-Q key combo for the Leave Game action
                leaveGameMenuExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        parentMenu.add(leaveGameMenuExit);
        
        // Mac OS X automatically creates a Quit menu item under the TripleA menu, 
        //     so all we need to do is register that menu item with triplea's shutdown mechanism
        if (isMac)
        {
                MacWrapper.registerMacShutdownHandler(m_frame);
        }
        else
        {   // On non-Mac operating systems, we need to manually create an Exit menu item
                JMenuItem menuFileExit = new JMenuItem(new AbstractAction("Exit")
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        m_frame.shutdown();
                    }
                });  
                parentMenu.add(menuFileExit);
        }
    }

    public IGame getGame()
    {
        return m_frame.getGame();
    }
    
    public GameData getData()
    {
        return m_frame.getGame().getData();
    }
}
