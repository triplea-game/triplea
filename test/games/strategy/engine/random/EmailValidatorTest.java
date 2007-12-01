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

import games.strategy.util.Util;
import junit.framework.TestCase;

/**
 * 
 *
 *
 * @author Sean Bridges
 */
public class EmailValidatorTest extends TestCase
{
    
    /**
     * @param arg0
     */
    public EmailValidatorTest(String arg0)
    {
        super(arg0);
    }
    
    
    public void testValidEmail()
    {
        String[] good = new String[]
                                    {
                			"some@some.com",
                			"some.someMore@some.com",
                			"some@some.com some2@some2.com",
                            "some@some.com some2@some2.co.uk",
                            "some@some.com some2@some2.co.br",
                			"",
                      "some@some.some.some.com"
                                    };
        String[] bad = new String[]
                                   {
               			"test"
               			
                                   };

        
        
        for(int i = 0; i < good.length; i++)
        {
            assertTrue(good[i] + " is good but failed",  Util.isMailValid(good[i]));
        }
        
        for(int i = 0; i < bad.length; i++)
        {
            assertFalse(bad[i] + " is bad but passed",  Util.isMailValid(bad[i]));
        }
                                    
    }
     
    
    
}
