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

import games.strategy.engine.vault.VaultID;
import games.strategy.net.IRemote;

/**
 * @author Sean Bridges
 */
public interface IRemoteRandom extends IRemote
{
    /**
     * 
     * Generate a random number, and lock it in the vault.
     * 
     * @param serverVaultID - the vaultID where the server has stored his numbers
     * 
     * @return the vault id for which we have locked the data
     */
    public VaultID generate(int max, int count, String annotation, VaultID serverVaultID);
    
    /**
     * unlock the random number last generated.
     *
     */
    public void unlock();
}
