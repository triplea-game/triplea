package games.strategy.triplea.delegate;

import java.io.Serializable;

class BidPurchaseExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 6896164200767186673L;

  Serializable superState;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  int m_bid;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  int m_spent;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  boolean m_hasBid;
}
