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

package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.lobby.client.LobbyClient;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

public class LobbyGamePanel extends JPanel
{
    private JButton m_hostGame;
    private LobbyGameTableModel m_gameTableModel;
    private LobbyClient m_lobbyClient;
    private JTable m_gameTable;
    
    public LobbyGamePanel(LobbyClient lobbyClient)
    {
        m_lobbyClient = lobbyClient;
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
    }

    private void createComponents()
    {
        m_hostGame = new JButton("Host Game");
        m_gameTableModel = new LobbyGameTableModel(m_lobbyClient.getMessenger(), m_lobbyClient.getChannelMessenger(), m_lobbyClient.getRemoteMessenger());
        m_gameTable = new JTable(m_gameTableModel);
        
    }

    private void layoutComponents()
    {
        JScrollPane scroll = new JScrollPane(m_gameTable);
        add(scroll, BorderLayout.CENTER);
        
        add(m_hostGame, BorderLayout.SOUTH);
    }

    private void setupListeners()
    {
        m_hostGame.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                hostGame();
            }
        
        });
    }
    

    protected void hostGame()
    {
        ServerOptions options = new ServerOptions(this, m_lobbyClient.getMessenger().getLocalNode().getName() ,3300);
        options.setLocationRelativeTo(this);
        options.setVisible(true);
        if(!options.getOKPressed())
        {
            return;
        }
         
        List<String> commands = new ArrayList<String>();
        String javaCommand =  System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"  ;
        commands.add(javaCommand);
        
        commands.add("-classpath");
        commands.add(System.getProperty("java.class.path"));
        
        commands.add("-D" + GameRunner2.TRIPLEA_SERVER_PROPERTY + "=true");
        commands.add("-D" + GameRunner2.TRIPLEA_PORT_PROPERTY + "=" + options.getPort());
        commands.add("-D" + GameRunner2.TRIPLEA_NAME_PROPERTY + "=" + options.getName());
        
        
        commands.add("-D" + GameRunner2.LOBBY_HOST + "=" + m_lobbyClient.getMessenger().getServerNode().getAddress().getHostAddress());
        commands.add("-D" + GameRunner2.LOBBY_PORT + "=" + m_lobbyClient.getMessenger().getServerNode().getPort());

        if(options.getPassword() != null &&  options.getPassword().length() > 0)
            commands.add("-D" + GameRunner2.TRIPLEA_SERVER_PASSWORD_PROPERTY + "=" + options.getPassword());

        commands.add("-Xmx128m");
        
        String javaClass = "games.strategy.engine.framework.GameRunner";
        commands.add(javaClass);
                
        
        
        try
        {
           
            
            @SuppressWarnings("unused")
            Process p =  Runtime.getRuntime().exec(commands.toArray(new String[] {}));
            
//            Reader reader = new InputStreamReader(p.getInputStream());
//            int c = reader.read();
//            while(c > 0)
//            {
//                System.out.write(c);
//                c = reader.read();
//            }
        } catch (IOException e)
        {
         
            e.printStackTrace();
        }
        
    }


    private void setWidgetActivation()
    {

    }
    
    

}
