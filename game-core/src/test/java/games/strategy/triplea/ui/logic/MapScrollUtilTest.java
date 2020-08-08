package games.strategy.triplea.ui.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class MapScrollUtilTest {

  @Test
  void testWithXWrapOnly() {
    final List<AffineTransform> transforms =
        MapScrollUtil.getPossibleTranslations(true, false, 9876, 2345);
    assertEquals(3, transforms.size());
    assertTrue(transforms.stream().anyMatch(transCriteria(-9876, 0)));
    assertTrue(transforms.stream().anyMatch(AffineTransform::isIdentity));
    assertTrue(transforms.stream().anyMatch(transCriteria(9876, 0)));
  }

  @Test
  void testWithYWrapOnly() {
    final List<AffineTransform> transforms =
        MapScrollUtil.getPossibleTranslations(false, true, 1234, 5678);
    assertEquals(3, transforms.size());
    assertTrue(transforms.stream().anyMatch(transCriteria(0, -5678)));
    assertTrue(transforms.stream().anyMatch(AffineTransform::isIdentity));
    assertTrue(transforms.stream().anyMatch(transCriteria(0, 5678)));
  }

  @Test
  void testWithXAndYWrap() {
    final List<AffineTransform> transforms =
        MapScrollUtil.getPossibleTranslations(true, true, 2345, 6789);
    assertEquals(9, transforms.size());
    assertTrue(transforms.stream().anyMatch(transCriteria(-2345, -6789)));
    assertTrue(transforms.stream().anyMatch(transCriteria(0, -6789)));
    assertTrue(transforms.stream().anyMatch(transCriteria(2345, -6789)));
    assertTrue(transforms.stream().anyMatch(transCriteria(-2345, 0)));
    assertTrue(transforms.stream().anyMatch(AffineTransform::isIdentity));
    assertTrue(transforms.stream().anyMatch(transCriteria(2345, 0)));
    assertTrue(transforms.stream().anyMatch(transCriteria(-2345, 6789)));
    assertTrue(transforms.stream().anyMatch(transCriteria(0, 6789)));
    assertTrue(transforms.stream().anyMatch(transCriteria(2345, 6789)));
  }

  @Test
  void testWithoutWrap() {
    final List<AffineTransform> transforms =
        MapScrollUtil.getPossibleTranslations(false, false, 8765, 4321);
    assertEquals(1, transforms.size());
    assertTrue(transforms.get(0).isIdentity());
  }

  private static Predicate<AffineTransform> transCriteria(final int xoffset, final int yoffset) {
    return trans ->
        trans.getTranslateX() == xoffset
            && trans.getTranslateY() == yoffset
            && trans.getType() == AffineTransform.TYPE_TRANSLATION;
  }
}
