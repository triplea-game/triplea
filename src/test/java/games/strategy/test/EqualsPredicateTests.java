package games.strategy.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public final class EqualsPredicateTests {
  private static EqualsPredicate newEqualsPredicate(final EqualityComparator... equalityComparators) {
    return new EqualsPredicate(EqualityComparatorRegistry.newInstance(equalityComparators));
  }

  @Nested
  public final class NullAndReferenceEqualityTest {
    private final EqualsPredicate equalsPredicate = newEqualsPredicate();

    @Test
    public void shouldReturnTrueWhenO1IsNullAndO2IsNull() {
      assertTrue(equalsPredicate.test(null, null));
    }

    @Test
    public void shouldReturnFalseWhenO1IsNullAndO2IsNotNull() {
      assertFalse(equalsPredicate.test(null, new Object()));
    }

    @Test
    public void shouldReturnFalseWhenO1IsNotNullAndO2IsNull() {
      assertFalse(equalsPredicate.test(new Object(), null));
    }

    @Test
    public void shouldReturnTrueWhenObjectsAreSame() {
      final Object o = new Object();

      assertTrue(equalsPredicate.test(o, o));
    }
  }

  @Nested
  public final class EquatableObjectWithoutCustomComparatorTest {
    private final EqualsPredicate equalsPredicate = newEqualsPredicate();

    @Test
    public void shouldReturnTrueWhenObjectsAreEqual() {
      assertTrue(equalsPredicate.test(new Integer(42), new Integer(42)));
    }

    @Test
    public void shouldReturnFalseWhenObjectsAreNotEqual() {
      assertFalse(equalsPredicate.test(new Integer(42), new Integer(-42)));
    }
  }

  @Nested
  public final class EquatableObjectWithCustomComparatorTest {
    @Test
    public void shouldReturnTrueWhenObjectsAreEqual() {
      final EqualsPredicate equalsPredicate = newEqualsPredicate(
          EqualityComparator.newInstance(Integer.class, (context, o1, o2) -> true));

      assertTrue(equalsPredicate.test(new Integer(42), new Integer(-42)));
    }

    @Test
    public void shouldReturnFalseWhenObjectsAreNotEqual() {
      final EqualsPredicate equalsPredicate = newEqualsPredicate(
          EqualityComparator.newInstance(Integer.class, (context, o1, o2) -> false));

      assertFalse(equalsPredicate.test(new Integer(42), new Integer(42)));
    }
  }

  @Nested
  public final class NonEquatableObjectTest {
    private final EqualsPredicate equalsPredicate = newEqualsPredicate(
        EqualityComparator.newInstance(FakeClass.class, (context, o1, o2) -> o1.value == o2.value));

    @Test
    public void shouldReturnTrueWhenObjectsAreEqual() {
      assertTrue(equalsPredicate.test(new FakeClass(42), new FakeClass(42)));
    }

    @Test
    public void shouldReturnFalseWhenObjectsAreNotEqual() {
      assertFalse(equalsPredicate.test(new FakeClass(42), new FakeClass(-42)));
    }
  }

  @Nested
  public final class NonEquatableNestedObjectTest {
    private final EqualsPredicate equalsPredicate = newEqualsPredicate(
        EqualityComparator.newInstance(FakeClass.class, (context, o1, o2) -> o1.value == o2.value),
        EqualityComparator.newInstance(FakeNestedClass.class, (context, o1, o2) -> context.equals(o1.value, o2.value)));

    @Test
    public void shouldReturnTrueWhenObjectsAreEqual() {
      assertTrue(equalsPredicate.test(
          new FakeNestedClass(new FakeClass(42)),
          new FakeNestedClass(new FakeClass(42))));
    }

    @Test
    public void shouldReturnFalseWhenObjectsAreNotEqual() {
      assertFalse(equalsPredicate.test(
          new FakeNestedClass(new FakeClass(42)),
          new FakeNestedClass(new FakeClass(-42))));
    }

    private final class FakeNestedClass {
      final FakeClass value;

      FakeNestedClass(final FakeClass value) {
        this.value = value;
      }
    }
  }

  @Nested
  public final class NonEquatableCircularReferenceTest {
    final EqualsPredicate equalsPredicate = newEqualsPredicate(
        EqualityComparator.newInstance(
            FakeOwnerClass.class,
            (context, o1, o2) -> context.equals(o1.ownee, o2.ownee) && (o1.value == o2.value)),
        EqualityComparator.newInstance(
            FakeOwneeClass.class,
            (context, o1, o2) -> context.equals(o1.owner, o2.owner) && (o1.value == o2.value)));

    @Test
    public void shouldReturnTrueWhenObjectsAreEqual() {
      final FakeOwnerClass owner1 = new FakeOwnerClass(11);
      final FakeOwneeClass ownee1 = new FakeOwneeClass(owner1, 22);
      owner1.ownee = ownee1;
      final FakeOwnerClass owner2 = new FakeOwnerClass(11);
      final FakeOwneeClass ownee2 = new FakeOwneeClass(owner2, 22);
      owner2.ownee = ownee2;

      assertTrue(equalsPredicate.test(owner1, owner2));
    }

    @Test
    public void shouldReturnFalseWhenOwneeObjectsAreNotEqual() {
      final FakeOwnerClass owner1 = new FakeOwnerClass(11);
      final FakeOwneeClass ownee1 = new FakeOwneeClass(owner1, 22);
      owner1.ownee = ownee1;
      final FakeOwnerClass owner2 = new FakeOwnerClass(11);
      final FakeOwneeClass ownee2 = new FakeOwneeClass(owner2, -22);
      owner2.ownee = ownee2;

      assertFalse(equalsPredicate.test(owner1, owner2));
    }

    @Test
    public void shouldReturnFalseWhenOwnerObjectsAreNotEqual() {
      final FakeOwnerClass owner1 = new FakeOwnerClass(11);
      final FakeOwneeClass ownee1 = new FakeOwneeClass(owner1, 22);
      owner1.ownee = ownee1;
      final FakeOwnerClass owner2 = new FakeOwnerClass(-11);
      final FakeOwneeClass ownee2 = new FakeOwneeClass(owner2, 22);
      owner2.ownee = ownee2;

      assertFalse(equalsPredicate.test(owner1, owner2));
    }

    private final class FakeOwnerClass {
      FakeOwneeClass ownee;
      final int value;

      FakeOwnerClass(final int value) {
        this.value = value;
      }
    }

    private final class FakeOwneeClass {
      final FakeOwnerClass owner;
      final int value;

      FakeOwneeClass(final FakeOwnerClass owner, final int value) {
        this.owner = owner;
        this.value = value;
      }
    }
  }

  private static final class FakeClass {
    final int value;

    FakeClass(final int value) {
      this.value = value;
    }
  }
}
