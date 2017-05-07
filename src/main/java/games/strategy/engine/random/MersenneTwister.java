package games.strategy.engine.random;

import java.util.Random;

/**
 * <h3>MersenneTwister and MersenneTwisterFast</h3>
 *
 * <p>
 * Version 9, based on version MT199937(99/10/29) of the Mersenne Twister algorithm found at
 * <a href="http://www.math.keio.ac.jp/matumoto/emt.html"> The Mersenne Twister Home Page</a>, with the initialization
 * improved using the new 2002/1/26 initialization algorithm By Sean Luke, October 2004.
 * </p>
 *
 * <p>
 * MersenneTwister is a drop-in subclass replacement for java.tools.Random. It is properly synchronized and can be
 * used in a multithreaded environment. On modern VMs such as HotSpot, it is approximately 1/3 slower than
 * java.tools.Random.
 * </p>
 *
 * <p>
 * MersenneTwisterFast is not a subclass of java.tools.Random. It has the same public methods as Random does, however,
 * and it is
 * algorithmically identical to MersenneTwister. MersenneTwisterFast has hard-code inlined all of its methods directly,
 * and made all of them
 * final (well, the ones of consequence anyway). Further, these methods are <i>not</i> synchronized, so the same
 * </p>
 *
 * <p>
 * MersenneTwisterFast
 * instance cannot be shared by multiple threads. But all this helps MersenneTwisterFast achieve well over twice the
 * speed of
 * MersenneTwister. java.tools.Random is about 1/3 slower than MersenneTwisterFast.
 * </p>
 *
 * <h3>About the Mersenne Twister</h3>
 *
 * <p>
 * This is a Java version of the C-program for MT19937: Integer version. The MT19937 algorithm was created by Makoto
 * Matsumoto and Takuji
 * Nishimura, who ask:
 * "When you use this, send an email to: matumoto@math.keio.ac.jp with an appropriate reference to your work". Indicate
 * that this is a translation of their algorithm into Java.
 * </p>
 *
 * <p>
 * <b>Reference. </b> Makato Matsumoto and Takuji Nishimura,
 * "Mersenne Twister: A 623-Dimensionally Equidistributed Uniform Pseudo-Random Number Generator", <i>ACM Transactions
 * on Modeling and Computer Simulation,</i> Vol. 8, No. 1, January 1998, pp 3--30.
 * </p>
 *
 * <p>
 * The MersenneTwister code is based on standard MT19937 C/C++ code by Takuji Nishimura, with suggestions from Topher
 * Cooper and Marc Rieffel, July 1997. The code was originally translated into Java by Michael Lecuyer, January 1999,
 * and the original code is Copyright (c) 1999 by Michael Lecuyer.
 * </p>
 */
public class MersenneTwister extends Random {
  private static final long serialVersionUID = -6946159560323874784L;
  // Period parameters
  private static final int N = 624;
  private static final int M = 397;
  private static final int MATRIX_A = 0x9908b0df;
  // most significant w-r bits
  private static final int UPPER_MASK = 0x80000000;
  // least significant r bits
  private static final int LOWER_MASK = 0x7fffffff;
  // Tempering parameters
  private static final int TEMPERING_MASK_B = 0x9d2c5680;
  private static final int TEMPERING_MASK_C = 0xefc60000;
  // the array for the state vector
  private int[] m_mt;
  // mti==N+1 means mt[N] is not initialized
  private int mti;
  private int[] mag01;
  /*
   * implemented here because there's a bug in Random's implementation
   * of the Gaussian code (divide by zero, and log(0), ugh!), yet its
   * gaussian variables are private so we can't access them here. :-(
   */
  private double nextNextGaussian;
  private boolean haveNextNextGaussian;

  /**
   * Constructor using the default seed.
   */
  public MersenneTwister() {
    this(System.currentTimeMillis());
  }

  /**
   * Constructor using a given seed. Though you pass this seed in
   * as a long, it's best to make sure it's actually an integer.
   */
  public MersenneTwister(final long seed) {
    super(seed); /* just in case */
    setSeed(seed);
  }

  /**
   * Constructor using an array.
   */
  public MersenneTwister(final int[] array) {
    super(System.currentTimeMillis()); /* pick something at random just in case */
    setSeed(array);
  }

  /**
   * Initalize the pseudo random number generator. Don't
   * pass in a long that's bigger than an int (Mersenne Twister
   * only uses the first 32 bits for its seed).
   */
  @Override
  public synchronized void setSeed(final long seed) {
    // it's always good style to call super
    super.setSeed(seed);
    // Due to a bug in java.tools.Random clear up to 1.2, we're
    // doing our own Gaussian variable.
    haveNextNextGaussian = false;
    m_mt = new int[N];
    mag01 = new int[2];
    mag01[0] = 0x0;
    mag01[1] = MATRIX_A;
    m_mt[0] = (int) (seed & 0xffffffff);
    for (mti = 1; mti < N; mti++) {
      m_mt[mti] = (1812433253 * (m_mt[mti - 1] ^ (m_mt[mti - 1] >>> 30)) + mti);
      /* See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier. */
      /* In the previous versions, MSBs of the seed affect */
      /* only MSBs of the array mt[]. */
      /* 2002/01/09 modified by Makoto Matsumoto */
      m_mt[mti] &= 0xffffffff;
      /* for >32 bit machines */
    }
  }

