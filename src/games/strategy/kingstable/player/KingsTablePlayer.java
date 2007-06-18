package games.strategy.kingstable.player;

import games.strategy.common.player.AbstractHumanPlayer;
import games.strategy.kingstable.delegate.remote.IPlayDelegate;
import games.strategy.kingstable.ui.PlayData;

public class KingsTablePlayer extends AbstractHumanPlayer implements IKingsTablePlayer
{

    public KingsTablePlayer(String name)
    {
        super(name);
    }
    

    @Override
    public void start(String stepName)
    {
        if (stepName.endsWith("Play"))
            play();
        else
            throw new IllegalArgumentException("Unrecognized step stepName:" + stepName);
    }
    
    private void play() 
    {   

        PlayData play = null;
        IPlayDelegate playDel = (IPlayDelegate) m_bridge.getRemote();
        
        while (play == null)
        {             
            play = (PlayData) m_ui.waitForPlay(m_id, m_bridge);

            String error = playDel.play(play.getStart(),play.getEnd());
            if(error != null)
            {
                m_ui.notifyError(error);
                play = null;
            }

        }
        
        //System.out.println("Starting at " + play.getStart().getName());
        //System.out.println("Ending at " + play.getEnd().getName());
        
       /*    
        Territory end = null;// = m_bridge.getGameData().getMap().getTerritoryFromCoordinates(3,3);
        while (end == null || end == start)// || !at.getUnits().isEmpty())
        {             
            end = m_ui.waitForPlay(m_id, m_bridge);

        }
        System.out.println("Ending at " + end.getName());
        */
        
        
        /*
        if(error != null)
            m_ui.notifyError(error);
        else 
        {
            m_ui.repaintGridSquare(start);
            m_ui.repaintGridSquare(end);
        }
        */
    }

}
