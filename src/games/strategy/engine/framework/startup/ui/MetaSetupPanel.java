package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.startup.mc.SetupPanelModel;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class MetaSetupPanel extends SetupPanel
{
    private JButton m_startLocal;
    private JButton m_startPBEM;
    private JButton  m_hostGame;
    private JButton m_connectToHostedGame;
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
