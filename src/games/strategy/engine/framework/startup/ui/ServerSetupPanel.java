/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.framework.startup.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.networkMaintenance.*;
import games.strategy.engine.framework.startup.launcher.*;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.mc.*;


import games.strategy.net.*;

public class ServerSetupPanel extends SetupPanel implements IRemoteModelListener
{
    private final ServerModel m_model;    
    
    private JTextField m_portField;
    private JTextField m_addressField;
    private JTextField m_nameField;
    
    private List<PlayerRow> m_playerRows = new ArrayList<PlayerRow>();
    private final GameSelectorModel m_gameSelectorModel;
    
    private JPanel m_info;
    private JPanel m_networkPanel;
    private JButton m_bootPlayerButton;
    private JButton m_setPasswordButton;

    private InGameLobbyWatcher m_lobbyWatcher;
    
    
    public ServerSetupPanel(ServerModel model, GameSelectorModel gameSelectorModel)
    {
        m_model = model;
        m_gameSelectorModel = gameSelectorModel;
        m_model.setRemoteModelListener(this);
        
        m_lobbyWatcher = InGameLobbyWatcher.newInGameLobbyWatcher();
        
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
        
        internalPlayerListChanged();
    }

    private void createComponents()
    {
        IServerMessenger messenger = m_model.getMessenger();
        
        Color backGround = new JTextField().getBackground();
        
        m_portField = new JTextField("" + messenger.getLocalNode().getPort());
        m_portField.setEnabled(true);
        m_portField.setEditable(false);
        m_portField.setBackground(backGround);
        
        m_portField.setColumns(6);

        m_addressField = new JTextField(messenger.getLocalNode().getAddress().getHostAddress());
        m_addressField.setEnabled(true);
        m_addressField.setEditable(false);
        m_addressField.setBackground(backGround);

        m_addressField.setColumns(20);

        m_nameField = new JTextField(messenger.getLocalNode().getName());
        m_nameField.setEnabled(true);
        m_nameField.setEditable(false);
        m_nameField.setBackground(backGround);
        

        m_nameField.setColumns(20);
        
        m_info = new JPanel();
        
        m_bootPlayerButton = new JButton("Boot Player...");
        m_setPasswordButton = new JButton("Set Game Password...");
        
        m_networkPanel = new JPanel();
        
    }

