package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

public class CardDeckTest extends TestCase {

  private CardDeck cardDeck;
  final int deckSizeAtStart = 10;

  @Override
  @Before
  public void setUp() throws Exception {

    final List<Card> cards = new ArrayList<Card>(deckSizeAtStart);
    for (int currentCard = 1; currentCard <= deckSizeAtStart; ++currentCard) {
      cards.add(new Card("Card " + currentCard, PlayerID.NULL_PLAYERID, null));
    }
    cardDeck = new CardDeck(cards);
  }

  @Test
  public void testCardDeck() {
    assertNotNull(cardDeck);
    assertEquals(deckSizeAtStart, cardDeck.getDeckSize());
  }

  @Test
  public void testGetStacksForAllCards() {
    final int stacksCounter = 4;
    final List<List<Card>> stacks = cardDeck.getStacksForAllCards(stacksCounter);
    // Deck should be empty now
    assertEquals(0, cardDeck.getDeckSize());

    final int cardsPerStack = deckSizeAtStart / stacksCounter;
    int cardsCounter = 0;
    for (final List<Card> stack : stacks) {
      final int stackSize = stack.size();
      cardsCounter += stackSize;
      assertTrue(cardsPerStack <= stackSize);
    }

    assertEquals(deckSizeAtStart, cardsCounter);
  }

  @Test
  public void testGetStacksWithEvenSize() {
    final int stacksCounter = 4;
    final List<List<Card>> stacks = cardDeck.getStacksWithEvenSize(stacksCounter);
    final int cardsPerStack = deckSizeAtStart / stacksCounter;
    final int expectedRemainingCards = deckSizeAtStart - stacksCounter * cardsPerStack;
    assertEquals(expectedRemainingCards, cardDeck.getDeckSize());

    for (final List<Card> stack : stacks) {
      assertEquals(stack.size(), cardsPerStack);
    }
  }

}
