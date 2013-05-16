/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.util;

import games.strategy.triplea.attatchments.RulesAttachment;
import junit.framework.TestCase;

public class PropertyUtilTest extends TestCase
{
	/*public void testGet()
	{
		final JLabel label = new JLabel("TestCase");
		assertEquals("TestCase", PropertyUtil.get("text", label));
	}
	
	public void testSet()
	{
		final JLabel label = new JLabel("TestCase");
		PropertyUtil.set("text", "changed", label);
		assertEquals("changed", PropertyUtil.get("text", label));
	}
	
	public void testSetInt()
	{
		final JLabel label = new JLabel();
		PropertyUtil.set("IconTextGap", 10, label);
		PropertyUtil.get("IconTextGap", label);
	}*/
	
	public void testGetFieldObject()
	{
		final RulesAttachment at = new RulesAttachment("test", null, null);
		int uses = (Integer) PropertyUtil.getPropertyFieldObject("uses", at);
		// default value should be -1
		assertEquals(-1, uses);
		PropertyUtil.set("uses", "3", at);
		uses = (Integer) PropertyUtil.getPropertyFieldObject("uses", at);
		assertEquals(3, uses);
		final IntegerMap<String> unitPresence = new IntegerMap<String>();
		unitPresence.add("Blah", 3);
		PropertyUtil.set("unitPresence", unitPresence, at);
		assertEquals(unitPresence, PropertyUtil.getPropertyFieldObject("unitPresence", at));
	}
}
