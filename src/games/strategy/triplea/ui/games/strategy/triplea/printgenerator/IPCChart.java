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
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * @author Gansito Frito
 * 
 */
class IPCChart
{
    private GameData m_data;
    private Iterator<PlayerID> m_playerIterator;
    private IntegerMap<PlayerID> m_moneyMap;
    private int m_numPlayers;
    private PlayerID[] m_playerArray;
    private Integer[] m_moneyArray;
    private Map<Integer, Integer> m_avoidMap;

    private final int X_DIMENSION = 7;
    private final int Y_DIMENSION = 6;
    private final Font CHART_FONT = new Font("Serif", Font.PLAIN, 12);
    private BufferedImage m_ipcImage;
    private Graphics2D m_g2d;

    private File m_outDir;

    IPCChart(PrintGenerationData printData)
    {
        m_data = printData.getData();
        m_playerIterator = m_data.getPlayerList().iterator();
        m_moneyMap = new IntegerMap<PlayerID>();
        m_numPlayers = m_data.getPlayerList().size();
        m_playerArray = new PlayerID[m_numPlayers];
        m_moneyArray = new Integer[m_numPlayers];
        m_avoidMap = new HashMap<Integer, Integer>();
        m_ipcImage = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
        m_g2d=m_ipcImage.createGraphics();
        m_outDir=printData.getOutDir();
    }

    private void initializeMap()
    {
        int count = 0;
        while (m_playerIterator.hasNext())
        {
            PlayerID currentPlayer = m_playerIterator.next();
            m_moneyMap.put(currentPlayer, currentPlayer.getResources()
                    .getQuantity(Constants.IPCS));

            m_playerArray[count] = currentPlayer;
            m_moneyArray[count] = currentPlayer.getResources().getQuantity(
                    Constants.IPCS);
            count++;
        }
    }

    private void initializeAvoidMap()
    {
        for (int i = 0; i < m_numPlayers - 1; i++)
        {
            for (int j = i + 1; j < m_numPlayers; j++)
            {
                Integer firstPlayerMoney = m_moneyArray[i];
                Integer secondPlayerMoney = m_moneyArray[j];
                if (firstPlayerMoney == secondPlayerMoney)
                {
                    // s_avoidMap.put(s_playerArray[i], s_playerArray[j]);
                    m_avoidMap.put(i, j);
                }
            }
        }
    }

    private void drawEllipseAndString(int x, int y, String string)
    {
        m_g2d.setFont(CHART_FONT);
        m_g2d.draw(new Ellipse2D.Double(5 + 87 * x, 5 + 87 * y, 72, 72));
        final FontMetrics metrics = m_g2d.getFontMetrics();
        final int h = metrics.stringWidth(string) / 2;
        final int k = metrics.getHeight() / 2;

        m_g2d.drawString(string, 42 + 87 * x - h, 39 + 87 * y + k);
    }

    protected void saveToFile() throws IOException
    {
        initializeMap();
        initializeAvoidMap();
        int numChartsNeeded = (int) Math.ceil(((double) m_moneyMap
                .totalValues())
                / (X_DIMENSION * Y_DIMENSION));
        for (int i = 0; i < numChartsNeeded; i++)
        {
            m_g2d.setColor(Color.black);
            // Draw Country Names
            for (int z = 0; z < m_playerArray.length; z++)
            {
                int valMod42 = m_moneyArray[z] % 42;
                int valModXDim = valMod42 % X_DIMENSION;
                int valFloorXDim = valMod42 / X_DIMENSION;

                if (m_avoidMap.containsKey(z) && m_moneyArray[z] / 42 == i)
                {
                    FontMetrics metrics = m_g2d.getFontMetrics();
                    int width = metrics.stringWidth(m_playerArray[z].getName()) / 2;
                    m_g2d.drawString(m_playerArray[z].getName(), 42 + 87
                            * valModXDim - width, 63 + 87 * valFloorXDim);
                } else if (m_avoidMap.containsValue(z)
                        && m_moneyArray[z] / 42 == i)
                {
                    FontMetrics metrics = m_g2d.getFontMetrics();
                    int width = metrics.stringWidth(m_playerArray[z].getName()) / 2;
                    m_g2d.drawString(m_playerArray[z].getName(), 42 + 87
                            * valModXDim - width, 30 + 87 * valFloorXDim);
                } else if (m_moneyArray[z] / 42 == i)
                {
                    FontMetrics metrics = m_g2d.getFontMetrics();
                    int width = metrics.stringWidth(m_playerArray[z].getName()) / 2;
                    m_g2d.drawString(m_playerArray[z].getName(), 42 + 87
                            * valModXDim - width, 60 + 87 * valFloorXDim);
                }
            }

            // Draw Ellipses and Numbers
            for (int j = 0; j < Y_DIMENSION; j++)
            {
                for (int k = 0; k < X_DIMENSION; k++)
                {
                    int numberincircle = X_DIMENSION * Y_DIMENSION * i
                            + X_DIMENSION * j + k;
                    String string = "" + numberincircle;
                    drawEllipseAndString(k, j, string);
                }
            }

            // Write to file
            final int firstnum = X_DIMENSION * Y_DIMENSION * i;
            final int secondnum = X_DIMENSION * Y_DIMENSION * (i + 1) - 1;
            final File outputfile = new File(m_outDir, "IPCchart" + firstnum
                    + "-" + secondnum + ".png");
            ImageIO.write(m_ipcImage, "png", outputfile);

            final Color transparent = new Color(0, 0, 0, 0);
            m_g2d.setColor(transparent);
            m_g2d.setComposite(AlphaComposite.Src);
            m_g2d.fill(new Rectangle2D.Float(0, 0, 600, 600));
        }
    }

}
