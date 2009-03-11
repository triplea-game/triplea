package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.ui.LobbyFrame;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class MetaSetupPanel extends SetupPanel
{
    private static final Logger s_logger = Logger.getLogger(MetaSetupPanel.class.getName());
    
    private JButton m_startLocal;
    private JButton m_startPBEM;
    private JButton  m_hostGame;
    private JButton m_connectToHostedGame;
    private JButton m_connectToLobby;
    private SetupPanelModel m_model;
    
    public MetaSetupPanel(SetupPanelModel model)
    {
        m_model = model;
        
        createComponents();
        layoutComponents(); 
        setupListeners();
        setWidgetActivation();
    }

    private void createComponents()
    {
        m_startLocal = new JButton("Start Local Game");
        m_startPBEM = new JButton("Start PBEM Game");
        m_hostGame = new JButton("Host Networked Game");
        m_connectToHostedGame = new JButton("Connect to Networked Game");
        m_connectToLobby = new JButton("Find Games On The Lobby Server");
    }

    private void layoutComponents()
    {
        
        setLayout(new GridBagLayout());
        
        //top space
        add(new JPanel(), new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(00,0,0,0), 0,0) );
        
        add(m_startLocal, new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10,0,0,0), 0,0));
        add(m_startPBEM, new GridBagConstraints(0,2,1,1,0,0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10,0,0,0), 0,0));
        add(m_hostGame, new GridBagConstraints(0,3,1,1,0,0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10,0,0,0), 0,0));
        add(m_connectToHostedGame, new GridBagConstraints(0,4,1,1,0,0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10,0,0,0), 0,0));
        add(m_connectToLobby, new GridBagConstraints(0,5,1,1,0,0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10,0,0,0), 0,0));

        //top space
        add(new JPanel(), new GridBagConstraints(0,100,1,1,1,1,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(00,0,0,0), 0,0) );

        
        
    }

    private void setupListeners()
    {
        m_startLocal.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                m_model.showLocal();
            }
        
        });
        
        m_startPBEM.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                m_model.showPBEM();
            }
        
        });
        
        m_hostGame.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                m_model.showServer(MetaSetupPanel.this);
            }
        
        });
        
        m_connectToHostedGame.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                m_model.showClient(MetaSetupPanel.this);
            }
        });
        
        m_connectToLobby.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                connectToLobby();
        
            }
        
        });

    }

    private void connectToLobby()
    {
        LobbyServerProperties props = getLobbyServerProperties();
        if(props == null)
        {
            props = new LobbyServerProperties(null, -1, "Server Lookup failed, try again later", null);
        }
        
        //for development, ignore what we read,
        //connect instead to localhost
        if(System.getProperties().getProperty("triplea.lobby.debug") != null)
        {
            props = new LobbyServerProperties("127.0.0.1", 3304, "", "the server says");
        }
        
        LobbyLogin login = new LobbyLogin(JOptionPane.getFrameForComponent(this), props);
        
        LobbyClient client = login.login();
        if(client == null)
            return;
        
        
        LobbyFrame lobbyFrame = new LobbyFrame(client,props);
        
        
        
        MainFrame.getInstance().setVisible(false);
        MainFrame.getInstance().dispose();

        lobbyFrame.setVisible(true);
        
        
    }

    private LobbyServerProperties getLobbyServerProperties()
    {
        //try to look up an override
        File f = new File(GameRunner.getRootFolder(), "lobby.properties");
        if(f.exists()) {
            Properties props = new Properties();
            try
            {
                FileInputStream fis = new FileInputStream(f);
                props.load(fis);
                return new LobbyServerProperties(props);
            } catch(IOException e) {
                throw new IllegalStateException(e);
            }
        }
        
        final URL serverPropsURL;
        try
        {
            serverPropsURL = getServerLookupURL();
            if (s_logger.isLoggable(Level.FINE))
            {
                s_logger.log(Level.FINE, "lobby url:" + serverPropsURL);
            }
            
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        
        //sourceforge sometimes takes a long while
        //to return results
        //so run a couple requests in parallell, starting
        //with delays to try and get
        //a response quickly
        final AtomicReference<LobbyServerProperties> ref = new AtomicReference<LobbyServerProperties>();

        final CountDownLatch latch = new CountDownLatch(1);
        
        
        Runnable r = new Runnable()
        {
            public void run()
            {
                for(int i =0; i < 5; i++)
                {
                    spawnRequest(serverPropsURL, ref, latch);
                    try
                    {
                        latch.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    if(ref.get() != null)
                        break;
                }
                
                //we have spawned a bunch of requests
                try
                {
                    latch.await(15, TimeUnit.SECONDS);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            private void spawnRequest(final URL serverPropsURL, final AtomicReference<LobbyServerProperties> ref, final CountDownLatch latch)
            {
                Thread t1 = new Thread(new Runnable()
                {
                    public void run()
                    {
                        ref.set(new LobbyServerProperties(serverPropsURL));
                        latch.countDown();
                    }
                
                });
                t1.start();
                
            }
        
        };
        
        BackgroundTaskRunner.runInBackground(this, "Looking Up Server", r);
        
        LobbyServerProperties props = ref.get();
        return props;
    }

    /**
     * Get the url which we use to lookup the lobby server.
     * 
     * we look for a system property triplea.lobby.server.lookup.url, if that is not defined
     * we default to looking on sourceforge, with a version dependent url
     */
    private URL getServerLookupURL() throws MalformedURLException
    {       
        //step 2 check for a system property
        //step 3 default
        final URL serverPropsURL;
        String defaultURL = "http://triplea.sourceforge.net/lobby/server_" + EngineVersion.VERSION.toString() +  ".properties" ;
        serverPropsURL = new URL(System.getProperties().getProperty("triplea.lobby.server.lookup.url", defaultURL));
        return serverPropsURL;
    }

    private void setWidgetActivation()
    {

    }
    
    @Override
    public boolean canGameStart()
    {
        //we cannot start
        return false;
    }

    @Override
    public void cancel()
    {
       //nothing to do
        
    }

}