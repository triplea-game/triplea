package games.strategy.engine.vault;

import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.RemoteName;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import org.jetbrains.annotations.NonNls;

/**
 * A vault is a secure way for the client and server to share information without trusting each
 * other.
 *
 * <p>Data can be locked in the vault by a node. This data then is not readable by other nodes until
 * the data is unlocked.
 *
 * <p>When the data is unlocked by the original node, other nodes can read the data. When data is
 * put in the vault, it cant be changed by the originating node.
 *
 * <p>NOTE: to allow the data locked in the vault to be gc'd, the <code>release(VaultId id)</code>
 * method should be called when it is no longer needed.
 */
public class Vault {
  private static final RemoteName VAULT_CHANNEL =
      new RemoteName("games.strategy.engine.vault.IServerVault.VAULT_CHANNEL", IRemoteVault.class);
  @NonNls private static final String ALGORITHM = "DES";
  // 0xCAFEBABE
  // we encrypt both this value and data when we encrypt data.
  // when decrypting we ensure that KNOWN_VAL is correct and thus guarantee that we are being given
  // the right key
  private static final byte[] KNOWN_VAL = new byte[] {0xC, 0xA, 0xF, 0xE, 0xB, 0xA, 0xB, 0xE};

  private final SecretKeyFactory secretKeyFactory;
  private final KeyGenerator keyGen;
  private final IChannelMessenger channelMessenger;
  // Maps VaultId -> SecretKey
  private final ConcurrentMap<VaultId, SecretKey> secretKeys = new ConcurrentHashMap<>();
  // maps VaultId -> encrypted byte[]
  private final ConcurrentMap<VaultId, byte[]> unverifiedValues = new ConcurrentHashMap<>();
  // maps VaultId -> byte[]
  private final ConcurrentMap<VaultId, byte[]> verifiedValues = new ConcurrentHashMap<>();
  private final Object waitForLock = new Object();
  private final IRemoteVault remoteVault =
      new IRemoteVault() {
        @Override
        public void addLockedValue(final VaultId id, final byte[] data) {
          if (id.getGeneratedOn().equals(channelMessenger.getLocalNode())) {
            return;
          }
          if (unverifiedValues.putIfAbsent(id, data) != null) {
            throw new IllegalStateException("duplicate values for id: " + id);
          }
          synchronized (waitForLock) {
            waitForLock.notifyAll();
          }
        }

        @Override
        public void unlock(final VaultId id, final byte[] secretKeyBytes) {
          if (id.getGeneratedOn().equals(channelMessenger.getLocalNode())) {
            return;
          }
          final SecretKey key = bytesToKey(secretKeyBytes);
          final Cipher cipher;
          try {
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
          } catch (final NoSuchAlgorithmException
              | InvalidKeyException
              | NoSuchPaddingException e) {
            throw new IllegalStateException(e);
          }
          final byte[] encrypted = unverifiedValues.remove(id);
          final byte[] decrypted;
          try {
            decrypted = cipher.doFinal(encrypted);
          } catch (final Exception e1) {
            throw new IllegalStateException("Failed to decrypt vault values", e1);
          }
          if (decrypted.length < KNOWN_VAL.length) {
            throw new IllegalStateException(
                "decrypted is not long enough to have known value, cheating is suspected");
          }
          // check that the known value is correct
          // we use the known value to check that the key given to
          // us was the key used to encrypt the value in the first place
          for (int i = 0; i < KNOWN_VAL.length; i++) {
            if (KNOWN_VAL[i] != decrypted[i]) {
              throw new IllegalStateException(
                  "Known value of cipher not correct, cheating is suspected");
            }
          }
          final byte[] data = new byte[decrypted.length - KNOWN_VAL.length];
          System.arraycopy(decrypted, KNOWN_VAL.length, data, 0, data.length);
          if (verifiedValues.putIfAbsent(id, data) != null) {
            throw new IllegalStateException("duplicate values for id: " + id);
          }
          synchronized (waitForLock) {
            waitForLock.notifyAll();
          }
        }

        @Override
        public void release(final VaultId id) {
          unverifiedValues.remove(id);
          verifiedValues.remove(id);
        }
      };

  public Vault(final IChannelMessenger channelMessenger) {
    this.channelMessenger = channelMessenger;
    this.channelMessenger.registerChannelSubscriber(remoteVault, VAULT_CHANNEL);
    try {
      secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
      keyGen = KeyGenerator.getInstance(ALGORITHM);
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException("Nothing known about algorithm: " + ALGORITHM, e);
    }
  }

  public void shutDown() {
    channelMessenger.unregisterChannelSubscriber(remoteVault, VAULT_CHANNEL);
  }

