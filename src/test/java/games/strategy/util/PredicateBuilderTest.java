package games.strategy.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class PredicateBuilderTest {

  private final Predicate<Object> truePredicate = o -> true;
  private final Predicate<Object> falsePredicate = o -> false;


  @Test
  public void testSimplePredicate() {
    assertEquals(truePredicate, PredicateBuilder.of(truePredicate).build());
    assertEquals(falsePredicate, PredicateBuilder.of(falsePredicate).build());
  }

  @Test
  public void testAndPredicate() {
    assertFalse(PredicateBuilder.of(falsePredicate).and(falsePredicate).build().test(new Object()));
    assertFalse(PredicateBuilder.of(truePredicate).and(falsePredicate).build().test(new Object()));
    assertFalse(PredicateBuilder.of(falsePredicate).and(truePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).and(truePredicate).build().test(new Object()));


    assertNotEquals(falsePredicate, PredicateBuilder.of(falsePredicate).and(falsePredicate).build());
    assertNotEquals(truePredicate, PredicateBuilder.of(truePredicate).and(truePredicate).build());
  }

  @Test
  public void testOrPredicate() {
    assertFalse(PredicateBuilder.of(falsePredicate).or(falsePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).or(falsePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(falsePredicate).or(truePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).or(truePredicate).build().test(new Object()));


    assertNotEquals(falsePredicate, PredicateBuilder.of(falsePredicate).or(falsePredicate).build());
    assertNotEquals(truePredicate, PredicateBuilder.of(truePredicate).or(truePredicate).build());
  }

  @Test
  public void testAndIfPredicate() {
    assertFalse(PredicateBuilder.of(falsePredicate).andIf(true, falsePredicate).build().test(new Object()));
    assertFalse(PredicateBuilder.of(truePredicate).andIf(true, falsePredicate).build().test(new Object()));
    assertFalse(PredicateBuilder.of(falsePredicate).andIf(true, truePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).andIf(true, truePredicate).build().test(new Object()));


    assertFalse(PredicateBuilder.of(falsePredicate).andIf(false, falsePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).andIf(false, falsePredicate).build().test(new Object()));
    assertFalse(PredicateBuilder.of(falsePredicate).andIf(false, truePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).andIf(false, truePredicate).build().test(new Object()));


    assertNotEquals(falsePredicate, PredicateBuilder.of(falsePredicate).andIf(true, falsePredicate).build());
    assertNotEquals(truePredicate, PredicateBuilder.of(truePredicate).andIf(true, truePredicate).build());

    assertEquals(falsePredicate, PredicateBuilder.of(falsePredicate).andIf(false, falsePredicate).build());
    assertEquals(truePredicate, PredicateBuilder.of(truePredicate).andIf(false, truePredicate).build());
  }

  @Test
  public void testOrIfPredicate() {
    assertFalse(PredicateBuilder.of(falsePredicate).orIf(true, falsePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).orIf(true, falsePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(falsePredicate).orIf(true, truePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).orIf(true, truePredicate).build().test(new Object()));


    assertFalse(PredicateBuilder.of(falsePredicate).orIf(false, falsePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).orIf(false, falsePredicate).build().test(new Object()));
    assertFalse(PredicateBuilder.of(falsePredicate).orIf(false, truePredicate).build().test(new Object()));
    assertTrue(PredicateBuilder.of(truePredicate).orIf(false, truePredicate).build().test(new Object()));


    assertNotEquals(falsePredicate, PredicateBuilder.of(falsePredicate).orIf(true, falsePredicate).build());
    assertNotEquals(truePredicate, PredicateBuilder.of(truePredicate).orIf(true, truePredicate).build());

    assertEquals(falsePredicate, PredicateBuilder.of(falsePredicate).orIf(false, falsePredicate).build());
    assertEquals(truePredicate, PredicateBuilder.of(truePredicate).orIf(false, truePredicate).build());
  }
}
