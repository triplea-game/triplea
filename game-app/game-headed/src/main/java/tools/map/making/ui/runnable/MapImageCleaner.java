package tools.map.making.ui.runnable;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Algorithm for cleaning up black and white image map images, to improve them for later use with
 * the PolygonGrabberTask.
 *
 * <p>Performs the following operations (in order): 1. Normalizes colors by changing pixels that
 * aren't white to black. 2. "Fills in" small regions (below `minimumRegionSize`) with black pixels.
 * 3. Removes "unnecessary" black pixels by turning them to white, making the resulting lines
 * between regions have a thickness of 1 pixel.
 *
 * <p>Note: It's up to the user to double-check that the result is as intended. If the input image
 * has an incomplete border (that has a gap), the algorithm will completely remove it.
 */
@Slf4j
class MapImageCleaner {
  // Indicates the pixel is not associated with a region.
  private static final int NO_REGION = 0;

  private final BufferedImage image;
  private final int minimumRegionSize;
  private final int[][] regionIds;

  public MapImageCleaner(final BufferedImage image, int minimumRegionSize) {
    this.image = image;
    this.minimumRegionSize = minimumRegionSize;
    this.regionIds = new int[image.getWidth()][image.getHeight()];
  }

  public void cleanUpImage() {
    turnNonWhitePixelsBlack();
    identifyRegions();
    shrinkRegionBorders();
  }

  private void turnNonWhitePixelsBlack() {
    int numChanged = 0;
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        final Point p = new Point(x, y);
        if (!isColor(p, Color.WHITE) && !isColor(p, Color.BLACK)) {
          setColor(p, Color.BLACK);
          numChanged++;
        }
      }
    }
    log.info("Changed {} non-white non-black pixels to black", numChanged);
  }

  private void identifyRegions() {
    int nextRegionId = 1;
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        final Point p = new Point(x, y);
        if (getRegionId(p) == NO_REGION && isColor(p, Color.WHITE)) {
          final int regionSize = markAllPixelsInRegion(p, nextRegionId);
          log.info("Found region of size {} at {},{}", regionSize, p.x, p.y);
          if (regionSize < minimumRegionSize) {
            log.info("Eliminating region");
            eliminateRegion(nextRegionId);
          }
          nextRegionId++;
        }
      }
    }
  }

  private void eliminateRegion(final int regionId) {
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        final Point p = new Point(x, y);
        if (getRegionId(p) == regionId) {
          setRegionId(p, NO_REGION);
          setColor(p, Color.BLACK);
        }
      }
    }
  }

  private void shrinkRegionBorders() {
    log.info("Shrinking borders of regions");
    // Generate all points in the image. This allows us to shuffle the list so we can iterate it in
    // a random order, which avoids the algorithm causing some undesirable from iterating in the
    // regular for loop order (e.g. single pixel diagonal extensions).
    final List<Point> imagePoints = new ArrayList<>();
    for (int x = 0; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        imagePoints.add(new Point(x, y));
      }
    }
    // Iterate until no more changes have been made.
    int pixelsChanged = Integer.MAX_VALUE;
    while (pixelsChanged > 0) {
      pixelsChanged = 0;
      Collections.shuffle(imagePoints);
      for (final Point p : imagePoints) {
        final int newRegionId = getNewRegionIdForPixel(p);
        if (newRegionId != NO_REGION) {
          setRegionId(p, newRegionId);
          setColor(p, Color.WHITE);
          pixelsChanged++;
        }
      }
      log.info("Updated {} pixels", pixelsChanged);
    }
  }

  private int getNewRegionIdForPixel(final Point p) {
    // Only change black pixels.
    if (!isColor(p, Color.BLACK)) {
      return NO_REGION;
    }
    int existingRegionId = NO_REGION;
    // If all adjacent white pixels have the same region id, return that id.
    for (final Point pixel : getAdjacentPoints(p)) {
      if (!inBounds(pixel)) {
        continue;
      }
      final int pixelRegionId = getRegionId(pixel);
      if (pixelRegionId != NO_REGION) {
        if (pixelRegionId != existingRegionId && existingRegionId != NO_REGION) {
          // We found different adjacent regions to this pixel, keep it.
          return NO_REGION;
        }
        existingRegionId = pixelRegionId;
      }
    }
    return existingRegionId;
  }

  private int markAllPixelsInRegion(Point pixel, final int regionId) {
    // Use a breadth-first traversal over pixels.
    LinkedList<Point> q = new LinkedList<>();
    q.add(pixel);
    int regionSize = 0;
    while (!q.isEmpty()) {
      final Point p = q.removeFirst();
      // Skip points outside of image bounds, points with a region set already and non-white pixels.
      if (!inBounds(p) || getRegionId(p) != NO_REGION || !isColor(p, Color.WHITE)) {
        continue;
      }
      // Mark the pixel as being in this region.
      setRegionId(p, regionId);
      regionSize++;
      Collections.addAll(q, getAdjacentPoints(p));
    }
    return regionSize;
  }

  private Point[] getAdjacentPoints(Point p) {
    // Does not consider diagonals.
    return new Point[] {
      new Point(p.x, p.y - 1),
      new Point(p.x, p.y + 1),
      new Point(p.x - 1, p.y),
      new Point(p.x + 1, p.y)
    };
  }

  private boolean inBounds(Point p) {
    return p.x >= 0 && p.x < image.getWidth() && p.y >= 0 && p.y < image.getHeight();
  }

  private int getRegionId(Point p) {
    return regionIds[p.x][p.y];
  }

  private void setRegionId(Point p, int regionId) {
    regionIds[p.x][p.y] = regionId;
  }

  private boolean isColor(Point p, Color color) {
    return image.getRGB(p.x, p.y) == color.getRGB();
  }

  private void setColor(Point p, Color color) {
    image.setRGB(p.x, p.y, color.getRGB());
  }
}
