package games.strategy.triplea.image;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * This class handles the various types of blends for base/relief tiles.
 */
class BlendComposite implements Composite {
  public enum BlendingMode {
    NORMAL, OVERLAY, MULTIPLY, DIFFERENCE, LINEAR_LIGHT
  }

  private float alpha;
  private final BlendingMode mode;

  BlendComposite(final BlendingMode mode) {
    this(mode, 1.0f);
  }

  private BlendComposite(final BlendingMode mode, final float alpha) {
    this.mode = mode;
    setAlpha(alpha);
  }

  public static BlendComposite getInstance(final BlendingMode mode) {
    return new BlendComposite(mode);
  }

  public BlendComposite derive(final float alpha) {
    return (this.alpha == alpha) ? this : new BlendComposite(getMode(), alpha);
  }

  public float getAlpha() {
    return alpha;
  }

  public BlendingMode getMode() {
    return mode;
  }

  private void setAlpha(final float alpha) {
    if ((alpha < 0.0f) || (alpha > 1.0f)) {
      throw new IllegalArgumentException("alpha must be comprised between 0.0f and 1.0f");
    }
    this.alpha = alpha;
  }

  @Override
  public CompositeContext createContext(final ColorModel srcColorModel, final ColorModel dstColorModel,
      final RenderingHints hints) {
    return new BlendingContext(this);
  }

  private static final class BlendingContext implements CompositeContext {
    private final Blender blender;
    private final BlendComposite composite;

    private BlendingContext(final BlendComposite composite) {
      this.composite = composite;
      this.blender = Blender.getBlenderFor(composite);
    }

    @Override
    public void dispose() {}

    @Override
    public void compose(final Raster src, final Raster dstIn, final WritableRaster dstOut) {
      if ((src.getSampleModel().getDataType() != DataBuffer.TYPE_INT)
          || (dstIn.getSampleModel().getDataType() != DataBuffer.TYPE_INT)
          || (dstOut.getSampleModel().getDataType() != DataBuffer.TYPE_INT)) {
        throw new IllegalStateException("Source and destination must store pixels as INT.");
      }
      final int width = Math.min(src.getWidth(), dstIn.getWidth());
      final int height = Math.min(src.getHeight(), dstIn.getHeight());
      final float alpha = composite.getAlpha();
      final int[] srcPixel = new int[4];
      final int[] dstPixel = new int[4];
      final int[] srcPixels = new int[width];
      final int[] dstPixels = new int[width];
      for (int y = 0; y < height; y++) {
        src.getDataElements(0, y, width, 1, srcPixels);
        dstIn.getDataElements(0, y, width, 1, dstPixels);
        for (int x = 0; x < width; x++) {
          // pixels are stored as INT_ARGB
          // our arrays are [R, G, B, A]
          int pixel = srcPixels[x];
          srcPixel[0] = (pixel >> 16) & 0xFF;
          srcPixel[1] = (pixel >> 8) & 0xFF;
          srcPixel[2] = (pixel) & 0xFF;
          srcPixel[3] = (pixel >> 24) & 0xFF;
          pixel = dstPixels[x];
          dstPixel[0] = (pixel >> 16) & 0xFF;
          dstPixel[1] = (pixel >> 8) & 0xFF;
          dstPixel[2] = (pixel) & 0xFF;
          dstPixel[3] = (pixel >> 24) & 0xFF;
          final int[] result = blender.blend(srcPixel, dstPixel);
          // mixes the result with the opacity
          dstPixels[x] = (((int) (dstPixel[3] + ((result[3] - dstPixel[3]) * alpha)) & 0xFF) << 24)
              | (((int) (dstPixel[0] + ((result[0] - dstPixel[0]) * alpha)) & 0xFF) << 16)
              | (((int) (dstPixel[1] + ((result[1] - dstPixel[1]) * alpha)) & 0xFF) << 8)
              | ((int) (dstPixel[2] + ((result[2] - dstPixel[2]) * alpha)) & 0xFF);
        }
        dstOut.setDataElements(0, y, width, 1, dstPixels);
      }
    }
  }

  abstract static class Blender {
    public abstract int[] blend(int[] src, int[] dst);

    private static Blender getBlenderFor(final BlendComposite composite) {
      switch (composite.getMode()) {
        case NORMAL:
          return new Blender() {
            @Override
            public int[] blend(final int[] src, final int[] dst) {
              return src;
            }
          };
        case OVERLAY:
          return new Blender() {
            @Override
            public int[] blend(final int[] src, final int[] dst) {
              return new int[] {
                  (dst[0] < 128) ? ((dst[0] * src[0]) >> 7) : (255 - (((255 - dst[0]) * (255 - src[0])) >> 7)),
                  (dst[1] < 128) ? ((dst[1] * src[1]) >> 7) : (255 - (((255 - dst[1]) * (255 - src[1])) >> 7)),
                  (dst[2] < 128) ? ((dst[2] * src[2]) >> 7) : (255 - (((255 - dst[2]) * (255 - src[2])) >> 7)),
                  Math.min(255, src[3] + dst[3])};
            }
          };
        case LINEAR_LIGHT:
          return new Blender() {
            @Override
            public int[] blend(final int[] src, final int[] dst) {
              return new int[] {(dst[0] < 128) ? ((dst[0] + src[0]) >> (7 - 255)) : ((dst[0] + (src[0] - 128)) >> 7),
                  (dst[1] < 128) ? ((dst[1] + src[1]) >> (7 - 255)) : ((dst[1] + (src[1] - 128)) >> 7),
                  (dst[2] < 128) ? ((dst[2] + src[2]) >> (7 - 255)) : ((dst[2] + (src[2] - 128)) >> 7),
                  Math.min(255, src[3] + dst[3])};
            }
          };
        case MULTIPLY:
          return new Blender() {
            @Override
            public int[] blend(final int[] src, final int[] dst) {
              return new int[] {(src[0] * dst[0]) >> 8, (src[1] * dst[1]) >> 8, (src[2] * dst[2]) >> 8,
                  Math.min(255, src[3] + dst[3])};
            }
          };
        case DIFFERENCE:
          return new Blender() {
            @Override
            public int[] blend(final int[] src, final int[] dst) {
              return new int[] {Math.abs(dst[0] - src[0]), Math.abs(dst[1] - src[1]), Math.abs(dst[2] - src[2]),
                  Math.min(255, src[3] + dst[3])};
            }
          };
        default:
          throw new IllegalArgumentException("Blender not implemented for " + composite.getMode());
      }
    }
  }
}
