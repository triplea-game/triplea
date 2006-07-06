package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.lobby.client.LobbyClient;

import java.awt.event.*;
import java.io.*;

import javax.swing.*;

public class GamePanel extends JPanel
{
    private JButton m_hostGame;
    private LobbyClient m_lobbyClient;
    
    public GamePanel(LobbyClient lobbyClient)
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
    }

    private void layoutComponents()
    {
        add(m_hostGame);
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
        options.setVisible(true);
        if(!options.getOKPressed())
        {
            return;
        }
        
        String classPath = System.getProperty("java.class.path");
        String javaCommand = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"; 
        String javaClass = "games.strategy.engine.framework.GameRunner";
        
        
        
        String tripleaParams =  "-D" + GameRunner2.TRIPLEA_SERVER_PROPERTY + "=true " + 
                                "-D" + GameRunner2.TRIPLEA_PORT_PROPERTY + "=" + options.getPort() + " " + 
                                "-D" + GameRunner2.TRIPLEA_NAME_PROPERTY + "=" + options.getName().replace(' ', '_'); 
        
        try
        {
            String command = javaCommand  + " -Xmx128m -classpath " + classPath + " " + tripleaParams + " " + javaClass;
            System.out.println(command);
            
            @SuppressWarnings("unused")
            Process p =  Runtime.getRuntime().exec(command);
            
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
