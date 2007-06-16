/**
 * 
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
    private static GameData s_data;
    private static Iterator<PlayerID> s_playerIterator;
    private static IntegerMap<PlayerID> s_moneyMap;
    private static int s_numPlayers;
    private static PlayerID[] s_playerArray;
    private static Integer[] s_moneyArray;
    // private static Map<PlayerID, PlayerID> s_avoidMap = new HashMap<PlayerID,
    // PlayerID>();
    private static Map<Integer, Integer> s_avoidMap;

    private static final int X_DIMENSION = 7;
    private static final int Y_DIMENSION = 6;
    private static final Font CHART_FONT = new Font("Serif", Font.PLAIN, 12);
    private static BufferedImage s_ipcImage;
    private static Graphics2D s_g2d;

    private static File s_outDir;

    IPCChart()
    {
        s_data = PrintGenerationData.getData();
        s_playerIterator = s_data.getPlayerList().iterator();
        s_moneyMap = new IntegerMap<PlayerID>();
        s_numPlayers = s_data.getPlayerList().size();
        s_playerArray = new PlayerID[s_numPlayers];
        s_moneyArray = new Integer[s_numPlayers];
        s_avoidMap = new HashMap<Integer, Integer>();
        s_ipcImage = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
        s_g2d=s_ipcImage.createGraphics();
        s_outDir=PrintGenerationData.getOutDir();
    }

    private static void initializeMap()
    {
        int count = 0;
        while (s_playerIterator.hasNext())
        {
            PlayerID currentPlayer = s_playerIterator.next();
            s_moneyMap.put(currentPlayer, currentPlayer.getResources()
                    .getQuantity(Constants.IPCS));

            s_playerArray[count] = currentPlayer;
            s_moneyArray[count] = currentPlayer.getResources().getQuantity(
                    Constants.IPCS);
            count++;
        }
    }

    private static void initializeAvoidMap()
    {
        for (int i = 0; i < s_numPlayers - 1; i++)
        {
            for (int j = i + 1; j < s_numPlayers; j++)
            {
                Integer firstPlayerMoney = s_moneyArray[i];
                Integer secondPlayerMoney = s_moneyArray[j];
                if (firstPlayerMoney == secondPlayerMoney)
                {
                    // s_avoidMap.put(s_playerArray[i], s_playerArray[j]);
                    s_avoidMap.put(i, j);
                }
            }
        }
    }

    private static void drawEllipseAndString(int x, int y, String string)
    {
        s_g2d.setFont(CHART_FONT);
        s_g2d.draw(new Ellipse2D.Double(5 + 87 * x, 5 + 87 * y, 72, 72));
        final FontMetrics metrics = s_g2d.getFontMetrics();
        final int h = metrics.stringWidth(string) / 2;
        final int k = metrics.getHeight() / 2;

        s_g2d.drawString(string, 42 + 87 * x - h, 39 + 87 * y + k);
    }

    protected void saveToFile() throws IOException
    {
        initializeMap();
        initializeAvoidMap();
        int numChartsNeeded = (int) Math.ceil(((double) s_moneyMap
                .totalValues())
                / (X_DIMENSION * Y_DIMENSION));
        for (int i = 0; i <= numChartsNeeded; i++)
        {
            s_g2d.setColor(Color.black);
            // Draw Country Names
            for (int z = 0; z < s_playerArray.length; z++)
            {
                int valMod42 = s_moneyArray[z] % 42;
                int valModXDim = valMod42 % X_DIMENSION;
                int valFloorXDim = valMod42 / X_DIMENSION;

                if (s_avoidMap.containsKey(z) && s_moneyArray[z] / 42 == i)
                {
                    FontMetrics metrics = s_g2d.getFontMetrics();
                    int width = metrics.stringWidth(s_playerArray[z].getName()) / 2;
                    s_g2d.drawString(s_playerArray[z].getName(), 42 + 87
                            * valModXDim - width, 63 + 87 * valFloorXDim);
                } else if (s_avoidMap.containsValue(z)
                        && s_moneyArray[z] / 42 == i)
                {
                    FontMetrics metrics = s_g2d.getFontMetrics();
                    int width = metrics.stringWidth(s_playerArray[z].getName()) / 2;
                    s_g2d.drawString(s_playerArray[z].getName(), 42 + 87
                            * valModXDim - width, 30 + 87 * valFloorXDim);
                } else if (s_moneyArray[z] / 42 == i)
                {
                    FontMetrics metrics = s_g2d.getFontMetrics();
                    int width = metrics.stringWidth(s_playerArray[z].getName()) / 2;
                    s_g2d.drawString(s_playerArray[z].getName(), 42 + 87
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
            final File outputfile = new File(s_outDir, "IPCchart" + firstnum
                    + "-" + secondnum + ".png");
            ImageIO.write(s_ipcImage, "png", outputfile);

            final Color transparent = new Color(0, 0, 0, 0);
            s_g2d.setColor(transparent);
            s_g2d.setComposite(AlphaComposite.Src);
            s_g2d.fill(new Rectangle2D.Float(0, 0, 600, 600));
        }
    }

}
