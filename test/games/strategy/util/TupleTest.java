package games.strategy.util;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.hamcrest.core.IsNot;
import org.junit.Test;

import com.google.common.collect.Maps;

public class TupleTest {
  Tuple<String, Integer> testObj = Tuple.of("hi", 123);

  @Test
  public void basicUsage() {
    assertThat(testObj.getFirst(), is("hi"));
    assertThat(testObj.getSecond(), is(123));
  }

  @Test
  public void verifyEquality() {
    assertThat(testObj, is(testObj));

    Tuple<String, Integer> copyObj = Tuple.of(testObj.getFirst(), testObj.getSecond());
    assertThat(testObj, is(copyObj));
    assertThat(copyObj, is(testObj));

    assertThat("check equals against null case",
        copyObj.equals(null), is(false));
  }

  @Test
  public void verifyToString() {
    assertThat(testObj.toString(), containsString(testObj.getFirst()));
    assertThat(testObj.toString(), containsString(String.valueOf(testObj.getSecond())));
  }

  @Test
  public void checkStoringNullCase() {
    Tuple<String, String> nullTuple = Tuple.of((String) null, (String) null);

    assertThat(nullTuple.getFirst(), nullValue());
    assertThat(nullTuple.getSecond(), nullValue());
    assertThat(nullTuple, IsNot.not(Tuple.of("something else", (String) null)));
  }

  @Test
  public void checkUsingTupleAsMapKey() {
    Map<Tuple<String, String>, String> map = Maps.newHashMap();
    Tuple<String, String> tuple = Tuple.of("This is a bad idea using tuples this much", "another value");
    String value = "some value";

    assertFalse(map.containsKey(tuple));

    map.put(tuple, value);
    assertTrue(map.containsKey(tuple));
    assertThat(map.get(tuple), is(value));
  }
}
