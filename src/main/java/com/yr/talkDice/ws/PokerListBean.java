package com.yr.talkDice.ws;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PokerListBean {

    private static PokerListBean pokerListBean;

    private List<String> pokerList;

    public PokerListBean() {
        pokerList = PokerDeck.shuffleDeck();
    }

    public static PokerListBean getInstance() {
        if (pokerListBean == null) {
            pokerListBean = new PokerListBean();
        }
        return pokerListBean;
    }

    public List<String> getPokerList() {
        return pokerList;
    }
}
