package games.strategy.triplea.attatchments;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.triplea.Constants;

public class CanalAttachment extends DefaultAttachment
{
    private String m_canalName;
    private String[] m_landTerritories;

    public static Set<Territory> getAllCanalSeaZones(String canalName, GameData data)
    {
        Set<Territory> rVal = new HashSet<Territory>();       
        for(Territory t : data.getMap())
        {
            Set<CanalAttachment> canalAttachments = get(t);
            if(canalAttachments.isEmpty())
                continue;
            
            Iterator<CanalAttachment> iter = canalAttachments.iterator();
            while(iter.hasNext() )
            {
                CanalAttachment canalAttachment = iter.next();
                if (canalAttachment.getCanalName().equals(canalName))
                {
                    rVal.add(t);
                }
            }
        }
        
        if(rVal.size() != 2)
            throw new IllegalStateException("Wrong number of sea zones for canal:" + rVal );
        
        return rVal;
    }
    
    public static Set<CanalAttachment> get(Territory t)
    {
        Set<CanalAttachment> rVal = new HashSet<CanalAttachment>();
        Map<String, IAttachment> map = t.getAttachments();
        Iterator<String> iter = map.keySet().iterator();
        while(iter.hasNext() )
        {
            IAttachment attachment = map.get(iter.next());
            String name = attachment.getName();
            if (name.startsWith(Constants.CANAL_ATTATCHMENT_PREFIX))
            {
                rVal.add((CanalAttachment)attachment);
            }
        }
        return rVal;
        
    }
    
    public void setCanalName(String name)
    {
        m_canalName = name;
    }
    
    public String getCanalName()
    {
        return m_canalName;
    }
    
    public void setLandTerritories(String landTerritories)
    {
        m_landTerritories = landTerritories.split(":");
    }
    
    /**
     * Called after the attatchment is created.
     */
    public void validate() throws GameParseException
    {
        if(m_canalName == null || m_landTerritories == null ||m_landTerritories.length == 0)
            throw new IllegalStateException("Canal error for:" + m_canalName + " not all variables set, land:" + m_landTerritories);
        getLandTerritories();
    }
    
    public Collection<Territory> getLandTerritories()    
    {
        List<Territory> rVal = new ArrayList<Territory>();
        
        for(String name : m_landTerritories)
        {
            Territory territory = getData().getMap().getTerritory(name);
            if(territory == null)
                throw new IllegalStateException("No territory called:" + territory); 
            rVal.add(territory);
        }        
        return rVal;
    }
    
}
