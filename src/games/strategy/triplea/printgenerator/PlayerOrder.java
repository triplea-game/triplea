/**
 * 
 */
package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Gansito Frito
 * 
 */
public class PlayerOrder
{
    private static Iterator<GameStep> s_gameStepIterator;
    private static GameData s_data;
    private static List<PlayerID> s_playerSet = new ArrayList<PlayerID>();

    private static <E> Set<E> removeDups(Collection<E> c)
    {
        return new LinkedHashSet<E>(c);
    }

    protected static void saveToFile(GameData data) throws IOException
    {
        s_data = data;
        
        s_gameStepIterator = s_data.getSequence().iterator();
        while (s_gameStepIterator.hasNext())
        {
            GameStep currentStep = s_gameStepIterator.next();
            PlayerID currentPlayerID = currentStep.getPlayerID();

            if (currentPlayerID != null && !currentPlayerID.isNull())
            {
                s_playerSet.add(currentPlayerID);
            }

        }

        FileWriter turnWriter = null;
        PrintGenerationData.getOutDir().mkdir();
        File outFile = new File(PrintGenerationData.getOutDir(),
                "General Information.csv");
        turnWriter = new FileWriter(outFile, true);

        turnWriter.write("Turn Order\r\n");

        Set<PlayerID> noDuplicates = removeDups(s_playerSet);

        Iterator<PlayerID> playerIterator = noDuplicates.iterator();
        int count=1;
        while (playerIterator.hasNext())
        {
            PlayerID currentPlayerID = playerIterator.next();
            turnWriter.write(count+". "+currentPlayerID.getName() + "\r\n");
            count++;
        }

        turnWriter.close();
    }
}