  /**
   * An alternative, more complete, method of seeding the
   * pseudo random number generator. array must be an
   * array of 624 ints, and they can be any value as long as
   * they're not *all* zero.
   */
  public synchronized void setSeed(final int[] array) {
    setSeed(19650218);
    int i = 1;
    int j = 0;
    int k = (N > array.length ? N : array.length);
    for (; k != 0; k--) {
      m_mt[i] = (m_mt[i] ^ ((m_mt[i - 1] ^ (m_mt[i - 1] >>> 30)) * 1664525)) + array[j] + j; /* non linear */
      m_mt[i] &= 0xffffffff; /* for WORDSIZE > 32 machines */
      i++;
      j++;
      if (i >= N) {
        m_mt[0] = m_mt[N - 1];
        i = 1;
      }
      if (j >= array.length) {
        j = 0;
      }
    }
    for (k = N - 1; k != 0; k--) {
      m_mt[i] = (m_mt[i] ^ ((m_mt[i - 1] ^ (m_mt[i - 1] >>> 30)) * 1566083941)) - i; /* non linear */
      m_mt[i] &= 0xffffffff; /* for WORDSIZE > 32 machines */
      i++;
      if (i >= N) {
        m_mt[0] = m_mt[N - 1];
        i = 1;
      }
    }
    m_mt[0] = 0x80000000; /* MSB is 1; assuring non-zero initial array */
  }

  /**
   * Returns an integer with <i>bits</i> bits filled with a random number.
   */
  @Override
  protected synchronized int next(final int bits) {
    int y;
    if (mti >= N) { // generate N words at one time
      int kk;
      // locals are slightly faster
      final int[] mt = this.m_mt;
      // locals are slightly faster
      final int[] mag01 = this.mag01;
      for (kk = 0; kk < N - M; kk++) {
        y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
        mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
      }
      for (; kk < N - 1; kk++) {
        y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
        mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
      }
      y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
      mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];
      mti = 0;
    }
    y = m_mt[mti++];
    // TEMPERING_SHIFT_U(y)
    y ^= y >>> 11;
    // TEMPERING_SHIFT_S(y)
    y ^= (y << 7) & TEMPERING_MASK_B;
    // TEMPERING_SHIFT_T(y)
    y ^= (y << 15) & TEMPERING_MASK_C;
    // TEMPERING_SHIFT_L(y)
    y ^= (y >>> 18);
    // hope that's right!
    return y >>> (32 - bits);
  }


  /**
   * This method is missing from jdk 1.0.x and below. JDK 1.1
   * includes this for us, but what the heck.
   */
  @Override
  public boolean nextBoolean() {
    return next(1) != 0;
  }


  /**
   * This method is missing from JDK 1.1 and below. JDK 1.2
   * includes this for us, but what the heck.
   */
  @Override
  public int nextInt(final int n) {
    if (n <= 0) {
      throw new IllegalArgumentException("n must be >= 0");
    }
    if ((n & -n) == n) {
      return (int) ((n * (long) next(31)) >> 31);
    }
    int bits;
    int val;
    do {
      bits = next(31);
      val = bits % n;
    } while (bits - val + (n - 1) < 0);
    return val;
  }

  /**
   * A bug fix for versions of JDK 1.1 and below. JDK 1.2 fixes
   * this for us, but what the heck.
   */
  @Override
  public double nextDouble() {
    return (((long) next(26) << 27) + next(27)) / (double) (1L << 53);
  }

  /**
   * A bug fix for versions of JDK 1.1 and below. JDK 1.2 fixes
   * this for us, but what the heck.
   */
  @Override
  public float nextFloat() {
    return next(24) / ((float) (1 << 24));
  }

  /**
   * A bug fix for all versions of the JDK. The JDK appears to
   * use all four bytes in an integer as independent byte values!
   * Totally wrong. I've submitted a bug report.
   */
  @Override
  public void nextBytes(final byte[] bytes) {
    for (int x = 0; x < bytes.length; x++) {
      bytes[x] = (byte) next(8);
    }
  }


  /**
   * A bug fix for all JDK code including 1.2. nextGaussian can theoretically
   * ask for the log of 0 and divide it by 0! See Java bug
   * <a href="http://developer.java.sun.com/developer/bugParade/bugs/4254501.html">
   * http://developer.java.sun.com/developer/bugParade/bugs/4254501.html</a>
   */
  @Override
  public synchronized double nextGaussian() {
    if (haveNextNextGaussian) {
      haveNextNextGaussian = false;
      return nextNextGaussian;
    } else {
      double v1;
      double v2;
      double s;
      do {
        // between -1.0 and 1.0
        v1 = 2 * nextDouble() - 1;
        // between -1.0 and 1.0
        v2 = 2 * nextDouble() - 1;
        s = v1 * v1 + v2 * v2;
      } while (s >= 1 || s == 0);
      final double multiplier = /* Strict */Math.sqrt(-2 * /* Strict */Math.log(s) / s);
      nextNextGaussian = v2 * multiplier;
      haveNextNextGaussian = true;
      return v1 * multiplier;
    }
  }
}
