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
package games.strategy.engine.vault;

import games.strategy.engine.message.*;
import games.strategy.net.INode;

import java.security.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;

/**
 * A vault is a secure way for the client and server to share information without 
 * trusting each other.<p>
 * 
 * Data can be locked in the vault by a node.  This data then is not readable
 * by other nodes until the data is unlocked.<p>
 * 
 * When the data is unlocked by the original node, other nodes can read the data.  
 * When data is put in the vault, it cant be changed by the originating node.<p>
 * 
 * NOTE: to allow the data locked in the vault to be gc'd, the <code>release(VaultID id)<code> method 
 * should be called when it is no longer needed.
 * 
 * 
 * @author Sean Bridges
 */
public class Vault implements IServerVault
{
    private static final String VAULT_CHANNEL = "games.strategy.engine.vault.IServerVault.VAULT_CHANNEL";
    
    private static final String ALGORITHM = "DES";

    private SecretKeyFactory mSecretKeyFactory;
    
    //0xCAFEBABE
    //we encrypt both this value and data when we encrypt data.
    //when decrypting we ensure that KNOWN_VAL is correct
    //and thus guarantee that we are being given the right key
    private static final byte[] KNOWN_VAL = new byte[] {0xC, 0xA, 0xF, 0xE, 0xB, 0xA, 0xB, 0xE };
    
    private final KeyGenerator m_keyGen;
    private final IChannelMessenger m_channelMessenger;
    
    //Maps VaultID -> SecretKey
    private final Map<VaultID, SecretKey> m_secretKeys = Collections.synchronizedMap(new HashMap<VaultID, SecretKey>());

    //maps ValutID -> encrypted byte[]
    private final Map<VaultID, byte[]> m_unverifiedValues = Collections.synchronizedMap(new HashMap<VaultID, byte[]>());
    //maps VaultID -> byte[]
    private final Map<VaultID, byte[]> m_verifiedValues = Collections.synchronizedMap(new HashMap<VaultID, byte[]>());
    //maps INode->Long
    //these are the ids of the remote keys that we have seen
    //dont allow values to be reused
    private final Map<INode, Long> m_maxRemoteIDIndexes = Collections.synchronizedMap(new HashMap<INode, Long>());
    
    private final Object m_waitForLock = new Object();
    
    private static final String VAULT_SERVER_REMOTE = "games.strategy.engine.vault.IServerVault.VAULT_SERVER_REMOTE";
    
    /**
     * @param channelMessenger
     */
    public Vault(final IChannelMessenger channelMessenger, IRemoteMessenger remoteMessenger)
    {
        m_channelMessenger = channelMessenger;
        
        if(!m_channelMessenger.hasChannel(VAULT_CHANNEL))
        {
            m_channelMessenger.createChannel(IRemoteVault.class, VAULT_CHANNEL);
        }
        m_channelMessenger.registerChannelSubscriber(m_remoteVault, VAULT_CHANNEL);
        
        handleInit(remoteMessenger);
        
        try
        {
            mSecretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            m_keyGen = KeyGenerator.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            throw new IllegalStateException("Nothing known about algorithm:" +  ALGORITHM);
        }
    }
    
    //serialize secret key as byte array to 
    //preserve jdk 1.4 to 1.5 compatability
    //they should be compatable, but we are 
    //getting errors with serializing secret keys
    private SecretKey bytesToKey(byte[] bytes)
    {
        try
        {
            DESKeySpec spec = new DESKeySpec(bytes);
            return mSecretKeyFactory.generateSecret(spec);
        } catch (GeneralSecurityException e)
        {
            throw new IllegalStateException(e.getMessage());
        }
    }
    
    private byte[] secretKeyToBytes(SecretKey key)
    {
        DESKeySpec ks;
        try
        {
            ks = (DESKeySpec) mSecretKeyFactory.getKeySpec(key, DESKeySpec.class);
            return  ks.getKey(); 
        } catch (GeneralSecurityException e)
        {
            throw new IllegalStateException(e.getMessage());
        }
       
    }
    
    /**
     * @param remoteMessenger
     */
    private void handleInit(IRemoteMessenger remoteMessenger)
    {
        if(m_channelMessenger.isServer())
        {
            //we init others
            remoteMessenger.registerRemote(IServerVault.class, this, VAULT_SERVER_REMOTE);
        }
        else
        {
            remoteMessenger.waitForRemote(VAULT_SERVER_REMOTE, 5000);
            //we need to be initialized
            IServerVault server = (IServerVault) remoteMessenger.getRemote(VAULT_SERVER_REMOTE);
            Map<VaultID, byte[]> [] initValues = server.getInitData();
            m_verifiedValues.putAll(initValues[0]);
            m_unverifiedValues.putAll(initValues[1]);
        }
    }

