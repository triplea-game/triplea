package games.strategy.kingstable.ui;

import games.strategy.common.ui.IPlayData;
import games.strategy.engine.data.Territory;

public class PlayData implements IPlayData
{
    private Territory m_start;
    private Territory m_end;
    
    public PlayData(Territory start, Territory end)
    {
        m_start = start;
        m_end = end;
    }
    
    public Territory getStart()
    {
        return m_start;
    }
    
    public Territory getEnd() 
    {
        return m_end;
    }
}
