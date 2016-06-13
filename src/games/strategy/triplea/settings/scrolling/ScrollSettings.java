package games.strategy.triplea.settings.scrolling;

public class ScrollSettings {

  private int mapEdgeFasterScrollZoneSize = 10;
  private int mapEdgeScrollZoneSize = 30;

  private int mapEdgeFasterScrollMultiplier = 3;

  private int arrowKeyScrollSpeed = 70;
  private int fasterArrowKeyScrollMultipler = 4;


  private int mapEdgeScrollSpeed = 30;
  private int wheelScrollAmount = 60;

  public ScrollSettings() {

  }

  public int getMapEdgeFasterScrollZoneSize() {
    return mapEdgeFasterScrollZoneSize;
  }

  public void setMapEdgeFasterScrollMultiplier(String fasterScrollMultipler) {
    this.mapEdgeFasterScrollMultiplier = Integer.parseInt(fasterScrollMultipler);
  }

  public int getMapEdgeFasterScrollMultiplier() {
    return mapEdgeFasterScrollMultiplier;
  }

  public void setMapEdgeFasterScrollZoneSize(String fasterScrollZoneSizeInPixels) {
    this.mapEdgeFasterScrollZoneSize = Integer.parseInt(fasterScrollZoneSizeInPixels);
  }


  public int getMapEdgeScrollZoneSize() {
    return mapEdgeScrollZoneSize;
  }

  public void setMapEdgeScrollZoneSize(String value) {
    this.mapEdgeScrollZoneSize = Integer.parseInt(value);
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

  public int getMapEdgeScrollSpeed() {
    return mapEdgeScrollSpeed;
  }

  public void setMapEdgeScrollSpeed(String mapEdgeScrollSpeed) {
    this.mapEdgeScrollSpeed = Integer.valueOf(mapEdgeScrollSpeed);
  }

  public int getWheelScrollAmount() {
    return wheelScrollAmount;
  }

  public void setWheelScrollAmount(String wheelScrollAmount) {
    this.wheelScrollAmount = Integer.valueOf(wheelScrollAmount);
  }
}
