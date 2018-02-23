package games.strategy.engine.vault;

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

import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.RemoteName;

/**
 * A vault is a secure way for the client and server to share information without
 * trusting each other.
 *
 * <p>
 * Data can be locked in the vault by a node. This data then is not readable by other nodes until the data is unlocked.
 * </p>
 *
 * <p>
 * When the data is unlocked by the original node, other nodes can read the data. When data is put in the vault, it cant
 * be changed by the
 * originating node.
 * </p>
 *
 * <p>
 * NOTE: to allow the data locked in the vault to be gc'd, the <code>release(VaultID id)</code> method
 * should be called when it is no longer needed.
 * </p>
 */
public class Vault {
  private static final RemoteName VAULT_CHANNEL =
      new RemoteName("games.strategy.engine.vault.IServerVault.VAULT_CHANNEL", IRemoteVault.class);
  private static final String ALGORITHM = "DES";
  private final SecretKeyFactory secretKeyFactory;
  // 0xCAFEBABE
  // we encrypt both this value and data when we encrypt data.
  // when decrypting we ensure that KNOWN_VAL is correct
  // and thus guarantee that we are being given the right key
  private static final byte[] KNOWN_VAL = new byte[] {0xC, 0xA, 0xF, 0xE, 0xB, 0xA, 0xB, 0xE};
  private final KeyGenerator keyGen;
  private final IChannelMessenger channelMessenger;
  // Maps VaultID -> SecretKey
  private final ConcurrentMap<VaultID, SecretKey> secretKeys = new ConcurrentHashMap<>();
  // maps ValutID -> encrypted byte[]
  private final ConcurrentMap<VaultID, byte[]> unverifiedValues = new ConcurrentHashMap<>();
  // maps VaultID -> byte[]
  private final ConcurrentMap<VaultID, byte[]> verifiedValues = new ConcurrentHashMap<>();
  private final Object waitForLock = new Object();

  /**
   * Creates a new instance of Vault.
   */
  public Vault(final IChannelMessenger channelMessenger) {
    this.channelMessenger = channelMessenger;
    this.channelMessenger.registerChannelSubscriber(remoteVault, VAULT_CHANNEL);
    try {
      secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
      keyGen = KeyGenerator.getInstance(ALGORITHM);
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException("Nothing known about algorithm:" + ALGORITHM, e);
    }
  }

  public void shutDown() {
    channelMessenger.unregisterChannelSubscriber(remoteVault, VAULT_CHANNEL);
  }