  private SecretKey bytesToKey(final byte[] bytes) {
    try {
      final DESKeySpec spec = new DESKeySpec(bytes);
      return secretKeyFactory.generateSecret(spec);
    } catch (final GeneralSecurityException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  private byte[] secretKeyToBytes(final SecretKey key) {
    try {
      final DESKeySpec ks = (DESKeySpec) secretKeyFactory.getKeySpec(key, DESKeySpec.class);
      return ks.getKey();
    } catch (final GeneralSecurityException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

  private IRemoteVault getRemoteBroadcaster() {
    return (IRemoteVault) channelMessenger.getChannelBroadcaster(VAULT_CHANNEL);
  }

  /**
   * Place data in the vault. An encrypted form of the data is sent at this time to all nodes.
   *
   * <p>The same key used to encrypt the KNOWN_VALUE so that nodes can verify the key when it is
   * used to decrypt the data.
   *
   * @param data - the data to lock
   * @return the VaultId of the data
   */
  public VaultId lock(final byte[] data) {
    final VaultId id = new VaultId(channelMessenger.getLocalNode());
    final SecretKey key = keyGen.generateKey();
    if (secretKeys.putIfAbsent(id, key) != null) {
      throw new IllegalStateException("duplicate id: " + id);
    }
    // we already know it, so might as well keep it
    verifiedValues.put(id, data);
    final Cipher cipher;
    try {
      cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, key);
    } catch (final NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException e) {
      throw new IllegalStateException(e);
    }
    // join the data and known value into one array
    final byte[] dataAndCheck = joinDataAndKnown(data);
    final byte[] encrypted;
    try {
      encrypted = cipher.doFinal(dataAndCheck);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
    // tell the world
    getRemoteBroadcaster().addLockedValue(id, encrypted);
    return id;
  }

  /**
   * Join known and data into one array.
   *
   * <p>package access so we can test.
   */
  static byte[] joinDataAndKnown(final byte[] data) {
    final byte[] dataAndCheck = new byte[KNOWN_VAL.length + data.length];
    System.arraycopy(KNOWN_VAL, 0, dataAndCheck, 0, KNOWN_VAL.length);
    System.arraycopy(data, 0, dataAndCheck, KNOWN_VAL.length, data.length);
    return dataAndCheck;
  }

  /**
   * allow other nodes to see the data.
   *
   * <p>You can only unlock data that was locked by the same instance of the Vault
   *
   * @param id - the vault id to unlock
   */
  public void unlock(final VaultId id) {
    if (!id.getGeneratedOn().equals(channelMessenger.getLocalNode())) {
      throw new IllegalArgumentException("Can't unlock data that wasn't locked on this node");
    }
    final SecretKey key = secretKeys.remove(id);
    // let everyone unlock it
    getRemoteBroadcaster().unlock(id, secretKeyToBytes(key));
  }

  /**
   * Note - if an id has been released, then this will return false. If this instance of vault
   * locked id, then this method will return true if the id has not been released.
   *
   * @return - has this id been unlocked
   */
  public boolean isUnlocked(final VaultId id) {
    return verifiedValues.containsKey(id);
  }

  /** Get the unlocked data. */
  public byte[] get(final VaultId id) throws NotUnlockedException {
    if (verifiedValues.containsKey(id)) {
      return verifiedValues.get(id);
    } else if (unverifiedValues.containsKey(id)) {
      throw new NotUnlockedException();
    } else {
      throw new IllegalStateException("Nothing known about id: " + id);
    }
  }

  /** {@code @TODO} Do we know about the given vault id? */
  public boolean knowsAbout(final VaultId id) {
    return verifiedValues.containsKey(id) || unverifiedValues.containsKey(id);
  }

  public List<VaultId> knownIds() {
    final List<VaultId> knownIds = new ArrayList<>(verifiedValues.keySet());
    knownIds.addAll(unverifiedValues.keySet());
    return knownIds;
  }

  /**
   * Allow all data associated with the given vault id to be released and garbage collected.
   *
   * <p>An id can be released by any node.
   *
   * <p>If the id has already been released, then nothing will happen.
   */
  public void release(final VaultId id) {
    getRemoteBroadcaster().release(id);
  }

  /** Waits until we know about a given vault id. waits for at most timeout milliseconds */
  public void waitForId(final VaultId id, final long timeoutMs) {
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("Must supply positive timeout argument");
    }
    final long endTime = timeoutMs + System.currentTimeMillis();
    while (System.currentTimeMillis() < endTime && !knowsAbout(id)) {
      synchronized (waitForLock) {
        if (knowsAbout(id)) {
          return;
        }
        try {
          final long waitTime = endTime - System.currentTimeMillis();
          if (waitTime > 0) {
            waitForLock.wait(waitTime);
          }
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /** Wait until the given id is unlocked. */
  public void waitForIdToUnlock(final VaultId id, final long timeout) {
    if (timeout <= 0) {
      throw new IllegalArgumentException("Must supply positive timeout argument");
    }
    final long startTime = System.currentTimeMillis();
    long leftToWait = timeout;
    while (leftToWait > 0 && !isUnlocked(id)) {
      synchronized (waitForLock) {
        if (isUnlocked(id)) {
          return;
        }
        try {
          waitForLock.wait(leftToWait);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        leftToWait = startTime + timeout - System.currentTimeMillis();
      }
    }
  }

  interface IRemoteVault extends IChannelSubscriber {
    @RemoteActionCode(0)
    void addLockedValue(VaultId id, byte[] data);

    @RemoteActionCode(2)
    void unlock(VaultId id, byte[] secretKeyBytes);

    @RemoteActionCode(1)
    void release(VaultId id);
  }
}
