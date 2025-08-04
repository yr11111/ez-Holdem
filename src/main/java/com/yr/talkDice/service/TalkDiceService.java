package com.yr.talkDice.service;

import com.yr.talkDice.utils.RandomArrayGenerator;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class TalkDiceService {

    public static String getTalkDiceStings() {

        int[] ints = RandomArrayGenerator.generateRandomArray();

        while (!RandomArrayGenerator.hasDuplicates(ints)) {
            ints = RandomArrayGenerator.generateRandomArray();
        }

        Arrays.sort(ints);

        StringBuilder result = new StringBuilder();
        for (int anInt : ints) {

            result.append("       [").append(anInt).append("]");
        }
        return result.toString();
    }
}
