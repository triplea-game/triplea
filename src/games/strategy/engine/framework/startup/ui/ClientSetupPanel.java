package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.startup.mc.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;



public class ClientSetupPanel extends SetupPanel
{
    private final Insets BUTTON_INSETS = new Insets(0,0,0,0);
    private final ClientModel m_model;
    private JLabel m_nameLabel;
    private List<PlayerRow> m_playerRows = Collections.emptyList();
    private RemoteModelListener m_listener = new RemoteModelListener()
    {
    
        public void playersTakenChanged()
        {
           
            
            
        }
    
        public void playerListChanged()
        {
            SwingUtilities.invokeLater(new Runnable()
                    {            
                        public void run()
                        {
                            internalPlayersChanged();
                        }
                    });
            
        }
    
    };
    
    public ClientSetupPanel(ClientModel model)
    {
        m_model = model;
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
    }

    private void internalPlayersChanged()
    {
        Map<String,String> m_players = m_model.getPlayerMapping();
        
        m_playerRows = new ArrayList<PlayerRow>();
        Iterator<String> iter = m_players.keySet().iterator();

        
        while(iter.hasNext())
        {
          String name = (String) iter.next();
          PlayerRow playerRow = new PlayerRow(name, IGameLoader.CLIENT_PLAYER_TYPE);
          m_playerRows.add(playerRow);
          playerRow.setPlayerName(m_players.get(name));
          
        }
        layoutComponents();
    }
    
    private void createComponents()
    {
        m_nameLabel = new JLabel("Name:" + m_model.getMessenger().getLocalNode().getName());
    }

    private void layoutComponents()
    {
        removeAll();
        setLayout(new BorderLayout());

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(m_nameLabel);
        info.add(new JLabel(" "));
        add(info, BorderLayout.NORTH);

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
        playerConstraints.gridx = 2;
        playerConstraints.insets = spacing;

        GridBagConstraints playConstraints = new GridBagConstraints();
        playConstraints.anchor = GridBagConstraints.WEST;
        playConstraints.gridx = 3;
        playConstraints.insets = lastSpacing;

        JLabel nameLabel = new JLabel("Name");
        nameLabel.setForeground(Color.black);
        layout.setConstraints(nameLabel, nameConstraints);
        players.add(nameLabel);

        JLabel playerLabel = new JLabel("Played By");
        playerLabel.setForeground(Color.black);
        layout.setConstraints(playerLabel, playerConstraints);
        players.add(playerLabel);

        JLabel playedByLabel = new JLabel("                    ");
        layout.setConstraints(playedByLabel, playConstraints);
        players.add(playedByLabel);

        Iterator<PlayerRow> iter = m_playerRows.iterator();
        while(iter.hasNext())
        {
          PlayerRow row = iter.next();

          layout.setConstraints(row.getName(), nameConstraints);
          players.add(row.getName());

          layout.setConstraints(row.getPlayer(), playerConstraints);
          players.add(row.getPlayer());

          layout.setConstraints(row.getPlayerComponent(), playConstraints);
          players.add(row.getPlayerComponent());
        }

        add(players, BorderLayout.CENTER);

        validate();
    }

    private void setupListeners()
    {
        m_model.setRemoteModelListener(m_listener);
    }

    private void setWidgetActivation()
    {

    }
    

    
    

    @Override
    public void cancel()
    {
        m_model.cancel();
    }

    @Override
    public boolean canGameStart()
    {
        //our server must handle this
        return false;
    }

    
    
    class PlayerRow
    {
      private JLabel m_playerNameLabel;
      private JLabel m_playerLabel;
      private JComponent m_playerComponent;
      private String m_localPlayerType;

      PlayerRow(String playerName, String localPlayerType)
      {
        m_playerNameLabel = new JLabel(playerName);
        m_playerLabel = new JLabel("");
        m_playerComponent = new JLabel("");
        m_localPlayerType = localPlayerType;
      }

      public JLabel getName()
      {
        return m_playerNameLabel;
      }

      public JLabel getPlayer()
      {
        return m_playerLabel;
      }

      public String getPlayerName()
      {
        return m_playerNameLabel.getText();
      }

      public void setPlayerName(String playerName)
      {
        if(playerName == null)
        {
          m_playerLabel.setText("-");
          JButton button = new JButton(m_takeAction);
          button.setMargin(BUTTON_INSETS);
          m_playerComponent = button;

        }
        else
        {
          m_playerLabel.setText(playerName);

          if(playerName.equals(m_model.getMessenger().getLocalNode().getName()))
          {
            JButton button = new JButton(m_dontTakeAction);
            button.setMargin(BUTTON_INSETS);
            m_playerComponent = button;
          }
          else
          {
            m_playerComponent = new JLabel("");
          }
        }
      }

      public boolean isPlaying()
      {
        return m_playerLabel.getText().equals(m_model.getMessenger().getLocalNode().getName());
      }

      public JComponent getPlayerComponent()
      {
        return m_playerComponent;
      }

      public String getLocalType()
      {
        return m_localPlayerType;
      }

      private Action m_takeAction = new AbstractAction("Play")
      {
        public void actionPerformed(ActionEvent e)
        {
            m_model.takePlayer(m_playerNameLabel.getText());
        }
      };

      private Action m_dontTakeAction = new AbstractAction("Dont Play")
      {
        public void actionPerformed(ActionEvent e)
        {
            m_model.releasePlayer(m_playerNameLabel.getText());
        }
      };
    }
    
    @Override
    public ChatPanel getChatPanel()
    {
        return m_model.getChatPanel();
    }

}
