package com.yr.talkDice.domain;

import lombok.Data;

import java.util.List;

@Data
public class Player {

    private String name;

    private int chip;

    private int currChip;

    private int watch;

    private int order;

    private int fold;

    private List<String> pokerList;
}
