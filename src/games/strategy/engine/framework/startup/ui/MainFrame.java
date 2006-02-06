package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.mc.*;

import javax.swing.JFrame;

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
        
        getContentPane().add(new MainPanel(m_setupPanelModel), BorderLayout.CENTER);
        
        pack();

        games.strategy.ui.Util.center(this);
    }
    
    public void reset()
    {
        m_gameSelectorModel.loadDefaultGame(this);
        m_setupPanelModel.showSelectType();
        setVisible(true);
    }
    
    
    
}
