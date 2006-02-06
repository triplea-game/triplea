package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.framework.startup.ui.*;

import java.awt.Component;
import java.util.Observable;

public class SetupPanelModel extends Observable
{
    
    
    private final GameSelectorModel m_gameSelectorModel;
    private SetupPanel m_panel;
    
    
    
    
    public SetupPanelModel(GameSelectorModel gameSelectorModel)
    {
        m_gameSelectorModel = gameSelectorModel;
    }
    
    public GameSelectorModel getGameSelectorModel()
    {
        return m_gameSelectorModel;
    }

    public void showSelectType()
    {
        setGameTypePanel(new MetaSetupPanel(this));
    }
    
    public void showLocal()
    {
        setGameTypePanel(new LocalSetupPanel(m_gameSelectorModel));
    }
    
    public void showPBEM()
    {
        setGameTypePanel(new PBEMSetupPanel(m_gameSelectorModel));
    }    
    
    public void showServer(Component ui)
    {
        ServerModel model = new ServerModel(m_gameSelectorModel, this);
        if(!model.createServerMessenger(ui))
            return;
        setGameTypePanel(new ServerSetupPanel(model, m_gameSelectorModel));
    }

    public void showClient(Component ui)
    {
        ClientModel model = new ClientModel(m_gameSelectorModel, this);
        if(!model.createClientMessenger(ui))
            return;
        setGameTypePanel(new ClientSetupPanel(model));
    }

    

    private void setGameTypePanel(SetupPanel panel)
    {
        if(m_panel != null)
        {
            m_panel.cancel();
        }
        m_panel = panel;
        super.setChanged();
        super.notifyObservers(m_panel);
        super.clearChanged();
    }
    
    public SetupPanel getPanel()
    {
        return m_panel;
    }

}
