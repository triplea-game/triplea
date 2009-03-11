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

import java.io.File;
import java.util.Map;

public class PrintGenerationData
{
    private File m_outDir;
    private Map<Integer, Integer> m_SameIPCMap;
    private GameData m_data;
    /**
     * General Constructor
     */
    protected PrintGenerationData()
    {
    }
    /**
     * @return the outDir
     */
    protected File getOutDir()
    {
        return m_outDir;
    }
    /**
     * @param outDir the outDir to set
     */
    protected void setOutDir(File outDir)
    {
        m_outDir = outDir;
    }
    /**
     * @return the sameIPCMap
     */
    protected Map<Integer, Integer> getSameIPCMap()
    {
        return m_SameIPCMap;
    }
    /**
     * @param sameIPCMap the sameIPCMap to set
     */
    protected void setSameIPCMap(Map<Integer, Integer> sameIPCMap)
    {
        m_SameIPCMap = sameIPCMap;
    }
    /**
     * @return the data
     */
    protected GameData getData()
    {
        return m_data;
    }
    /**
     * @param data the data to set
     */
    protected void setData(GameData data)
    {
        m_data = data;
    }
    
    
}
