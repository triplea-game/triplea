package games.strategy.engine.random;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.vault.VaultID;

public interface IRemoteRandom extends IRemote {
  /**
   * Generate a random number, and lock it in the vault.
   *
   * @param serverVaultId
   *        - the vaultID where the server has stored his numbers
   * @return the vault id for which we have locked the data
   */
  int[] generate(int max, int count, String annotation, VaultID serverVaultId);

  /**
   * unlock the random number last generated.
   */
  void verifyNumbers();
}
