/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.Dynamix_AI.UI;

import games.strategy.engine.data.GameData;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

/**
 *
 * @author Stephen
 */
public class UI
{
    public static void Initialize(GameData data)
    {
        try
        {
            SwingUtilities.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    s_settingsWindow = new SettingsWindow();
                }
            });
        }
        catch (Exception ex)
        {
            System.out.print("Error initializing Dynamix settings window. \r\n");
        }
    }

    private static SettingsWindow s_settingsWindow = null;
    public static void ShowSettingsWindow()
    {
        s_settingsWindow.setVisible(true);
    }
    public static void NotifyAILogMessage(Level level, String message)
    {
        s_settingsWindow.addMessage(level, message);
    }
    public static void NotifyStartOfRound(int round)
    {
        s_settingsWindow.notifyNewRound(round);
    }
}
