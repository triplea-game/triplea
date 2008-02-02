package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.io.File;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.startup.mc.*;

import javax.swing.*;


/**
 * arguments
 * 
 * to host a game
 * triplea.server=true
 * triplea.port=3300
 * triplea.name=myName
 * 
 *  to connect to a game
 *  triplea.client=true
 *  triplea.port=300
 *  triplea.host=127.0.0.1
 *  triplea.name=myName
 * 
 * 
 * @author Sean Bridges
 *
 */
public class MainFrame extends JFrame
{
    
    //a hack, till i think of something better
    private static MainFrame s_instance;
    
    public static MainFrame getInstance()
    {
        return s_instance;
    }
    
    private GameSelectorModel m_gameSelectorModel;
    private SetupPanelModel m_setupPanelModel;
    
    public MainFrame()
    {
        super("TripleA");
        
        if(s_instance != null)
            throw new IllegalStateException("Instance already exists");
        
        s_instance = this;
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(GameRunner.getGameIcon(this));
        
        
        m_gameSelectorModel = new GameSelectorModel();
        
        m_gameSelectorModel.loadDefaultGame(this);
        
        m_setupPanelModel  = new SetupPanelModel(m_gameSelectorModel);
        m_setupPanelModel.showSelectType();
        MainPanel mainPanel = new MainPanel(m_setupPanelModel);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        //this is a good idea, but in networked play pressing enter should
        //send a new message
        //getRootPane().setDefaultButton(mainPanel.getDefaultButton());
        pack();

        setLocationRelativeTo(null);
    }
    
    /**
     * todo, replace with something better
     * 
     * Get the chat for the game, or null if there is no chat
     */
    public Chat getChat()
    {
        SetupPanel model = m_setupPanelModel.getPanel();
        
        if(model instanceof ServerSetupPanel)
        {
            return model.getChatPanel().getChat();
        }
        else if(model instanceof ClientSetupPanel)
        {
            return model.getChatPanel().getChat();
        }
        else
        {
            return null;
        }
        
    }
    
    /**
     * After the game has been left, call this.
     *
     */
    public void clientLeftGame()
    {
        m_gameSelectorModel.loadDefaultGame(this);
        m_setupPanelModel.showSelectType();
        setVisible(true);
    }
    
    private void loadGameFile(String fileName)
    {
        File f = new File(fileName);
        m_gameSelectorModel.load(f, this);
    }

    /**
     * For displaying on startup.
     * 
     * Only call once!
     *
     */
    public void start()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                String fileName = System.getProperty(GameRunner2.TRIPLEA_GAME_PROPERTY, "");
                if(fileName.length() > 0)
                    loadGameFile(fileName);
                
                setVisible(true);
                
                if(System.getProperty(GameRunner2.TRIPLEA_SERVER_PROPERTY, "false").equals("true"))
                {
                    m_setupPanelModel.showServer(MainFrame.this);
                }
                else if(System.getProperty(GameRunner2.TRIPLEA_CLIENT_PROPERTY, "false").equals("true"))
                {
                    m_setupPanelModel.showClient(MainFrame.this);
                }
            }
        
        });
            
        
        
        
    }
    
    
    
}
