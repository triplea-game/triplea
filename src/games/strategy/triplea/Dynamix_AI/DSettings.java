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

package games.strategy.triplea.Dynamix_AI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 * @author Stephen
 */
public class DSettings implements Serializable
{
    public boolean UseActionLengthGoals = true;

    public int PurchaseWait_AL = 750;
    public int CombatMoveWait_AL = 750;
    public int NonCombatMoveWait_AL = 750;
    public int PlacementWait_AL = 750;

    public int PurchaseWait_AW = 250;
    public int CombatMoveWait_AW = 250;
    public int NonCombatMoveWait_AW = 250;
    public int PlacementWait_AW = 250;

    private static DSettings s_lastSettings = null;
    private static String PROGRAM_SETTINGS = "Program Settings";
    public static DSettings LoadSettings()
    {
        if (s_lastSettings == null)
        {
            DSettings result = new DSettings();
            try
            {
                byte[] pool = Preferences.userNodeForPackage(Dynamix_AI.class).getByteArray(PROGRAM_SETTINGS, null);
                if (pool != null)
                {
                    result = (DSettings) new ObjectInputStream(new ByteArrayInputStream(pool)).readObject();
                }
            }
            catch (Exception ex)
            {
            }
            if (result == null)
            {
                result = new DSettings();
            }
            s_lastSettings = result;
            return result;
        }
        else
            return s_lastSettings;
    }
    public static void SaveSettings(DSettings settings)
    {
        s_lastSettings = settings;
        ObjectOutputStream outputStream = null;
        try
        {
            ByteArrayOutputStream pool = new ByteArrayOutputStream(10000);
            outputStream = new ObjectOutputStream(pool);
            outputStream.writeObject(settings);

            Preferences prefs = Preferences.userNodeForPackage(Dynamix_AI.class);
            prefs.putByteArray(PROGRAM_SETTINGS, pool.toByteArray());
            try
            {
                prefs.flush();
            }
            catch (BackingStoreException ex)
            {
                ex.printStackTrace();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                outputStream.close();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
}
