package games.strategy.engine.data.util;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.ResourceList;
import games.strategy.triplea.Constants;
import java.util.Arrays;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.IntegerMap;

@ExtendWith(MockitoExtension.class)
final class ResourceCollectionUtilsTest {
  @Mock private GameData data;

  private Resource pus;

  private Resource techTokens;

  private Resource vps;

  @BeforeEach
  void setUp() {
    pus = newResource(Constants.PUS);
    techTokens = newResource(Constants.TECH_TOKENS);
    vps = newResource(Constants.VPS);
  }

  private Resource newResource(final String name) {
    return new Resource(name, data);
  }

  @Test
  void testExcludeByResources_ShouldExcludeSpecifiedResources() {
    final ResourceCollection unfiltered = newResourceCollection(pus, techTokens, vps);

    final ResourceCollection filtered = ResourceCollectionUtils.exclude(unfiltered, pus, vps);

    assertThat(filtered.getQuantity(pus), is(0));
    assertThat(filtered.getQuantity(techTokens), is(unfiltered.getQuantity(techTokens)));
    assertThat(filtered.getQuantity(vps), is(0));
  }

  private ResourceCollection newResourceCollection(final Resource... resources) {
    final ResourceCollection resourceCollection = new ResourceCollection(data);
    resourceCollection.add(
        new IntegerMap<>(Arrays.stream(resources).collect(toMap(Function.identity(), r -> 42))));
    return resourceCollection;
  }

  @Test
  void testExcludeByResourcesShouldIgnoreUnregisteredResources() {
    final Resource gold = newResource("gold");
    final ResourceCollection unfiltered = newResourceCollection(pus);

    final ResourceCollection filtered = ResourceCollectionUtils.exclude(unfiltered, gold);

    assertThat(filtered.getQuantity(pus), is(unfiltered.getQuantity(pus)));
  }

  @Test
  void testExcludeByNamesShouldExcludeSpecifiedResources() {
    givenGameResources(pus, vps);
    final ResourceCollection unfiltered = newResourceCollection(pus, techTokens, vps);

    final ResourceCollection filtered =
        ResourceCollectionUtils.exclude(unfiltered, pus.getName(), vps.getName());

    assertThat(filtered.getQuantity(pus), is(0));
    assertThat(filtered.getQuantity(techTokens), is(unfiltered.getQuantity(techTokens)));
    assertThat(filtered.getQuantity(vps), is(0));
  }

  private void givenGameResources(final Resource... resources) {
    final ResourceList gameResources = mock(ResourceList.class);
    for (final Resource resource : resources) {
      doReturn(resource).when(gameResources).getResource(resource.getName());
    }
    when(data.getResourceList()).thenReturn(gameResources);
  }

  @Test
  void testExcludeByNamesShouldIgnoreUnregisteredResourceNames() {
    final Resource gold = newResource("gold");
    givenGameResources();
    final ResourceCollection unfiltered = newResourceCollection(pus);

    final ResourceCollection filtered = ResourceCollectionUtils.exclude(unfiltered, gold.getName());

    assertThat(filtered.getQuantity(pus), is(unfiltered.getQuantity(pus)));
  }

  @Test
  void testGetProductionResourcesShouldIncludeAllResourcesExceptTechTokensAndVPs() {
    final Resource gold = newResource("gold");
    givenGameResources(techTokens, vps);
    final ResourceCollection unfiltered = newResourceCollection(gold, pus, techTokens, vps);

    final ResourceCollection filtered = ResourceCollectionUtils.getProductionResources(unfiltered);

    assertThat(filtered.getQuantity(gold), is(unfiltered.getQuantity(gold)));
    assertThat(filtered.getQuantity(pus), is(unfiltered.getQuantity(pus)));
    assertThat(filtered.getQuantity(techTokens), is(0));
    assertThat(filtered.getQuantity(vps), is(0));
  }
}
