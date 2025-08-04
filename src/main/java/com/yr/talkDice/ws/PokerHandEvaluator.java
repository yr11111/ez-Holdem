package com.yr.talkDice.ws;

import java.util.*;
import java.util.stream.Collectors;

public class PokerHandEvaluator {

    private static final List<String> RANKS = Arrays.asList("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A");
    private static final List<String> SUITS = Arrays.asList("♥", "♦", "♣", "♠");
    private static final Map<HandType, String> HAND_TYPE_NAMES = new HashMap<>();

    static {
        HAND_TYPE_NAMES.put(HandType.HIGH_CARD, "高牌");
        HAND_TYPE_NAMES.put(HandType.ONE_PAIR, "一对");
        HAND_TYPE_NAMES.put(HandType.TWO_PAIR, "两对");
        HAND_TYPE_NAMES.put(HandType.THREE_OF_A_KIND, "三条");
        HAND_TYPE_NAMES.put(HandType.STRAIGHT, "顺子");
        HAND_TYPE_NAMES.put(HandType.FLUSH, "同花");
        HAND_TYPE_NAMES.put(HandType.FULL_HOUSE, "葫芦");
        HAND_TYPE_NAMES.put(HandType.FOUR_OF_A_KIND, "四条");
        HAND_TYPE_NAMES.put(HandType.STRAIGHT_FLUSH, "同花顺");
        HAND_TYPE_NAMES.put(HandType.ROYAL_FLUSH, "皇家同花顺");
    }

    public static void main(String[] args) {
        List<String> hand1 = Arrays.asList("♥6", "♠A", "♣3", "♦4", "♠10", "♠J", "♠K");
        List<String> hand2 = Arrays.asList("♥A", "♠Q", "♣3", "♦4", "♠5", "♠9", "♠K");
        List<String> hand3 = Arrays.asList("♥2", "♠J", "♣4", "♦5", "♠6", "♠7", "♠Q");
        List<Result> results = new ArrayList<>();
        results.add(getBestPokerHand(hand1));
        results.add(getBestPokerHand(hand2));
        results.add(getBestPokerHand(hand3));

        Result bestOfBest = getBestOfBestHands(results);
        System.out.println("Best Hand: " + bestOfBest.bestHand + ", Hand Type: " + bestOfBest.handTypeName);
    }

    public static Result getBestPokerHand(List<String> cards) {
        List<List<String>> allCombinations = getAllCombinations(cards);
        List<String> bestHand = new ArrayList<>();
        HandType bestHandType = HandType.HIGH_CARD;

        for (List<String> hand : allCombinations) {
            HandType currentHandType = evaluateHand(hand);
            if (currentHandType.compareTo(bestHandType) > 0 ||
                    (currentHandType == bestHandType && isBetterHand(hand, bestHand))) {
                bestHand = new ArrayList<>(hand);
                bestHandType = currentHandType;
            }
        }

        // 确保在高牌牌型时返回最高的五张牌
        if (bestHandType == HandType.HIGH_CARD) {
            Collections.sort(cards, (card1, card2) -> RANKS.indexOf(card2.substring(1)) - RANKS.indexOf(card1.substring(1)));
            bestHand = cards.subList(0, 5);
        }

        return new Result(bestHand, HAND_TYPE_NAMES.get(bestHandType), false);
    }

