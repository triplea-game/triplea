package games.strategy.triplea.oddsCalculator.zengland;

import java.math.BigDecimal;

public class tester {
  public tester() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @param args
   */
  public static void main(final String[] args) {
    final BigDecimal per = new BigDecimal(1.001d);
    String res = per.toString();
    int endSpace = 0;
    if (res.indexOf(".") + 3 >= res.length() || res.indexOf(".") == -1) {
      endSpace = res.length();
    } else {
      endSpace = res.indexOf(".") + 3;
    }
    res = res.substring(0, endSpace);
    if (res.indexOf(".") == -1) {
      res += ".00";
    }
    res += "%";
  }
}
