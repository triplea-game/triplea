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

package games.strategy.engine.framework;

import games.strategy.engine.data.*;

import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class GameDataManagerTest extends TestCase
{

  

  public GameDataManagerTest(String name)
  {
    super(name);
  }

  public void setUp() throws Exception
  {
    //get the xml file
    URL url = SerializationTest.class.getResource("Test.xml");

    //get the source  data
    InputStream input= url.openStream();

     (new GameParser()).parse(input);

  }

 public void testNone()
 {

 }

}
