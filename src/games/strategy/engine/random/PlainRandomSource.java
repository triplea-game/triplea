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

import games.strategy.util.IntegerMap;

import java.util.*;

/**
 * Gets random numbers from javas random number generators.
 */

public class PlainRandomSource implements IRandomSource
{

  /**
     Knowing the seed gives a player an advantage.
     Do something a little more clever than current time.
     which could potentially be guessed
   
     If the execution path is different before the first random
     call is made then the object will have a somewhat random
     adress in the virtual machine, especially if
     a lot of ui and networking objects are created
     in response to semi random mouse motion etc.
     if the excecution is always the same then
     this may vary depending on the vm
   
   */
  public static long getSeed()
  {
    
    Object seedObj = new Object();
    long seed = seedObj.hashCode();     //hash code is an int, 32 bits

    seed += System.currentTimeMillis(); //seed with current time as well
    return seed;
  }


  //private static Random s_random;
    private static MersenneTwister s_random;

  public synchronized int[] getRandom(int max, int count, String annotation)
  {
    int[] numbers = new int[count];
    for (int i = 0; i < count; i++)
    {
      numbers[i] = getRandom(max, annotation);
    }
    return numbers;
  }


  public synchronized int getRandom(int max, String annotation)
  {
    if (s_random == null)
      s_random = new MersenneTwister(getSeed());
    return s_random.nextInt(max);

  }
  
  
  public static void main(String[] args)
  {
     IntegerMap results = new IntegerMap();
     
     int[] random =  new PlainRandomSource().getRandom(6, 100000, "Test");
     for(int i = 0; i < random.length; i++)
     {
         results.add(new Integer(random[i] + 1),  1);
     }
     System.out.println(results);
  }
  
}
