package games.strategy.engine.random;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.vault.VaultId;

/**
 * A service that generates random numbers. All generated numbers are stored in a cryptographic
 * vault to prevent tampering.
 */
public interface IRemoteRandom extends IRemote {
  /**
   * Generate a random number, and lock it in the vault.
   *
   * @param serverVaultId - the vaultID where the server has stored his numbers
   * @return the vault id for which we have locked the data
   */
  @RemoteActionCode(0)
  int[] generate(int max, int count, String annotation, VaultId serverVaultId);

  /** unlock the random number last generated. */
  @RemoteActionCode(1)
  void verifyNumbers();
}
