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

import games.strategy.engine.data.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.startup.ui.NewGameFileChooser;

import java.awt.Component;
import java.io.*;
import java.util.Observable;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

public class GameSelectorModel extends Observable
{
    
    public static final File DEFAULT_DIRECTORY = new File(GameRunner.getRootFolder(),  "/games");
    private static final String DEFAULT_FILE_NAME_PREF = "DefaultFileName";
    
    private GameData m_data;
    private String m_gameName;
    private String m_gameVersion;
    private String m_gameRound;
    private String m_fileName;
    private boolean m_canSelect = true;;
    
    
    public GameSelectorModel()
    {
        setGameData(null);
    }
    
    public void load(File file, Component ui)
    {
        GameDataManager manager = new GameDataManager();
        
        
        if(!file.exists())
        {
            error("Could not find file:" + file, ui);
            return;
        }
        if(file.isDirectory())
        {
            error("Cannot load a directory:" + file, ui);
            return;
        }
        
        GameData newData;
        try
        {
            //if the file name is xml, load it as a new game
            if(file.getName().toLowerCase().endsWith("xml"))
            {
                newData= (new GameParser()).parse(new FileInputStream(file));
                
                
                Preferences prefs = Preferences.userNodeForPackage(this.getClass());
                String s=file.getName();
                prefs.put(DEFAULT_FILE_NAME_PREF, s);
            
                
            }
            //the extension should be tsvg, but 
            //try to load it as a saved game whatever the extension
            else
            {
                newData = manager.loadGame(file);
            }
            
            m_fileName = file.getName();
            setGameData(newData);
        } catch (Exception e)
        {
            error(e.getMessage(), ui);
        }
    }
    
    private void error(String message, Component ui)
    {
        JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ui), message, "Could not load Game", JOptionPane.ERROR_MESSAGE );
    }
    
    public GameData getGameData()
    {
        return m_data;
    }
    
    public void setCanSelect(boolean aBool)
    {
        m_canSelect = aBool;
        
        notifyObs();
        
    }
    
    public boolean canSelect()
    {
        return m_canSelect;
    }
    
    
    /**
     * We dont have a gane data (ie we are a remote player and the data has not been sent yet), but
     * we still want to display game info
     */    
    public void clearDataButKeepGameInfo(String gameName, String gameRound, String gameVersion)
    {
        m_data = null;
        m_gameName = gameName;
        m_gameRound = gameRound;
        m_gameVersion = gameVersion;
        
        notifyObs();
    }
    
    public String getFileName()
    {
        if(m_data == null)
            return "-";
        else
            return m_fileName;
    }
    
    public String getGameName()
    {
        return m_gameName;
    }

    public String getGameRound()
    {
        return m_gameRound;
    }

    public String getGameVersion()
    {
        return m_gameVersion;
    }

    public void setGameData(GameData data)
    {
        if(data == null)
        {
            m_gameName = m_gameRound = m_gameVersion = "-";
        }
        else
        {
            m_gameName = data.getGameName();
            m_gameRound = "" + data.getSequence().getRound();
            m_gameVersion = data.getGameVersion().toString();
        }
        
        m_data = data;
        
        notifyObs();
    }

    private void notifyObs()
    {
        super.setChanged();
        super.notifyObservers(m_data);
        super.clearChanged();
    }
    
    public void loadDefaultGame(Component ui)
    {
        //load the previously saved value
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        
        String defaultFileName = "classic_a&a.xml";
        String s= prefs.get(DEFAULT_FILE_NAME_PREF, defaultFileName);
        
        File defaultGame =  new File(NewGameFileChooser.DEFAULT_DIRECTORY, s);
        if(!defaultGame.exists())
            defaultGame = new File(NewGameFileChooser.DEFAULT_DIRECTORY, defaultFileName);
        
        if(!defaultGame.exists())
            return;
        
        load(defaultGame, ui);
    }
    

}