    private IRemoteVault getRemoteBroadcaster()
    {
        return (IRemoteVault) m_channelMessenger.getChannelBroadcastor(VAULT_CHANNEL);
    }
    
    /**
     * place data in the vault.  An encrypted form of the data is sent at this
     * time to all nodes.  <p>
     * The same key used to encrypt the KNOWN_VALUE so that nodes can verify
     * the key when it is used to decrypt the data.  
     * 
     * @param data - the data to lock
     * @return the VaultId of the data
     */
    //this method is synchronized since vault ids must arrive in order
    public synchronized VaultID lock(byte[] data)
    {
        VaultID id = new VaultID(m_channelMessenger.getLocalNode());
        SecretKey key = m_keyGen.generateKey();
        m_secretKeys.put(id, key);
        //we already know it, so might as well keep it
        m_verifiedValues.put(id, data);
        
        Cipher cipher;
        try
        {
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);            
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        } catch (NoSuchPaddingException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }catch (InvalidKeyException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }     
        
        //join the data and known value into one array
        byte[] dataAndCheck = joinDataAndKnown(data);
        
        byte[] encrypted;
        try
        {
            encrypted = cipher.doFinal(dataAndCheck);
        } catch (Exception e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
        
        //tell the world
        getRemoteBroadcaster().addLockedValue(id, encrypted);
        
        return id;
    }
        
    
    /**
     * Join known and data into one array.<p>
     * package access so we can test.
     */
    static byte[] joinDataAndKnown(byte[] data)
    {
        byte[] dataAndCheck = new byte[KNOWN_VAL.length + data.length];
        System.arraycopy(KNOWN_VAL, 0,  dataAndCheck, 0, KNOWN_VAL.length);
        System.arraycopy(data, 0,  dataAndCheck, KNOWN_VAL.length, data.length);
        return dataAndCheck;
    }

    /**
     * allow other nodes to see the data.<p>
     * 
     * You can only unlock data that was locked by the same instance of the Vault
     * 
     * @param id - the vault id to unlock
     */
    public void unlock(VaultID id)
    {
        if(!id.getGeneratedOn().equals(m_channelMessenger.getLocalNode()))
        {
            throw new IllegalArgumentException("Cant unlock data that wasnt locked on this node");
        }
        SecretKey key = m_secretKeys.get(id);
        //allow the secret key to be gc'd
        m_secretKeys.remove(key);
    
        //let everyone unlock it
        getRemoteBroadcaster().unlock(id, secretKeyToBytes( key));
    }
    
    /**
     * Note - if an id has been released, then this will return false.
     * If this instance of vault locked id, then this method will return true
     * if the id has not been released.
     * 
     * @return - has this id been unlocked
     */
    public boolean isUnlocked(VaultID id)
    {
      return m_verifiedValues.containsKey(id);   
    }
     
    /**
     * Get the unlocked data.
     *  
     */
    public byte[] get(VaultID id) throws NotUnlockedException
    {
        if(m_verifiedValues.containsKey(id))
            return m_verifiedValues.get(id);
        else if(m_unverifiedValues.containsKey(id))
        	throw new NotUnlockedException();
        else 
            throw new IllegalStateException("Nothing known about id:" + id);
    }
    
    /**
     * Do we know about the given vault id. 
     */
    public boolean knowsAbout(VaultID id)
    {
        return m_verifiedValues.containsKey(id) || m_unverifiedValues.containsKey(id);
    }
    
    
    /**
     * Allow all data associated with the given vault id to be released and garbage collected<p>
     * An id can be released by any node.<p>
     * If the id has already been released, then nothing will happen. 
     * 
     */
    public void release(VaultID id)
    {
        getRemoteBroadcaster().release(id);
    }
    
