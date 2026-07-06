package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.triplea.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceCollectionTest {

  private static final GameData GAME_DATA = new GameData();
  private static final String RESOURCE_2 = "resource2";
  private static final String RESOURCE_3 = "resource3";
  private static final Resource resourcePu = new Resource(Constants.PUS, GAME_DATA);
  private static final Resource resource2 = new Resource(RESOURCE_2, GAME_DATA);
  private static final Resource resource3 = new Resource(RESOURCE_3, GAME_DATA);
  private ResourceCollection resourceCollection;

  @BeforeEach
  void setUp() {
    resourceCollection = buildResourceCollectionWith(5, 6, 7);
  }

  ResourceCollection buildResourceCollectionWith(
      final int puCount, final int resource2Count, final int resource3Count) {
    final ResourceCollection resourceCollectionNew = new ResourceCollection(GAME_DATA);
    resourceCollectionNew.putResource(resourcePu, puCount);
    resourceCollectionNew.putResource(resource2, resource2Count);
    resourceCollectionNew.putResource(resource3, resource3Count);
    return resourceCollectionNew;
  }

  @Test
  void addResource() {
    resourceCollection.addResource(resourcePu, 4);
    resourceCollection.addResource(resource2, 3);
    resourceCollection.addResource(resource3, 2);
    verifyResourceMap(9, 9, 9);
  }

  private void verifyResourceMap(
      final int expectedPuCount,
      final int expectedResource2Count,
      final int expectedResource3Count) {
    verifyResourceMap(
        expectedPuCount, expectedResource2Count, expectedResource3Count, resourceCollection);
  }

  private void verifyResourceMap(
      final int expectedPuCount,
      final int expectedResource2Count,
      final int expectedResource3Count,
      final ResourceCollection resourceCollectionToVerify) {
    assertThat(
        "Resource " + resourcePu + " should have expected result value.",
        resourceCollectionToVerify.getQuantity(resourcePu),
        is(expectedPuCount));
    assertThat(
        "Resource " + resource2 + " should have expected result value.",
        resourceCollectionToVerify.getQuantity(resource2),
        is(expectedResource2Count));
    assertThat(
        "Resource " + resource3 + " should have expected result value.",
        resourceCollectionToVerify.getQuantity(resource3),
        is(expectedResource3Count));
  }

  @Test
  void addResourceCollection() {
    final ResourceCollection resourceCollectionAdd = buildResourceCollectionWith(4, 3, 2);
    resourceCollection.add(resourceCollectionAdd);
    verifyResourceMap(9, 9, 9);
  }

  @Test
  void removeResourceUpTo() {
    resourceCollection.removeResourceUpTo(resourcePu, 6);
    resourceCollection.removeResourceUpTo(resource2, 6);
    resourceCollection.removeResourceUpTo(resource3, 6);
    verifyResourceMap(0, 0, 1);
  }

  @Test
  void difference() {
    final ResourceCollection resourceCollectionSubtract = buildResourceCollectionWith(4, 3, 2);
    ResourceCollection resourceCollectionDifference =
        resourceCollection.difference(resourceCollectionSubtract);
    verifyResourceMap(1, 3, 5, resourceCollectionDifference);
  }

  @Test
  void fitsHowOften() {
    final ResourceCollection resourceCollectionFit = buildResourceCollectionWith(2, 2, 2);
    int fitCount = resourceCollection.fitsHowOften(resourceCollectionFit.getResourcesCopy());
    assertThat(fitCount, is(2));
  }

  @Test
  void fitsHowOftenMaxByEmptyCosts() {
    final ResourceCollection resourceCollectionFit = buildResourceCollectionWith(0, 0, 0);
    int fitCount = resourceCollection.fitsHowOften(resourceCollectionFit.getResourcesCopy());
    assertThat(fitCount, is(ResourceCollection.MAX_FIT_VALUE));
  }

  @Test
  void fitsHowOftenMaxWithFilledCosts() {
    final ResourceCollection resourceCollectionFit = buildResourceCollectionWith(1, 0, 0);
    resourceCollection.putResource(resourcePu, ResourceCollection.MAX_FIT_VALUE);
    int fitCount = resourceCollection.fitsHowOften(resourceCollectionFit.getResourcesCopy());
    assertThat(fitCount, is(ResourceCollection.MAX_FIT_VALUE));
  }

  @Test
  void multiply() {
    resourceCollection.multiply(2);
    verifyResourceMap(10, 12, 14);
  }
}
