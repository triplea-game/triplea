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

package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Gansito Frito
 * 
 */
public class IPCInfo
{

    private GameData m_data;
    private Map<PlayerID, Map<Resource, Integer>> m_infoMap = new HashMap<PlayerID, Map<Resource,Integer>>();
    private Iterator<PlayerID> m_playerIterator;
    
    private PrintGenerationData m_printData;

    protected void saveToFile(PrintGenerationData printData)
    {
        m_data = printData.getData();
        m_printData=printData;

        m_playerIterator = m_data.getPlayerList().iterator();

        while (m_playerIterator.hasNext())
        {
            PlayerID currentPlayer = m_playerIterator.next();
            Iterator<Resource> resourceIterator = m_data.getResourceList().getResources().iterator();
            Map<Resource, Integer> resourceMap = new HashMap<Resource, Integer>();
            while (resourceIterator.hasNext())
            {
                Resource currentResource = resourceIterator.next();
                Integer amountOfResource = currentPlayer.getResources()
                        .getQuantity(currentResource);
                resourceMap.put(currentResource, amountOfResource);
            }
            m_infoMap.put(currentPlayer, resourceMap);
        }
        
        FileWriter resourceWriter=null;
        try
        {
            File outFile = new File(m_printData.getOutDir(), "General Information.csv");
            resourceWriter = new FileWriter(outFile, true);

            //Print Title
            int numResources=m_data.getResourceList().size();
            for(int i=0; i<numResources/2-1+numResources%2; i++)
            {
                resourceWriter.write(",");
            }
            resourceWriter.write("Resource Chart");
            for(int i=0; i<numResources/2-numResources%2; i++)
            {
                resourceWriter.write(",");
            }
            resourceWriter.write("\r\n");
            
            //Print Resources
            
            Iterator<Resource> resourceIterator=m_data.getResourceList().getResources().iterator();
            resourceWriter.write(",");
            while(resourceIterator.hasNext())
            {
                Resource currentResource=resourceIterator.next();
                resourceWriter.write(currentResource.getName()+",");
            }
            resourceWriter.write("\r\n");
            
            
            //Print Player's and Resource Amount's
            
            m_playerIterator=m_data.getPlayerList().iterator();
            while(m_playerIterator.hasNext())
            {
                PlayerID currentPlayer=m_playerIterator.next();
                resourceWriter.write(currentPlayer.getName());
                Map<Resource, Integer> resourceMap=m_infoMap.get(currentPlayer);
                Iterator<Resource> resIterator=resourceMap.keySet().iterator();
                while(resIterator.hasNext())
                {
                    Resource currentResource=resIterator.next();
                    Integer amountResource=resourceMap.get(currentResource);
                    resourceWriter.write(","+amountResource);
                }
                resourceWriter.write("\r\n");
            }
            resourceWriter.write("\r\n");

            resourceWriter.close();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
       

    }

}
