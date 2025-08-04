package com.yr.talkDice.utils;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RandomArrayGenerator {

    /**
     * 生成1到6之间的随机数数组
     */
    public static int[] generateRandomArray() {
        Random random = new Random();
        int[] array = new int[5];

        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextInt(6) + 1;
        }

        return array;
    }

    /**
     * 判断是否有重复元素
     *
     * @param array
     * @return
     */
    public static boolean hasDuplicates(int[] array) {
        Set<Integer> seenElements = new HashSet<>();

        for (int num : array) {
            if (seenElements.contains(num)) {
                return true; // 有重复元素
            }
            seenElements.add(num);
        }

        return false; // 无重复元素
    }

}
