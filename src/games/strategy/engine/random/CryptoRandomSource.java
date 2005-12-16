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

package games.strategy.engine.random;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.*;
import games.strategy.engine.vault.*;

/**
 * A random source that generates numbers using a secure algorithm shared
 * between two players.
 * 
 * Code originally contributed by Ben Giddings.
 */

public class CryptoRandomSource implements IRandomSource
{

    private final IRandomSource m_plainRandom = new PlainRandomSource();

    /**
     * converts an int[] to a bytep[
     */
    public static byte[] intsToBytes(int[] ints)
    {
        byte[] rVal = new byte[ints.length * 4];

        for (int i = 0; i < ints.length; i++)
        {
            rVal[4 * i] = (byte) (0x000000FF & ints[i]);
            rVal[(4 * i) + 1] = (byte) ((0x000000FF & (ints[i] >> 8)));
            rVal[(4 * i) + 2] = (byte) ((0x000000FF & (ints[i] >> 16)));
            rVal[(4 * i) + 3] = (byte) ((0x000000FF & (ints[i] >> 24)));
        }

        return rVal;
    }

    static int byteToIntUnsigned(byte val)
    {
        return ((int) val) & 0xff;  
    }
    
    public static int[] bytesToInts(byte[] bytes)
    {
        int[] rVal = new int[bytes.length / 4];
        for (int i = 0; i < rVal.length; i++)
        {
            rVal[i] = byteToIntUnsigned(bytes[4*i]) +
            		  (byteToIntUnsigned(bytes[4*i + 1]) << 8) +
            		  (byteToIntUnsigned(bytes[4*i + 2]) << 16) +
            		  (byteToIntUnsigned(bytes[4*i + 3]) << 24);
        }

        return rVal;
    }

    public static int[] xor(int[] val1, int[] val2, int max)
    {
        if (val1.length != val2.length)
        {
            throw new IllegalArgumentException("Arrays not of same length");
        }
        int[] rVal = new int[val1.length];
        for (int i = 0; i < val1.length; i++)
        {
            rVal[i] = (val1[i] + val2[i]) % max;
        }

        return rVal;

    }

    //the remote players who involved in rolling the dice
    //dice are rolled securly between us and her
    final private PlayerID m_remotePlayer;

    final private IGame m_game;

    public CryptoRandomSource(PlayerID remotePlayer, IGame game)
    {
        m_remotePlayer = remotePlayer;
        m_game = game;

    }

    /**
     * All delegates should use random data that comes from both players so that
     * neither player cheats.
     */
    public int getRandom(int max, String annotation)
    {
        return getRandom(max, 1, annotation)[0];
    }

    /**
     * Delegates should not use random data that comes from any other source.
     */
    public int[] getRandom(int max, int count, String annotation)
    {
        if (count <= 0)
            throw new IllegalArgumentException("Invalid count:" + count);

        Vault vault = m_game.getVault();

        //generate numbers locally, and put them in the vault
        int[] localRandom = m_plainRandom.getRandom(max, count, annotation);
        
        //lock it so the client knows that its there, but cant read it
        VaultID localID = vault.lock(intsToBytes(localRandom));

        //ask the remote to generate numbers
        IRemoteRandom remote = (IRemoteRandom) (m_game.getRemoteMessenger().getRemote(ServerGame.getRemoteRandomName(m_remotePlayer)));
        int[] remoteNumbers = remote.generate(max, count, annotation, localID);


        //unlock ours, tell the client he can verify
        vault.unlock(localID);
        remote.verifyNumbers();
        
        //finally, we join the two together to get the real value
        return xor(localRandom, remoteNumbers, max);

    }
}