    private static List<List<String>> getAllCombinations(List<String> cards) {
        List<List<String>> combinations = new ArrayList<>();
        int n = cards.size();
        for (int i = 0; i < (1 << n); i++) {
            List<String> combination = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) > 0) {
                    combination.add(cards.get(j));
                }
            }
            if (combination.size() == 5) {
                combinations.add(combination);
            }
        }
        return combinations;
    }

    private static HandType evaluateHand(List<String> hand) {
        // 排序手牌
        Collections.sort(hand, (card1, card2) -> {
            int rankComparison = Integer.compare(RANKS.indexOf(card1.substring(1)), RANKS.indexOf(card2.substring(1)));
            if (rankComparison != 0) return rankComparison;
            return SUITS.indexOf(card1.substring(0, 1)) - SUITS.indexOf(card2.substring(0, 1));
        });

        boolean isFlush = true;
        boolean isStraight = true;
        Set<String> ranks = new HashSet<>();
        String previousRank = null;
        String suit = hand.get(0).substring(0, 1);

        for (String card : hand) {
            String rank = card.substring(1);
            ranks.add(rank);
            if (!card.substring(0, 1).equals(suit)) {
                isFlush = false;
            }
            if (previousRank != null && RANKS.indexOf(rank) != RANKS.indexOf(previousRank) + 1) {
                isStraight = false;
            }
            previousRank = rank;
        }

        // 处理 A 2 3 4 5 顺子
        if (!isStraight && ranks.containsAll(Arrays.asList("A", "2", "3", "4", "5"))) {
            isStraight = true;
            // 排序成 2, 3, 4, 5, A 的顺序
            hand.sort((card1, card2) -> {
                if (card1.substring(1).equals("A")) return 1;
                if (card2.substring(1).equals("A")) return -1;
                return RANKS.indexOf(card1.substring(1)) - RANKS.indexOf(card2.substring(1));
            });
        }

        if (isFlush && isStraight) {
            return ranks.contains("A") && ranks.contains("10") ? HandType.ROYAL_FLUSH : HandType.STRAIGHT_FLUSH;
        }

        Map<String, Integer> rankCounts = new HashMap<>();
        for (String card : hand) {
            String rank = card.substring(1);
            rankCounts.put(rank, rankCounts.getOrDefault(rank, 0) + 1);
        }

        if (rankCounts.containsValue(4)) {
            return HandType.FOUR_OF_A_KIND;
        }
        if (rankCounts.containsValue(3) && rankCounts.containsValue(2)) {
            return HandType.FULL_HOUSE;
        }
        if (isFlush) {
            return HandType.FLUSH;
        }
        if (isStraight) {
            return HandType.STRAIGHT;
        }
        if (rankCounts.containsValue(3)) {
            return HandType.THREE_OF_A_KIND;
        }
        if (Collections.frequency(rankCounts.values(), 2) == 2) {
            return HandType.TWO_PAIR;
        }
        if (rankCounts.containsValue(2)) {
            return HandType.ONE_PAIR;
        }
        return HandType.HIGH_CARD;
    }


    private static boolean isBetterHand(List<String> currentHand, List<String> bestHand) {
        if (currentHand.isEmpty() || bestHand.isEmpty()) {
            return false;
        }

        HandType currentHandType = evaluateHand(currentHand);
        HandType bestHandType = evaluateHand(bestHand);

        if (currentHandType != bestHandType) {
            return currentHandType.compareTo(bestHandType) > 0;
        }

        // 处理同牌型的比较逻辑
        switch (currentHandType) {
            case ONE_PAIR:
                return compareOnePair(currentHand, bestHand);
            case TWO_PAIR:
                return compareTwoPair(currentHand, bestHand);
            case THREE_OF_A_KIND:
                return compareThreeOfAKind(currentHand, bestHand);
            case STRAIGHT:
                return compareStraight(currentHand, bestHand);
            case FLUSH:
                return compareFlush(currentHand, bestHand);
            case FULL_HOUSE:
                return compareFullHouse(currentHand, bestHand);
            case FOUR_OF_A_KIND:
                return compareFourOfAKind(currentHand, bestHand);
            case STRAIGHT_FLUSH:
            case ROYAL_FLUSH:
                return compareStraightFlush(currentHand, bestHand);
            default:
                return compareHighCard(currentHand, bestHand);
        }
    }

    // 比较一对的逻辑
    private static boolean compareOnePair(List<String> currentHand, List<String> bestHand) {
        int currentPairRank = getPairRank(currentHand);
        int bestPairRank = getPairRank(bestHand);

        if (currentPairRank != bestPairRank) {
            return currentPairRank > bestPairRank;
        }

        return compareRemainingCards(currentHand, bestHand);
    }

    // 比较两对的逻辑
    private static boolean compareTwoPair(List<String> currentHand, List<String> bestHand) {
        List<Integer> currentPairs = getTwoPairsRanks(currentHand);
        List<Integer> bestPairs = getTwoPairsRanks(bestHand);

        Collections.sort(currentPairs, Collections.reverseOrder());
        Collections.sort(bestPairs, Collections.reverseOrder());

        for (int i = 0; i < currentPairs.size(); i++) {
            if (!currentPairs.get(i).equals(bestPairs.get(i))) {
                return currentPairs.get(i) > bestPairs.get(i);
            }
        }

        return compareRemainingCards(currentHand, bestHand);
    }

    // 比较三条的逻辑
    private static boolean compareThreeOfAKind(List<String> currentHand, List<String> bestHand) {
        int currentTripleRank = getTripleRank(currentHand);
        int bestTripleRank = getTripleRank(bestHand);

        if (currentTripleRank != bestTripleRank) {
            return currentTripleRank > bestTripleRank;
        }

        return compareRemainingCards(currentHand, bestHand);
    }

    // 比较顺子的逻辑
    private static boolean compareStraight(List<String> currentHand, List<String> bestHand) {
        int currentHighCardIndex = getHighCardIndexForStraight(currentHand);
        int bestHighCardIndex = getHighCardIndexForStraight(bestHand);
        return currentHighCardIndex > bestHighCardIndex;
    }

    // 比较同花的逻辑
    private static boolean compareFlush(List<String> currentHand, List<String> bestHand) {
        return compareHighCard(currentHand, bestHand);
    }

    // 比较葫芦的逻辑
    private static boolean compareFullHouse(List<String> currentHand, List<String> bestHand) {
        return compareThreeOfAKind(currentHand, bestHand);
    }

    // 比较四条的逻辑
    private static boolean compareFourOfAKind(List<String> currentHand, List<String> bestHand) {
        int currentQuadRank = getQuadRank(currentHand);
        int bestQuadRank = getQuadRank(bestHand);

        if (currentQuadRank != bestQuadRank) {
            return currentQuadRank > bestQuadRank;
        }

        return compareRemainingCards(currentHand, bestHand);
    }

    // 比较同花顺或皇家同花顺的逻辑
    private static boolean compareStraightFlush(List<String> currentHand, List<String> bestHand) {
        return compareStraight(currentHand, bestHand);
    }

    // 比较高牌的逻辑
    private static boolean compareHighCard(List<String> currentHand, List<String> bestHand) {
        for (int i = 0; i < currentHand.size(); i++) {
            int currentRank = RANKS.indexOf(currentHand.get(i).substring(1));
            int bestRank = RANKS.indexOf(bestHand.get(i).substring(1));
            if (currentRank > bestRank) {
                return true;
            } else if (currentRank < bestRank) {
                return false;
            }
        }
        return false;
    }

    // 获取一对的牌值
    private static int getPairRank(List<String> hand) {
        return getRankWithCount(hand, 2);
    }

    // 获取两对的牌值
    private static List<Integer> getTwoPairsRanks(List<String> hand) {
        return getRanksWithCount(hand, 2);
    }

    // 获取三条的牌值
    private static int getTripleRank(List<String> hand) {
        return getRankWithCount(hand, 3);
    }

    // 获取四条的牌值
    private static int getQuadRank(List<String> hand) {
        return getRankWithCount(hand, 4);
    }

    // 获取某个数量的牌值
    private static int getRankWithCount(List<String> hand, int count) {
        Map<String, Integer> rankCount = new HashMap<>();
        for (String card : hand) {
            String rank = card.substring(1);
            rankCount.put(rank, rankCount.getOrDefault(rank, 0) + 1);
        }
        return rankCount.entrySet().stream()
                .filter(entry -> entry.getValue() == count)
                .mapToInt(entry -> RANKS.indexOf(entry.getKey()))
                .max()
                .orElse(-1);
    }

    // 获取某个数量的牌值列表
    private static List<Integer> getRanksWithCount(List<String> hand, int count) {
        Map<String, Integer> rankCount = new HashMap<>();
        for (String card : hand) {
            String rank = card.substring(1);
            rankCount.put(rank, rankCount.getOrDefault(rank, 0) + 1);
        }
        return rankCount.entrySet().stream()
                .filter(entry -> entry.getValue() == count)
                .map(entry -> RANKS.indexOf(entry.getKey()))
                .collect(Collectors.toList());
    }

    // 比较剩余的牌值
    private static boolean compareRemainingCards(List<String> currentHand, List<String> bestHand) {
        List<String> currentRemainingCards = getRemainingCards(currentHand);
        List<String> bestRemainingCards = getRemainingCards(bestHand);
        return compareHighCard(currentRemainingCards, bestRemainingCards);
    }

    // 获取手牌中剩余的单牌
    private static List<String> getRemainingCards(List<String> hand) {
        Map<String, Integer> rankCount = new HashMap<>();
        for (String card : hand) {
            String rank = card.substring(1);
            rankCount.put(rank, rankCount.getOrDefault(rank, 0) + 1);
        }
        return hand.stream()
                .filter(card -> rankCount.get(card.substring(1)) == 1)
                .sorted((card1, card2) -> RANKS.indexOf(card2.substring(1)) - RANKS.indexOf(card1.substring(1)))
                .collect(Collectors.toList());
    }


    private static boolean isStraight(List<String> hand) {
        List<Integer> ranks = new ArrayList<>();
        for (String card : hand) {
            ranks.add(RANKS.indexOf(card.substring(1)));
        }
        Collections.sort(ranks);

        // 处理 A, 2, 3, 4, 5 顺子的特殊情况
        if (ranks.equals(Arrays.asList(0, 1, 2, 3, 12))) {
            return true;
        }

        for (int i = 0; i < ranks.size() - 1; i++) {
            if (ranks.get(i) + 1 != ranks.get(i + 1)) {
                return false;
            }
        }
        return true;
    }

    private static int getHighCardIndexForStraight(List<String> hand) {
        List<Integer> ranks = new ArrayList<>();
        for (String card : hand) {
            ranks.add(RANKS.indexOf(card.substring(1)));
        }
        Collections.sort(ranks);

        // 处理 A, 2, 3, 4, 5 顺子的特殊情况
        if (ranks.equals(Arrays.asList(0, 1, 2, 3, 12))) {
            return 3; // 5 是最高牌
        }
        return ranks.get(ranks.size() - 1);
    }


    public static Result getBestOfBestHands(List<Result> results) {
        Result bestResult = results.get(0);
        boolean tie = false;

        for (int i = 1; i < results.size(); i++) {
            if (results.get(i).handType.compareTo(bestResult.handType) > 0 ||
                    (results.get(i).handType == bestResult.handType && isBetterHand(results.get(i).bestHand, bestResult.bestHand))) {
                bestResult = results.get(i);
                tie = false; // 如果发现有更好的牌型，则不是平手
            } else if (results.get(i).handType == bestResult.handType && !isBetterHand(results.get(i).bestHand, bestResult.bestHand) &&
                    !isBetterHand(bestResult.bestHand, results.get(i).bestHand)) {
                tie = true; // 如果两手牌相等，则设置为平手
            }
        }

        bestResult.isTie = tie;
        return bestResult;
    }

    public static boolean areHandsEqual(List<String> hand1, List<String> hand2) {
        if (hand1.size() != hand2.size()) {
            return false;
        }

        List<String> sortedHand1 = new ArrayList<>(hand1);
        List<String> sortedHand2 = new ArrayList<>(hand2);

        Collections.sort(sortedHand1);
        Collections.sort(sortedHand2);

        return sortedHand1.equals(sortedHand2);
    }


    enum HandType {
        HIGH_CARD,
        ONE_PAIR,
        TWO_PAIR,
        THREE_OF_A_KIND,
        STRAIGHT,
        FLUSH,
        FULL_HOUSE,
        FOUR_OF_A_KIND,
        STRAIGHT_FLUSH,
        ROYAL_FLUSH
    }

    static class Result {
        List<String> bestHand;
        String handTypeName;
        HandType handType;
        boolean isTie;


        Result(List<String> bestHand, String handTypeName, boolean isTie) {
            this.bestHand = bestHand;
            this.handTypeName = handTypeName;
            this.handType = getHandTypeByName(handTypeName);
            this.isTie = isTie;
        }

        private HandType getHandTypeByName(String handTypeName) {
            for (Map.Entry<HandType, String> entry : HAND_TYPE_NAMES.entrySet()) {
                if (entry.getValue().equals(handTypeName)) {
                    return entry.getKey();
                }
            }
            return HandType.HIGH_CARD; // default case
        }
    }
}



