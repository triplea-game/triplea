package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;

import java.io.File;
import java.util.Map;

public class PrintGenerationData
{
    private static File s_outDir;
    private static Map<Integer, Integer> s_SameIPCMap;
    private static GameData s_data;
    

    /**
     * @return the outDir
     */
    protected static File getOutDir()
    {
        return s_outDir;
    }

    /**
     * @param outDir the outDir to set
     */
    protected static void setOutDir(File outDir)
    {
        s_outDir = outDir;
    }

    /**
     * @return the sameIPCMap
     */
    protected static Map<Integer, Integer> getSameIPCMap()
    {
        return s_SameIPCMap;
    }

    /**
     * @param sameIPCMap the sameIPCMap to set
     */
    protected static void setSameIPCMap(Map<Integer, Integer> sameIPCMap)
    {
        s_SameIPCMap = sameIPCMap;
    }

    /**
     * @return the data
     */
    protected static GameData getData()
    {
        return s_data;
    }

    /**
     * @param data the data to set
     */
    protected static void setData(GameData data)
    {
        s_data = data;
    }


}
