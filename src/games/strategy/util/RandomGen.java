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

/*
 * RandomGen.java
 *
 * Created on March 11, 2003, 12:15 AM
 * $Id$
 */

package games.strategy.util;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.security.SecureRandom;
import javax.crypto.*;
import games.strategy.engine.random.*;


/**
 * Usage:
 * ---------------------------------------------------------------
 * | Player A                     | Player B                     |
 * ---------------------------------------------------------------
 * | Receives MaxRandomSize       |                              |
 * | Generates Triplet, Key       |                              |
 * | Returns Triplet              |                              |
 * ---------------------------------------------------------------
 * |                              | Receives Triplet             |
 * |                              | Generates Triplet, Key       |
 * |                              | Returns Triplet              |
 * ---------------------------------------------------------------
 * | Receives Triplet             |                              |
 * | Returns Key                  |                              |
 * ---------------------------------------------------------------
 * |                              | Receives Key                 |
 * |                              | Verifies known value         |
 * |                              | Decodes random value         |
 * |                              | Generates shared random      |
 * |                              | Returns Key                  |
 * ---------------------------------------------------------------
 * | Receives Key                 |                              |
 * | Verifies known value         |                              |
 * | Decodes random value         |                              |
 * | Generates shared random      |                              |
 * | Returns shared random        |                              |
 * ---------------------------------------------------------------
 */
public class RandomGen
{
  private static final String ALGORITHM = "DES";
  private static final String KNOWN_VAL = "Random Gen known value.  This is that and nothing else";





  // These are used to do the work of generating
  // and working with the encrypted random numbers
  
  private static MersenneTwister s_seed_gen = new MersenneTwister(PlainRandomSource.getSeed());

  private Cipher m_cryptor;
  private KeyGenerator m_keygen;

  // These are the values that are shared with outside entities
  private SecretKey m_key;

  private Integer m_max_num;
  private Long m_random_seed;

  private byte[] m_enc_random_seed;
  private byte[] m_enc_known;
  
  private final String m_annotation;
  private final int m_randomCount;

  public RandomGen(RandomTriplet triplet)
  {
    // DEBUG
    // System.out.println("RandomGen()");
    m_max_num = null;
    m_random_seed = null;
    
    setEncryptedRandomSeed(triplet.m_encrypted_random);
    setEncryptedKnown(triplet.m_encrypted_known);
    setMaxVal(triplet.m_max_num);
    m_randomCount = triplet.getRandomCount();
    m_annotation = triplet.getAnnotation();
 
    
  }

  public RandomGen(int in_max_num, int randomCount, String annotation)
  {
    if(randomCount < 0)
      throw new IllegalArgumentException("Random count must be >=0. not :" + randomCount);

      // DEBUG
    // System.out.println("RandomGen(" + in_max_num + ")");
    m_max_num = new Integer(in_max_num);
    m_random_seed = null;
    m_randomCount = randomCount;
    m_annotation = annotation;
  }


  public int[] getSharedRandomArr(RandomGen other)
  {
      
      if(m_randomCount == -1)
          throw new IllegalStateException("Count not set");
    // DEBUG
    //System.out.println(this + "#getSharedRandom(" + other + ")");
    //System.out.println(this + "#getSharedRandom() seed " +
    //                   getRandomSeed() + " ^ " + other.getRandomSeed() + " => " +
    //                    (getRandomSeed() ^ other.getRandomSeed()));

    MersenneTwister rng = new MersenneTwister(getRandomSeed() ^ other.getRandomSeed());
    int[] rnds = new int[m_randomCount];

    if (null == m_max_num)
    {
      for (int i=0; i<m_randomCount; i++)
      {
        rnds[i] = rng.nextInt();
      }
    }
    else
    {
      for (int i=0; i<m_randomCount; i++)
      {
        rnds[i] = rng.nextInt(m_max_num.intValue());
      }
    }

    return rnds;
  }


