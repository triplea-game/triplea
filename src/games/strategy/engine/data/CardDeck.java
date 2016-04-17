package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CardDeck {
  private final List<Card> cards;

  public CardDeck(final List<Card> cards) {
    this.cards = cards;
  }

  /**
   *
   * @return Card from the card deck.
   */
  public Card drawRandomCard() {
    final Random generator = new Random();
    final int index = generator.nextInt(cards.size());
    return cards.remove(index);
  }

  /**
   * Returns all cards divided into stacks of the card deck.
   * In case they cannot be evenly divided, the method adds the remaining cards to the first stacks.
   *
   * @param stacks - number of stacks requested
   * @return list of stacks
   */
  public List<List<Card>> getStacksForAllCards(final int stacks) {
    final List<List<Card>> result = new ArrayList<List<Card>>(stacks);
    final int stackSizeMin = cards.size() / stacks;
    final int stacksWithAdditionalCard = cards.size() % stacks;
    for (int stackNr = 0; stackNr < stacks; ++stackNr) {
      // initialize currentStack if needed with one card already
      final List<Card> currentStack;
      if (stackNr < stacksWithAdditionalCard) {
        currentStack = new ArrayList<Card>(stackSizeMin + 1);
        currentStack.add(drawRandomCard());
      } else {
        currentStack = new ArrayList<Card>(stackSizeMin);
      }

      for (int i = 0; i < stackSizeMin; ++i) {
        currentStack.add(drawRandomCard());
      }
      result.add(currentStack);
    }
    return result;
  }

  /**
   * Returns evenly sized stacks of the card deck.
   *
   * @param stacks - number of stacks requested
   * @return list of evenly sized stacks
   */
  public List<List<Card>> getStacksWithEvenSize(final int stacks) {
    final List<List<Card>> result = new ArrayList<List<Card>>(stacks);
    final int stackSizeMin = cards.size() / stacks;
    for (int stackNr = 0; stackNr < stacks; ++stackNr) {
      final List<Card> currentStack = new ArrayList<Card>(stackSizeMin);
      for (int i = 0; i < stackSizeMin; ++i) {
        currentStack.add(drawRandomCard());
      }
      result.add(currentStack);
    }
    return result;
  }

  public int getDeckSize() {
    return cards.size();
  }

  @Override
  public String toString() {
    final String result = "Cards remaining in deck: " + cards;

    return result;

  }
}
