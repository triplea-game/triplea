package games.strategy.kingstable.attachments;

import games.strategy.engine.data.DefaultAttachment;

public class TerritoryAttachment extends DefaultAttachment
{
    private boolean m_isKingsSquare = false;
    private boolean m_isKingsExit = false;
    
    /** Creates new TerritoryAttatchment */
    public TerritoryAttachment()
    {
    }
    
    public void setKingsSquare(String value)
    {
        m_isKingsSquare = getBool(value);
    }

    public boolean isKingsSquare()
    {
        return m_isKingsSquare;
    }

    public void setKingsExit(String value)
    {
        m_isKingsExit = getBool(value);
    }
    
    public boolean isKingsExit()
    {
        return m_isKingsExit;
    }
}
