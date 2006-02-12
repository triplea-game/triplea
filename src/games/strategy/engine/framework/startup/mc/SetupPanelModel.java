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
        {
            model.cancel();
            return;
        }
        setGameTypePanel(new ServerSetupPanel(model, m_gameSelectorModel));
    }

    public void showClient(Component ui)
    {
        ClientModel model = new ClientModel(m_gameSelectorModel, this);
        if(!model.createClientMessenger(ui))
        {
            model.cancel();
            return;
        }
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
