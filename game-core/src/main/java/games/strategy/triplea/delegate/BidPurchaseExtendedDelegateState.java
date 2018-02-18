package games.strategy.triplea.delegate;

import java.io.Serializable;

class BidPurchaseExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 6896164200767186673L;
  Serializable superState;
  int m_bid;
  int m_spent;
  boolean m_hasBid;
}