  /**
   * Creates the random number and the encryption key
   */
  private void createRandom()
  {
    // DEBUG
    // System.out.println(this + "#createRandom()");
    try {
      m_keygen = KeyGenerator.getInstance(ALGORITHM);

      m_key = m_keygen.generateKey();

      m_random_seed = new Long(s_seed_gen.nextLong());

      m_cryptor = Cipher.getInstance(ALGORITHM);
      m_cryptor.init(Cipher.ENCRYPT_MODE, m_key);
    }
    catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException("Bad algorithm: " + ALGORITHM);
    }
    catch (GeneralSecurityException ex) {
      throw new RuntimeException(ex.getMessage());
    }
  }

  
  /**
   * Gets the encrypted random number, the encrypted known number
   * and the max random value
   */
  public RandomTriplet getTriplet()
  {
    // DEBUG
    // System.out.println(this + "#getTriplet()");
    return new RandomTriplet(getEncryptedRandomSeed(),
                             getEncryptedKnown(),
                             getMaxVal(),
                             m_randomCount,
                             m_annotation
                             );
  }



  /**
   * Gets the random number.  If it doesn't yet it either creates
   * it or decrypts it, as appropriate.
   */
  public long getRandomSeed()
  {
    // DEBUG
    // System.out.println(this + "#getRandomSeed()");
    if (null == m_random_seed)
    {
      if ((null != m_enc_random_seed) && (null != m_key))
      {
        // Here we must be trying to get a random number by
        // decrypting the remote side's random number
        try {
          m_cryptor = Cipher.getInstance(ALGORITHM);
          m_cryptor.init(Cipher.DECRYPT_MODE, m_key);


          // DEBUG
          //System.out.println("About to decrypt this random: ");
          StringBuffer buff = new StringBuffer();
          for (int i=0; i<m_enc_random_seed.length; i++)
          {
            buff.append(Integer.toHexString((int)m_enc_random_seed[i]));
          }
          buff.append("', ");
          //System.out.println(buff.toString());
          //System.out.println("Using this key");
          //System.out.println(m_key);



          m_random_seed = new Long(byteArrToLong(m_cryptor.doFinal(m_enc_random_seed)));
        }
        catch (GeneralSecurityException ex) {
          throw new RuntimeException(ex.getMessage());
        }
      }
      else
      {
        // Here we must be
        createRandom();
      }
    }

    return m_random_seed.longValue();
  }

  /**
   * Sets the maximum value for the random number generator to use.
   */
  public void setMaxVal(int max)
  {
    // DEBUG
    // System.out.println(this + "#setMaxVal(" + max + ")");
    setMaxVal(new Integer(max));
  }

  /**
   * Sets the maximum value for the random number generator to use.
   */
  public void setMaxVal(Integer max)
  {
    // DEBUG
    // System.out.println(this + "#setMaxVal(" + max + ")");
    m_max_num = max;
  }

  /**
   * Returns the maximum value random numbers can have, or null
   * if there is no maximum set.
   */
  public Integer getMaxVal()
  {
    // DEBUG
    // System.out.println(this + "#getMaxVal()");
    return m_max_num;
  }

  /**
   * Sets the encrypted known, likely generated by the other side.
   */
  public void setEncryptedKnown(byte[] in_enc_known)
  {
    // DEBUG
    // System.out.println(this + "#setEncryptedKnown(...)");
    m_enc_known = in_enc_known;
  }

  /**
   * Sets the encrypted random, likely generated by the other side.
   */
  public void setEncryptedRandomSeed(byte[] in_enc_seed)
  {
    // DEBUG
    // System.out.println(this + "#setEncryptedRandomSeed(...)");
    m_enc_random_seed = in_enc_seed;
  }

  /**
   * Gets the encrypted random number, either the one set
   * explicitly or by encrypting one generated automatically.
   */
  public byte[] getEncryptedRandomSeed()
  {
    // DEBUG
    // System.out.println(this + "#getEncryptedRandomSeed()");
    if (null == m_enc_random_seed) {
      if (null == m_random_seed)
      {
        createRandom();
      }

      m_enc_random_seed = null;

      try {
        m_enc_random_seed = m_cryptor.doFinal(longToByteArr(m_random_seed.longValue()));


        // DEBUG
        // StringBuffer buff = new StringBuffer("Just created this random: '");
        // for (int i=0; i<m_enc_random_seed.length; i++)
        // {
        //   buff.append(Integer.toHexString((int)m_enc_random_seed[i]));
        // }
        // buff.append("'.");
        // System.out.println(buff.toString());
        // System.out.println("Using this key: " + m_key);

      }
      catch (GeneralSecurityException ex) 
      {
          ex.printStackTrace();
      }
    }

    return m_enc_random_seed;
  }

  /**
   * Gets the known value encrypted by the appropriate key.
   */
  public byte[] getEncryptedKnown()
  {
    // DEBUG
    // System.out.println(this + "#getEncryptedKnown()");
    if (null == m_enc_known) {
      if (null == m_random_seed)
      {
        createRandom();
      }

      byte[] ascii_known =
        {
          0x62, 0x75, 0x67
        };

      m_enc_known = new byte[0];

      try {
        ascii_known = KNOWN_VAL.getBytes("US-ASCII");
        m_enc_known = m_cryptor.doFinal(ascii_known);
      }
      catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex.getMessage());
      }
      catch (GeneralSecurityException ex) {
        throw new RuntimeException(ex.getMessage());
      }
    }

    return m_enc_known;
  }


  public boolean verifyKnown()
  {
    // DEBUG
    // System.out.println(this + "#verifyKnown()");
    if ((null != m_enc_known) && (null != m_key))
    {
      // Here we must be trying to get a random number by
      // decrypting the remote side's random number
      String known = "BUG";
      try {
        m_cryptor = Cipher.getInstance(ALGORITHM);
        m_cryptor.init(Cipher.DECRYPT_MODE, m_key);
        known = new String(m_cryptor.doFinal(m_enc_known));
      }
      catch (GeneralSecurityException ex) {
        throw new RuntimeException(ex.getMessage());
      }

      return known.equals(KNOWN_VAL);
    }
    else
    {
      // Here we must be
      // DEBUG
      // System.out.println("We should fix this");
      return false;
    }
  }

  /**
   * Sets the key to use for encryption / decryption
   */
  public void setKey(SecretKey in_key)
  {
    // DEBUG
    // System.out.println(this + "#setKey(" + in_key + ")");
    m_key = in_key;
  }

  /**
   * Gets the key used for encryption / decryption, creating
   * it if necessary
   */
  public SecretKey getKey()
  {
    // DEBUG
    // System.out.println(this + "#getKey()");
    if (null == m_key)
    {
      createRandom();
    }

    return m_key;
  }

  public static byte[] longToByteArr(long num)
  {
      return ("" + num).getBytes();
  }


  public static long byteArrToLong(byte[] arr)
  {
    return Long.parseLong(new String(arr));
  }


  public String getAnnotation()
  {
      return m_annotation;
  }
 
}
