package games.strategy.triplea.ui.screen;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable;

public class TileTest {

  private final Image image = mock(Image.class);
  private final Graphics2D graphics = mock(Graphics2D.class);
  private final Rectangle rect = new Rectangle(100, 100);
  private final Tile tile = new Tile(rect, image);
  private final IDrawable drawable = mock(IDrawable.class);

  @BeforeEach
  public void setup() {
    when(image.getGraphics()).thenReturn(graphics);
    when(image.getWidth(any())).thenReturn(100);
    when(image.getHeight(any())).thenReturn(100);
  }

  @Test
  public void testIsDirty() {
    assertSame(image, tile.getImage());
    assertTrue(tile.isDirty());
    assertFalse(tile.hasDrawingStarted());
    tile.drawImage(null, null);
    assertFalse(tile.isDirty());
    assertFalse(tile.hasDrawingStarted());

    tile.addDrawable(drawable);

    assertTrue(tile.isDirty());
    assertFalse(tile.hasDrawingStarted());
    tile.drawImage(null, null);
    assertFalse(tile.isDirty());
    assertFalse(tile.hasDrawingStarted());

    tile.addDrawables(Collections.singleton(drawable));

    assertTrue(tile.isDirty());
    assertFalse(tile.hasDrawingStarted());
    tile.drawImage(null, null);
    assertFalse(tile.isDirty());
    assertFalse(tile.hasDrawingStarted());
  }

  @Test
  public void testDrawOrder() {
    final IDrawable drawable2 = mock(IDrawable.class);
    when(drawable.getLevel()).thenReturn(1);
    when(drawable2.getLevel()).thenReturn(2);
    tile.addDrawable(drawable2);
    tile.addDrawable(drawable);
    final GameData data = mock(GameData.class);
    final MapData mapData = mock(MapData.class);
    final InOrder inOrder = Mockito.inOrder(drawable, drawable2);
    tile.drawImage(data, mapData);
    inOrder.verify(drawable).draw(eq(rect), eq(data), any(), eq(mapData));
    inOrder.verify(drawable2).draw(eq(rect), eq(data), any(), eq(mapData));
  }

  @Test
  public void testDrawableInsertAndRemoval() {
    final IDrawable drawable2 = mock(IDrawable.class);
    tile.addDrawable(drawable);
    tile.addDrawables(Collections.singleton(drawable2));
    assertEquals(Arrays.asList(drawable, drawable2), tile.getDrawables());
    tile.removeDrawables(Collections.singleton(drawable2));
    assertEquals(Collections.singletonList(drawable), tile.getDrawables());
    tile.removeDrawables(Collections.singleton(drawable));
    assertEquals(Collections.emptyList(), tile.getDrawables());
    tile.addDrawables(Arrays.asList(drawable, drawable2));
    tile.clear();
    assertEquals(Collections.emptyList(), tile.getDrawables());
  }

  @Test
  public void testCorrectBounds() {
    assertSame(rect, tile.getBounds());
    final Tile tile = new Tile(rect);
    assertEquals(rect.width, tile.getImage().getWidth(null));
    assertEquals(rect.height, tile.getImage().getHeight(null));
  }
}
