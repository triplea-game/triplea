package games.strategy.engine.data.util;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.ResourceList;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

@RunWith(MockitoJUnitRunner.class)
public final class ResourceCollectionUtilsTest {
  @Mock
  private GameData data;

  private Resource pus;

  private Resource techTokens;

  private Resource vps;

  @Before
  public void setUp() {
    pus = createResource(Constants.PUS);
    techTokens = createResource(Constants.TECH_TOKENS);
    vps = createResource(Constants.VPS);
    setGameResources(pus, techTokens, vps);
  }

  private Resource createResource(final String name) {
    return new Resource(name, data);
  }

  private void setGameResources(final Resource... resources) {
    final ResourceList gameResources = mock(ResourceList.class);
    for (final Resource resource : resources) {
      when(gameResources.getResource(resource.getName())).thenReturn(resource);
    }

    when(data.getResourceList()).thenReturn(gameResources);
  }

  @Test
  public void testExcludeByResources_ShouldExcludeSpecifiedResources() {
    final ResourceCollection unfiltered = createResourceCollection(pus, techTokens, vps);

    final ResourceCollection filtered = ResourceCollectionUtils.exclude(unfiltered, pus, vps);

    assertThat(filtered.getQuantity(pus), is(0));
    assertThat(filtered.getQuantity(techTokens), is(unfiltered.getQuantity(techTokens)));
    assertThat(filtered.getQuantity(vps), is(0));
  }

  private ResourceCollection createResourceCollection(final Resource... resources) {
    final ResourceCollection resourceCollection = new ResourceCollection(data);
    resourceCollection.add(new IntegerMap<>(Arrays.stream(resources).collect(toList()), 42));
    return resourceCollection;
  }

  @Test
  public void testExcludeByResources_ShouldIgnoreUnregisteredResources() {
    final Resource gold = createResource("gold");
    final ResourceCollection unfiltered = createResourceCollection(pus);

    final ResourceCollection filtered = ResourceCollectionUtils.exclude(unfiltered, gold);

    assertThat(filtered.getQuantity(pus), is(unfiltered.getQuantity(pus)));
  }

  @Test
  public void testExcludeByNames_ShouldExcludeSpecifiedResources() {
    final ResourceCollection unfiltered = createResourceCollection(pus, techTokens, vps);

    final ResourceCollection filtered = ResourceCollectionUtils.exclude(unfiltered, pus.getName(), vps.getName());

    assertThat(filtered.getQuantity(pus), is(0));
    assertThat(filtered.getQuantity(techTokens), is(unfiltered.getQuantity(techTokens)));
    assertThat(filtered.getQuantity(vps), is(0));
  }

  @Test
  public void testExcludeByNames_ShouldIgnoreUnregisteredResourceNames() {
    final Resource gold = createResource("gold");
    final ResourceCollection unfiltered = createResourceCollection(pus);

    final ResourceCollection filtered = ResourceCollectionUtils.exclude(unfiltered, gold.getName());

    assertThat(filtered.getQuantity(pus), is(unfiltered.getQuantity(pus)));
  }

  @Test
  public void testGetProductionResources_ShouldIncludeAllResourcesExceptTechTokensAndVPs() {
    final Resource gold = createResource("gold");
    setGameResources(gold, pus, techTokens, vps);
    final ResourceCollection unfiltered = createResourceCollection(gold, pus, techTokens, vps);

    final ResourceCollection filtered = ResourceCollectionUtils.getProductionResources(unfiltered);

    assertThat(filtered.getQuantity(gold), is(unfiltered.getQuantity(gold)));
    assertThat(filtered.getQuantity(pus), is(unfiltered.getQuantity(pus)));
    assertThat(filtered.getQuantity(techTokens), is(0));
    assertThat(filtered.getQuantity(vps), is(0));
  }
}