  // serialize secret key as byte array to
  // preserve jdk 1.4 to 1.5 compatability
  // they should be compatable, but we are
  // getting errors with serializing secret keys
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
    return (IRemoteVault) channelMessenger.getChannelBroadcastor(VAULT_CHANNEL);
  }

  /**
   * place data in the vault. An encrypted form of the data is sent at this
   * time to all nodes.
   *
   * <p>
   * The same key used to encrypt the KNOWN_VALUE so that nodes can verify the key when it is used to decrypt the data.
   * </p>
   *
   * @param data
   *        - the data to lock
   * @return the VaultId of the data
   */
  public VaultID lock(final byte[] data) {
    final VaultID id = new VaultID(channelMessenger.getLocalNode());
    final SecretKey key = keyGen.generateKey();
    if (secretKeys.putIfAbsent(id, key) != null) {
      throw new IllegalStateException("dupliagte id:" + id);
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
   * <p>
   * package access so we can test.
   * </p>
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
   * <p>
   * You can only unlock data that was locked by the same instance of the Vault
   * </p>
   *
   * @param id
   *        - the vault id to unlock
   */
  public void unlock(final VaultID id) {
    if (!id.getGeneratedOn().equals(channelMessenger.getLocalNode())) {
      throw new IllegalArgumentException("Cant unlock data that wasnt locked on this node");
    }
    final SecretKey key = secretKeys.remove(id);
    // let everyone unlock it
    getRemoteBroadcaster().unlock(id, secretKeyToBytes(key));
  }

  /**
   * Note - if an id has been released, then this will return false.
   * If this instance of vault locked id, then this method will return true
   * if the id has not been released.
   *
   * @return - has this id been unlocked
   */
  public boolean isUnlocked(final VaultID id) {
    return verifiedValues.containsKey(id);
  }

  /**
   * Get the unlocked data.
   */
  public byte[] get(final VaultID id) throws NotUnlockedException {
    if (verifiedValues.containsKey(id)) {
      return verifiedValues.get(id);
    } else if (unverifiedValues.containsKey(id)) {
      throw new NotUnlockedException();
    } else {
      throw new IllegalStateException("Nothing known about id:" + id);
    }
  }

  /**
   * Do we know about the given vault id.
   */
  public boolean knowsAbout(final VaultID id) {
    return verifiedValues.containsKey(id) || unverifiedValues.containsKey(id);
  }

  public List<VaultID> knownIds() {
    final ArrayList<VaultID> knownIds = new ArrayList<>(verifiedValues.keySet());
    knownIds.addAll(unverifiedValues.keySet());
    return knownIds;
  }

  /**
   * Allow all data associated with the given vault id to be released and garbage collected
   *
   * <p>
   * An id can be released by any node.
   * </p>
   *
   * <p>
   * If the id has already been released, then nothing will happen.
   * </p>
   */
  public void release(final VaultID id) {
    getRemoteBroadcaster().release(id);
  }

  private final IRemoteVault remoteVault = new IRemoteVault() {
    @Override
    public void addLockedValue(final VaultID id, final byte[] data) {
      if (id.getGeneratedOn().equals(channelMessenger.getLocalNode())) {
        return;
      }
      if (unverifiedValues.putIfAbsent(id, data) != null) {
        throw new IllegalStateException("duplicate values for id:" + id);
      }
      synchronized (waitForLock) {
        waitForLock.notifyAll();
      }
    }

    @Override
    public void unlock(final VaultID id, final byte[] secretKeyBytes) {
      if (id.getGeneratedOn().equals(channelMessenger.getLocalNode())) {
        return;
      }
      final SecretKey key = bytesToKey(secretKeyBytes);
      final Cipher cipher;
      try {
        cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
      } catch (final NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException e) {
        throw new IllegalStateException(e);
      }
      final byte[] encrypted = unverifiedValues.remove(id);
      final byte[] decrypted;
      try {
        decrypted = cipher.doFinal(encrypted);
      } catch (final Exception e1) {
        e1.printStackTrace();
        throw new IllegalStateException(e1.getMessage());
      }
      if (decrypted.length < KNOWN_VAL.length) {
        throw new IllegalStateException("decrypted is not long enough to have known value, cheating is suspected");
      }
      // check that the known value is correct
      // we use the known value to check that the key given to
      // us was the key used to encrypt the value in the first place
      for (int i = 0; i < KNOWN_VAL.length; i++) {
        if (KNOWN_VAL[i] != decrypted[i]) {
          throw new IllegalStateException("Known value of cipher not correct, cheating is suspected");
        }
      }
      final byte[] data = new byte[decrypted.length - KNOWN_VAL.length];
      System.arraycopy(decrypted, KNOWN_VAL.length, data, 0, data.length);
      if (verifiedValues.putIfAbsent(id, data) != null) {
        throw new IllegalStateException("duplicate values for id:" + id);
      }
      synchronized (waitForLock) {
        waitForLock.notifyAll();
      }
    }

    @Override
    public void release(final VaultID id) {
      unverifiedValues.remove(id);
      verifiedValues.remove(id);
    }
  };

  /**
   * Waits until we know about a given vault id.
   * waits for at most timeout milliseconds
   */
  public void waitForId(final VaultID id, final long timeoutMs) {
    if (timeoutMs <= 0) {
      throw new IllegalArgumentException("Must suppply positive timeout argument");
    }
    final long endTime = timeoutMs + System.currentTimeMillis();
    while ((System.currentTimeMillis() < endTime) && !knowsAbout(id)) {
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

  /**
   * Wait until the given id is unlocked.
   */
  public void waitForIdToUnlock(final VaultID id, final long timeout) {
    if (timeout <= 0) {
      throw new IllegalArgumentException("Must suppply positive timeout argument");
    }
    final long startTime = System.currentTimeMillis();
    long leftToWait = timeout;
    while ((leftToWait > 0) && !isUnlocked(id)) {
      synchronized (waitForLock) {
        if (isUnlocked(id)) {
          return;
        }
        try {
          waitForLock.wait(leftToWait);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        leftToWait = (startTime + timeout) - System.currentTimeMillis();
      }
    }
  }

  interface IRemoteVault extends IChannelSubscribor {
    void addLockedValue(VaultID id, byte[] data);

    void unlock(VaultID id, byte[] secretKeyBytes);

    void release(VaultID id);
  }
}

