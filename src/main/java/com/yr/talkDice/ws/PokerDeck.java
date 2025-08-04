package com.yr.talkDice.ws;

import java.util.*;

public class PokerDeck {

    public static Set<String> generateDeck() {
        String[] suits = {"♥", "♦", "♣", "♠"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        Set<String> deck = new HashSet<>();

        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(suit + rank);
            }
        }

        return deck;
    }

    public static List<String> shuffleDeck() {
        List<String> deckList = new ArrayList<>(generateDeck());
        Collections.shuffle(deckList);
        return deckList;
    }

    public static String popRandomElements(int n, List<String> pokerList) {
        if (n > pokerList.size() || n < 0) {
            throw new IllegalArgumentException("Invalid number of elements to pop.");
        }

        Random random = new Random();
        List<String> poppedElements = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int randomIndex = random.nextInt(pokerList.size());
            String element = pokerList.remove(randomIndex);
            poppedElements.add(element);
        }

        return String.join(", ", poppedElements);
    }

}
