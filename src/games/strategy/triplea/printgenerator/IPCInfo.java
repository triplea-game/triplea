/**
 * 
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

    private static GameData s_data;
    private static Map<PlayerID, Map<Resource, Integer>> s_infoMap = new HashMap<PlayerID, Map<Resource,Integer>>();
    private static Iterator<PlayerID> s_playerIterator;

    protected static void saveToFile(GameData data)
    {
        s_data = data;

        s_playerIterator = s_data.getPlayerList().iterator();

        while (s_playerIterator.hasNext())
        {
            PlayerID currentPlayer = s_playerIterator.next();
            Iterator<Resource> resourceIterator = s_data.getResourceList().getResources().iterator();
            Map<Resource, Integer> resourceMap = new HashMap<Resource, Integer>();
            while (resourceIterator.hasNext())
            {
                Resource currentResource = resourceIterator.next();
                Integer amountOfResource = currentPlayer.getResources()
                        .getQuantity(currentResource);
                resourceMap.put(currentResource, amountOfResource);
            }
            s_infoMap.put(currentPlayer, resourceMap);
        }
        
        FileWriter resourceWriter=null;
        try
        {
            File outFile = new File(PrintGenerationData.getOutDir(), "General Information.csv");
            resourceWriter = new FileWriter(outFile, true);

            //Print Title
            int numResources=s_data.getResourceList().size();
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
            
            Iterator<Resource> resourceIterator=s_data.getResourceList().getResources().iterator();
            resourceWriter.write(",");
            while(resourceIterator.hasNext())
            {
                Resource currentResource=resourceIterator.next();
                resourceWriter.write(currentResource.getName()+",");
            }
            resourceWriter.write("\r\n");
            
            
            //Print Player's and Resource Amount's
            
            s_playerIterator=s_data.getPlayerList().iterator();
            while(s_playerIterator.hasNext())
            {
                PlayerID currentPlayer=s_playerIterator.next();
                resourceWriter.write(currentPlayer.getName());
                Map<Resource, Integer> resourceMap=s_infoMap.get(currentPlayer);
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