    private final IRemoteVault m_remoteVault = new IRemoteVault()
    {
        public void addLockedValue(VaultID id, byte[] data)
        {
            if(id.getGeneratedOn().equals(m_channelMessenger.getLocalNode()))
                return;
            
            //we only want to put something in the vault once!
            //if a remote node can lock the same vaultid more than once
            //then we have no security.
            //we rely on the monotone increasing value of ids
            Long lastID = m_maxRemoteIDIndexes.get(id.getGeneratedOn());
            if(lastID == null)
                lastID = new Long(-1);
            
            if(id.getUniqueID() <= lastID.longValue())
                throw new IllegalStateException("Attempt was made to reuse a vaultID, cheating is suspected");
                
            m_maxRemoteIDIndexes.put(id.getGeneratedOn(), new Long(id.getUniqueID()));
            
            m_unverifiedValues.put(id, data);
            
            synchronized(m_waitForLock)
            {
                m_waitForLock.notifyAll();
            }
            
        }

        public void unlock(VaultID id, byte[] secretKeyBytes)
        {
            if(id.getGeneratedOn().equals(m_channelMessenger.getLocalNode()))
                return;
            
            SecretKey key = bytesToKey(secretKeyBytes);
            
            Cipher cipher;
            try
            {
                cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key);            
            } catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            } catch (NoSuchPaddingException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }catch (InvalidKeyException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }     
            
            byte[] encrypted = m_unverifiedValues.get(id);
            //allow it to be gcd
            m_unverifiedValues.remove(id);
            byte[] decrypted;
            
            try
            {
                decrypted = cipher.doFinal(encrypted);
            } catch (Exception e1)
            {
                e1.printStackTrace();
                throw new IllegalStateException(e1.getMessage());
            }
            
            if(decrypted.length < KNOWN_VAL.length)
                throw new IllegalStateException("decrypted is not long enough to have known value, cheating is suspected");
            
            //check that the known value is correct
            //we use the known value to check that the key given to
            //us was the key used to encrypt the value in the first place
            for(int i = 0; i < KNOWN_VAL.length; i++)
            {
                if( KNOWN_VAL[i] !=  decrypted[i])
                    throw new IllegalStateException("Known value of cipher not correct, cheating is suspected");
            }
            
            byte[] data = new byte[decrypted.length - KNOWN_VAL.length];
            System.arraycopy(decrypted, KNOWN_VAL.length, data, 0, data.length);
            
            m_verifiedValues.put(id, data);
            
            synchronized(m_waitForLock)
            {
                m_waitForLock.notifyAll();
            }
     
        }

        public void release(VaultID id)
        {
            m_unverifiedValues.remove(id);
            m_verifiedValues.remove(id);
        }
    };
    
    /**
     * Waits until we know about a given vault id.
     * waits for at most timeout milliseconds 
     */
    public void waitForID(VaultID id, long timeoutMS)
    {
        if(timeoutMS <= 0)
            throw new IllegalArgumentException("Must suppply positive timeout argument");
        
        long endTime = timeoutMS + System.currentTimeMillis();
        
        while(System.currentTimeMillis() < endTime && !knowsAbout(id))
        {
            synchronized(m_waitForLock)
            {
                if(knowsAbout(id))
                    return;
                try
                {
                    m_waitForLock.wait(endTime - System.currentTimeMillis());
                } catch (InterruptedException e)
                {
                    //not a big deal
                }                
        }
      } 
               
    }
    
    /**
     * Wait until the given id is unlocked
     */
    public void waitForIdToUnlock(VaultID id, long timeout)
    {
        if(timeout <= 0)
            throw new IllegalArgumentException("Must suppply positive timeout argument");
        
        long startTime = System.currentTimeMillis();
        long leftToWait = timeout;
        
        while(leftToWait > 0 && !isUnlocked(id))
        {
            synchronized(m_waitForLock)
            {
                if(isUnlocked(id))
                    return;
                try
                {
                    m_waitForLock.wait(leftToWait);
                } catch (InterruptedException e)
                {
                    //not a big deal
                }                
                leftToWait =  startTime + timeout - System.currentTimeMillis(); 
        }
      }        
    }

    /* (non-Javadoc)
     * @see games.strategy.engine.vault.IServerVault#getInitData()
     */
    @SuppressWarnings("unchecked")
    public Map<VaultID, byte[]> [] getInitData()
    {
        Map<VaultID, byte[]> [] rVal =  new Map[]{m_verifiedValues, m_unverifiedValues};
        return rVal;
         
    }
    
}


interface IRemoteVault extends IChannelSubscribor
{
   public void addLockedValue(VaultID id, byte[] data);
   public void unlock(VaultID id, byte[] secretKeyBytes);
   public void release(VaultID id);
}

interface IServerVault extends IRemote
{
    /**
     * 
     * @return [0] - verified
     * @return [1] - unverified
     */
    public Map<VaultID, byte[]> [] getInitData();
}