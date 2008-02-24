package games.strategy.common.ui;

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

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class BasicGameMenuBar<CustomGameFrame extends MainGameFrame> extends JMenuBar
{
    protected final CustomGameFrame m_frame;
    
    public BasicGameMenuBar(CustomGameFrame frame) 
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
		String text = "<h2>"+ getData().getGameName()  + "</h2>"+

		"<p><b>Engine Version:</b> " + games.strategy.engine.EngineVersion.VERSION.toString()+
		"<br><b>Game:</b> "+getData().getGameName()+
		"<br><b>Game Version:</b>" + getData().getGameVersion()+"</p>"+
		"<p>For more information please visit,<br><br>"+
		"<b><a hlink='http://triplea.sourceforge.net/'>http://triplea.sourceforge.net/</a></b><br><br>";

		final JEditorPane editorPane = new JEditorPane();
		editorPane.setBorder(null);
		editorPane.setBackground(getBackground());
		editorPane.setEditable(false);
		editorPane.setContentType("text/html");
		editorPane.setText(text);

		JScrollPane scroll = new JScrollPane(editorPane);
		scroll.setBorder(null);
		
    	if (System.getProperty("mrj.version") == null) { 
    		parentMenu.addSeparator();
    		
    		parentMenu.add(new AbstractAction("About...")
    		{
    			public void actionPerformed(ActionEvent e)
    			{
    				JOptionPane.showMessageDialog(m_frame, editorPane, "About " + m_frame.getGame().getData().getGameName(), JOptionPane.PLAIN_MESSAGE);
    			}
    		});

    	}
    	else // On Mac OS X, put the About menu where Mac users expect it to be
    	{	
    		Application.getApplication().addApplicationListener(new ApplicationAdapter()
    		{
    			public void handleAbout(ApplicationEvent event)
    			{
    				event.setHandled(true); // otherwise the default About menu will still show appear

    				JOptionPane.showMessageDialog(m_frame, editorPane, "About " + m_frame.getGame().getData().getGameName(), JOptionPane.PLAIN_MESSAGE);
    			}
    		});
    	}
        
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

                    SwingUtilities.invokeLater(new Runnable()
                    {
                    
                        public void run()
                        {
                            JEditorPane editorPane = new JEditorPane();
                            editorPane.setEditable(false);
                            editorPane.setContentType("text/html");
                            editorPane.setText(notes);

                            final JScrollPane scroll = new JScrollPane(editorPane);
                            final JDialog dialog = new JDialog(m_frame);
                            dialog.setModal(true);
                            dialog.add(scroll, BorderLayout.CENTER);
                            JPanel buttons = new JPanel();
                            
                            final JButton button = new JButton(new AbstractAction("OK")
                            {
                                public void actionPerformed(ActionEvent e)
                                {
                                    dialog.setVisible(false);
                                }
                            
                            });
                            buttons.add(button);
                            dialog.getRootPane().setDefaultButton(button);
                            dialog.add(buttons, BorderLayout.SOUTH);
                            dialog.pack();
                            
                            if(dialog.getWidth() < 300) {
                                dialog.setSize(300, dialog.getHeight());
                            }
                            if(dialog.getHeight() < 300) {
                                dialog.setSize(dialog.getWidth(), 300);
                            }
                            
                            if(dialog.getWidth() > 500) {
                                dialog.setSize(500, dialog.getHeight());
                            }
                            if(dialog.getHeight() > 500) {
                                dialog.setSize(dialog.getWidth(), 500);
                            }
                            
                            dialog.setLocationRelativeTo(m_frame);
                            
                            dialog.addWindowListener(new WindowAdapter()
                            {                            
                                @Override
                                public void windowOpened(WindowEvent e)
                                {
                                    scroll.getVerticalScrollBar().getModel().setValue(0);
                                    scroll.getHorizontalScrollBar().getModel().setValue(0);
                                    button.requestFocus();
                                }
                            });

                            dialog.setVisible(true);
                            dialog.dispose();
                        }
                    
                    });
                    
  

                    //JOptionPane.showMessageDialog(m_frame, scroll, "Notes", JOptionPane.PLAIN_MESSAGE);
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
        JMenuItem menuFileSave = new JMenuItem(new AbstractAction("Save...")
        {
            public void actionPerformed(ActionEvent e)
            { 
                // For some strange reason, 
                //    the only way to get a Mac OS X native-style file dialog
                //    is to use an AWT FileDialog instead of a Swing JDialog
                if(GameRunner.isMac())
                {
                    FileDialog fileDialog = new FileDialog(m_frame);
                    fileDialog.setMode(FileDialog.SAVE);
                    SaveGameFileChooser.ensureDefaultDirExists();
                    fileDialog.setDirectory(SaveGameFileChooser.DEFAULT_DIRECTORY.getPath());
                    fileDialog.setFilenameFilter(new FilenameFilter(){
                       public boolean accept(File dir, String name)
                       {    // the extension should be .tsvg, but find svg extensions as well
                           return name.endsWith(".tsvg") || name.endsWith(".svg");
                       }
                    });
                    
                    fileDialog.setVisible(true);
                    
                    
                    String fileName = fileDialog.getFile();
                    String dirName = fileDialog.getDirectory();
                    
                    if (fileName==null)
                        return;
                    else
                    {
                        if (!fileName.endsWith(".tsvg"))
                            fileName += ".tsvg";
                        File f = new File(dirName, fileName);
                        
                        // If the user selects a filename that already exists,
                        //    the AWT Dialog on Mac OS X will ask the user for confirmation
                        //    so, we don't need to explicitly ask user if they want to overwrite the old file
                        
                        getGame().saveGame(f);                            
                        JOptionPane.showMessageDialog(m_frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
                
                // Non-Mac platforms should use the normal Swing JFileChooser
                else
                {
                    JFileChooser fileChooser = SaveGameFileChooser.getInstance();

                    int rVal = fileChooser.showSaveDialog(m_frame);
                    if (rVal == JFileChooser.APPROVE_OPTION)
                    {
                        File f = fileChooser.getSelectedFile();

                        if (!f.getName().toLowerCase().endsWith(".tsvg"))
                        {
                            f = new File(f.getParent(), f.getName() + ".tsvg");
                        }
                        
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

                        getGame().saveGame(f);                            
                        JOptionPane.showMessageDialog(m_frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
                    }
                    
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
