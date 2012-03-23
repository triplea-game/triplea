package games.strategy.triplea.strongAI;

import games.strategy.triplea.ai.strongAI.SUtils;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class SUtilsTest extends TestCase
{
	public void testReorderLesser()
	{
		final List<String> vals = Arrays.asList("b", "a", "c");
		final Map<String, Double> order = new HashMap<String, Double>();
		order.put("a", 0.1);
		order.put("b", 0.2);
		order.put("c", 0.3);
		SUtils.reorder(vals, order, false);
		assertEquals(Arrays.asList("a", "b", "c"), vals);
	}
	
	public void testReorderGreater()
	{
		final List<String> vals = Arrays.asList("b", "a", "c");
		final Map<String, Double> order = new HashMap<String, Double>();
		order.put("a", 0.1);
		order.put("b", 0.2);
		order.put("c", 0.3);
		SUtils.reorder(vals, order, true);
		assertEquals(Arrays.asList("c", "b", "a"), vals);
	}
	
	public void testReorderIntGreater()
	{
		final List<String> vals = new ArrayList<String>(Arrays.asList("b", "a", "c"));
		final IntegerMap<String> order = new IntegerMap<String>();
		order.put("a", 1);
		order.put("b", 2);
		order.put("c", 3);
		SUtils.reorder(vals, order, true);
		assertEquals(Arrays.asList("c", "b", "a"), vals);
	}
	
	public void testReorderIntLesser()
	{
		final List<String> vals = new ArrayList<String>(Arrays.asList("b", "a", "c"));
		final IntegerMap<String> order = new IntegerMap<String>();
		order.put("a", 1);
		order.put("b", 2);
		order.put("c", 3);
		SUtils.reorder(vals, order, false);
		assertEquals(Arrays.asList("a", "b", "c"), vals);
	}
}
