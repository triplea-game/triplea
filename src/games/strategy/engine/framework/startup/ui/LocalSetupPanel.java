package games.strategy.engine.framework.startup.ui;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.launcher.*;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.random.*;




public class LocalSetupPanel extends SetupPanel implements Observer
{
    private final GameSelectorModel m_gameSelectorModel;
    private List<LocalPlayerComboBoxSelector> m_playerTypes = new ArrayList<LocalPlayerComboBoxSelector>();    
    
    public LocalSetupPanel(GameSelectorModel model)
    {
        m_gameSelectorModel = model;
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
    }

    private void createComponents()
    {

    }

    private void layoutComponents()
    {
        GameData data = m_gameSelectorModel.getGameData();
        
        removeAll();
        m_playerTypes.clear();
        setLayout(new GridBagLayout());

        if(data == null)
        {
          add(new JLabel("No game selected!"));
          return;
        }


        String[] playerTypes =  data.getGameLoader().getServerPlayerTypes();

        String[] playerNames = data.getPlayerList().getNames();
        Arrays.sort(playerNames);

        for(int i = 0; i < playerNames.length; i++)
        {
          LocalPlayerComboBoxSelector selector = new LocalPlayerComboBoxSelector(playerNames[i], playerTypes);
          m_playerTypes.add(selector);
          selector.layout(i, this);
        }


        validate();

    }

    private void setupListeners()
    {
        m_gameSelectorModel.addObserver(this);
    }

    private void setWidgetActivation()
    {

    }
    
    
    @Override
    public boolean canGameStart()
    {
        return m_gameSelectorModel.getGameData() != null;
    }

    @Override
    public void cancel()
    {
        m_gameSelectorModel.deleteObserver(this);
        
    }

    public void update(Observable o, Object arg)
    {
        layoutComponents();
    }
    
    public String getPlayerType(String playerName)
    {
      Iterator<LocalPlayerComboBoxSelector> iter = m_playerTypes.iterator();
      while (iter.hasNext())
      {
        LocalPlayerComboBoxSelector item = iter.next();
        if(item.getPlayerName().equals(playerName))
          return item.getPlayerType();
      }
      throw new IllegalStateException("No player found:" + playerName);
    }

    @Override
    public ILauncher getLauncher()
    {
        IRandomSource randomSource = new PlainRandomSource();
        Map<String,String> playerTypes = new HashMap<String,String>();
        
        for(LocalPlayerComboBoxSelector player : m_playerTypes)
        {
            playerTypes.put(player.getPlayerName(), player.getPlayerType());
        }
        
        LocalLauncher launcher = new LocalLauncher(m_gameSelectorModel, randomSource, playerTypes);
        return launcher;
        
    }

}



class LocalPlayerComboBoxSelector
{
  private final String m_playerName;
  private final JComboBox m_playerTypes;


  LocalPlayerComboBoxSelector(String playerName, String[] types)
  {
    m_playerName = playerName;
    m_playerTypes = new JComboBox(types);

  }

  public void layout(int row, Container container)
  {
    container.add(new JLabel(m_playerName + ":"), new GridBagConstraints(0,row, 1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,5,5),0,0) );
    container.add(m_playerTypes, new GridBagConstraints(1, row, 1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,5,5),0,0) );

  }

  public String getPlayerName()
  {
    return m_playerName;
  }

  public String getPlayerType()
  {
    return (String) m_playerTypes.getSelectedItem();
  }

  

}