    private void layoutComponents()
    {
        setLayout(new BorderLayout());


        m_info.setLayout(new GridBagLayout());

        m_info.add(new JLabel("Name:"), new GridBagConstraints(0,0,1,1,0,0.0,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,10,0,5), 0,0));
        m_info.add(new JLabel("Address:"), new GridBagConstraints(0,1,1,1,0,0.0,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,10,0,5), 0,0));
        m_info.add(new JLabel("Port:"), new GridBagConstraints(0,2,1,1,0,0.0,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,10,0,5), 0,0));

        m_info.add(m_nameField, new GridBagConstraints(1,0,1,1,0.5,1.0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,0,0,5), 0,0));
        m_info.add(m_addressField, new GridBagConstraints(1,1,1,1,0.5,1.0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,0,0,5), 0,0));
        m_info.add(m_portField, new GridBagConstraints(1,2,1,1,0.5,1.0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,0,0,5), 0,0));

        add(m_info, BorderLayout.NORTH);

        m_networkPanel.setLayout(new BoxLayout(m_networkPanel, BoxLayout.X_AXIS));
        m_networkPanel.add(m_bootPlayerButton);
        m_networkPanel.add(m_setPasswordButton);
        add(m_networkPanel, BorderLayout.SOUTH);

        
    }
    
    private void layoutPlayers()
    {
      JPanel players = new JPanel();
      GridBagLayout layout = new GridBagLayout();
      players.setLayout(layout);

      Insets spacing = new Insets(3,23,0,0);
      Insets lastSpacing = new Insets(3,23,0,23);

      GridBagConstraints nameConstraints = new GridBagConstraints();
      nameConstraints.anchor = GridBagConstraints.WEST;
      nameConstraints.gridx = 0;
      nameConstraints.insets = spacing;

      GridBagConstraints playerConstraints = new GridBagConstraints();
      playerConstraints.anchor = GridBagConstraints.WEST;
      playerConstraints.gridx = 1;
      playerConstraints.insets = spacing;

      GridBagConstraints localConstraints = new GridBagConstraints();
      localConstraints.anchor = GridBagConstraints.WEST;
      localConstraints.gridx = 2;
      localConstraints.insets = lastSpacing;

      JLabel nameLabel = new JLabel("Name");
      nameLabel.setForeground(Color.black);
      layout.setConstraints(nameLabel, nameConstraints);
      players.add(nameLabel);

      JLabel playedByLabel = new JLabel("Played by");
      playedByLabel.setForeground(Color.black);
      layout.setConstraints(playedByLabel, playerConstraints);
      players.add(playedByLabel);

      JLabel localLabel = new JLabel("Local");
      localLabel.setForeground(Color.black);
      layout.setConstraints(localLabel, localConstraints);
      players.add(localLabel);

      Iterator<PlayerRow> iter = m_playerRows.iterator();

      if(!iter.hasNext())
      {
        JLabel noPlayers = new JLabel("Load a game file first");
        layout.setConstraints(noPlayers, nameConstraints);
        players.add(noPlayers);
      }

      while(iter.hasNext())
      {
        PlayerRow row = iter.next();

        layout.setConstraints(row.getName(), nameConstraints);
        players.add(row.getName());

        layout.setConstraints(row.getPlayer(), playerConstraints);
        players.add(row.getPlayer());

        layout.setConstraints(row.getLocal(), localConstraints);
        players.add(row.getLocal());
      }

      removeAll();
      add(m_info, BorderLayout.NORTH);
      add(players, BorderLayout.CENTER);
      add(m_networkPanel, BorderLayout.SOUTH);

      
      invalidate();
      validate();

    }
    
    
    

    private void setupListeners()
    {
        m_bootPlayerButton.addActionListener( new BootPlayerAction(this, m_model.getMessenger()));
        m_setPasswordButton.addActionListener(new SetPasswordAction(this, (ClientLoginValidator) m_model.getMessenger().getLoginValidator() ));
    }

    private void setWidgetActivation()
    {

    }
    
    @Override
    public void cancel()
    {
        m_model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
        m_model.cancel();
        
        if(m_lobbyWatcher != null)
        {
            m_lobbyWatcher.shutDown();
        }
    }

    @Override
    public boolean canGameStart()
    {
        if(m_gameSelectorModel.getGameData() == null)
            return false;
        
        Map<String, String> players = m_model.getPlayers();
        for(String player : players.keySet())
        {
            if(players.get(player) == null)
                return false;
        }
        return true;
    }

    public void playerListChanged()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                internalPlayerListChanged();
            }
        
        });
        
    }

    public void playersTakenChanged()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                internalPlayersTakenChanged();
            }
        });
        
    }


    private void internalPlayersTakenChanged()
    {
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");
        
        Map<String,String> players = m_model.getPlayers();
        for(PlayerRow row : m_playerRows)
        {
            row.update(players);
        }
        
        super.notifyObservers();
        
    }
    
    private void internalPlayerListChanged()
    {
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");
        
        m_playerRows = new ArrayList<PlayerRow>();
        Map<String,String> players = m_model.getPlayers();
        List<String> keys = new ArrayList<String>(players.keySet());
        Collections.sort(keys);
        for(String name: keys)
        {
            PlayerRow newPlayerRow = new PlayerRow(name);
            
            m_playerRows.add(newPlayerRow);
            newPlayerRow.update(players);
        }
        
        layoutPlayers();
        internalPlayersTakenChanged();
    }
    
    class PlayerRow
    {
      private JLabel m_nameLabel;
      private JLabel m_playerLabel;
      private JCheckBox m_localCheckBox;

      PlayerRow(String playerName)
      {
        m_nameLabel = new JLabel(playerName);
        m_playerLabel = new JLabel(m_model.getMessenger().getLocalNode().getName());
        m_localCheckBox = new JCheckBox();
        m_localCheckBox.addActionListener(m_actionListener);
        m_localCheckBox.setSelected(true);
      }

      public JLabel getName()
      {
        return m_nameLabel;
      }

      public JLabel getPlayer()
      {
        return m_playerLabel;
      }

      public JCheckBox getLocal()
      {
        return m_localCheckBox;
      }      
      
      public void update(Map<String,String> players)
      {
          String text = players.get(m_nameLabel.getText());
          if(text == null)
              text = "-";
          m_playerLabel.setText(text);
          m_localCheckBox.setSelected(text.equals(m_model.getMessenger().getLocalNode().getName()));
      }
      
      private ActionListener m_actionListener = new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
            if( m_localCheckBox.isSelected())
                m_model.takePlayer( m_nameLabel.getText());
            else
                m_model.releasePlayer( m_nameLabel.getText());
        }
      };
    }
    
    @Override
    public ChatPanel getChatPanel()
    {
        return m_model.getChatPanel();
    }


    @Override
    public ILauncher getLauncher()
    {
        return m_model.getLauncher();
    }
    
}
