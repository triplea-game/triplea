package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.login.CreateUpdateAccountPanel;
import games.strategy.engine.lobby.server.*;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.sound.ClipPlayer;
import games.strategy.util.MD5Crypt;

import java.awt.event.*;

import javax.swing.*;

public class LobbyMenu extends JMenuBar
{
    
    private final LobbyFrame m_frame;

    public LobbyMenu(LobbyFrame frame)
    {
        m_frame = frame;
        
        //file only has one value
        //and on mac it is in the apple menu
        if(!GameRunner.isMac())
            createFileMenu(this);
        else
            MacLobbyWrapper.registerMacShutdownHandler(m_frame);
            
        createAccountMenu(this);
        createSettingsMenu(this);
    }

    private void createAccountMenu(LobbyMenu menuBar)
    {
        JMenu account = new JMenu("Account");
        menuBar.add(account);
        
        addUpdateAccountMenu(account);
        
    }
    
    private void createSettingsMenu(LobbyMenu menuBar)
    {
        JMenu settings = new JMenu("Settings");
        menuBar.add(settings);
        
        addSoundMenu(settings);
        
    }

    
    private void addSoundMenu(JMenu parentMenu)
    {
        final JCheckBoxMenuItem soundCheckBox = new JCheckBoxMenuItem("Enable Sound");

        soundCheckBox.setSelected(!ClipPlayer.getInstance().getBeSilent());
        //temporarily disable sound

        soundCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ClipPlayer.getInstance().setBeSilent(!soundCheckBox.isSelected());
            }
        });
        parentMenu.add(soundCheckBox);

    }

    private void addUpdateAccountMenu(JMenu account)
    {
       JMenuItem update = new JMenuItem("Update Account...");
       //only if we are not anonymous login
       update.setEnabled(!m_frame.getLobbyClient().isAnonymousLogin()); 
       
       update.addActionListener(new ActionListener()
       {
            public void actionPerformed(ActionEvent e)
            {           
                updateAccountDetails();
            }
        
       });
       account.add(update);
        
        
    }

    private void updateAccountDetails()
    {
        IUserManager manager = (IUserManager) m_frame.getLobbyClient().getRemoteMessenger().getRemote(IUserManager.USER_MANAGER);
        DBUser user = manager.getUserInfo( m_frame.getLobbyClient().getMessenger().getLocalNode().getName() );
        if(user == null)
        {
            JOptionPane.showMessageDialog(this, "No user info found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        CreateUpdateAccountPanel panel = CreateUpdateAccountPanel.newUpdatePanel(user);
        CreateUpdateAccountPanel.ReturnValue rVal = panel.show(m_frame);
        if(rVal == CreateUpdateAccountPanel.ReturnValue.CANCEL )
            return;
        
        String error = manager.updateUser(panel.getUserName(), panel.getEmail(),MD5Crypt.crypt(panel.getPassword()));
        if(error != null)
        {
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createFileMenu(JMenuBar menuBar)
    {
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        
        addExitMenu(fileMenu);
    }

    private void addExitMenu(JMenu parentMenu)
    {
        boolean isMac = GameRunner.isMac();
        
        
        // Mac OS X automatically creates a Quit menu item under the TripleA menu, 
        //     so all we need to do is register that menu item with triplea's shutdown mechanism
        if (!isMac)
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
    
    
}
