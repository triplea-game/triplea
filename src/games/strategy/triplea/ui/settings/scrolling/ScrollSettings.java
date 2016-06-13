package games.strategy.triplea.ui.settings.scrolling;

public class ScrollSettings {

  private int mapFasterScrollZoneSizeInPixels = 30;
  private int mapScrollZoneSizeInPixels = 15;

  private int fasterSpeedMultipler = 3;
  private int scrollSpeed = 50;

  private int arrowKeyScrollSpeed = 70;
  private int fasterArrowKeyScrollMultipler = 4;
  public ScrollSettings() {

  }

  public int getMapFasterScrollZoneSizeInPixels() {
    return mapFasterScrollZoneSizeInPixels;
  }

  public void setScrollSpeed(String scrollSpeed) {
    this.scrollSpeed = Integer.parseInt(scrollSpeed);
  }


  public int getScrollSpeed() {
    return scrollSpeed;
  }

  public void setFasterScrollMultipler(String fasterScrollMultipler) {
    this.fasterSpeedMultipler = Integer.parseInt(fasterScrollMultipler);
  }

  public int getFasterSpeedMultipler() {
    return fasterSpeedMultipler;
  }

  public void setFasterScrollZoneSizeInPixels(String fasterScrollZoneSizeInPixels) {
    this.mapFasterScrollZoneSizeInPixels = Integer.parseInt(fasterScrollZoneSizeInPixels);
  }


  public int getMapScrollZoneSizeInPixels() {
    return mapScrollZoneSizeInPixels;
  }

  public void setScrollZoneSizeInPixels(String value) {

    this.mapScrollZoneSizeInPixels = Integer.parseInt(value);
  }

  public int getArrowKeyScrollSpeed() {
    System.out.println("ARROW SCROLL => " + arrowKeyScrollSpeed);
    return arrowKeyScrollSpeed;
  }

  public void setArrowKeyScrollSpeed(String value) {
    arrowKeyScrollSpeed = Integer.parseInt(value);
    System.out.println("SET ARROW SCROLL <= " + arrowKeyScrollSpeed);
  }


  public int getFasterArrowKeyScrollMultipler() {
    return fasterArrowKeyScrollMultipler;
  }

  public void setFasterArrowKeyScrollMultipler(String fasterArrowKeyScrollMultipler) {
    this.fasterArrowKeyScrollMultipler = Integer.valueOf(fasterArrowKeyScrollMultipler);
  }
}
