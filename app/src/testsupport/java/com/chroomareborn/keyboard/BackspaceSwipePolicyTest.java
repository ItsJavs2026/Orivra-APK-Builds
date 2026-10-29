package com.chroomareborn.keyboard;

public final class BackspaceSwipePolicyTest {
    private static void equal(int expected, int actual) {
        if (actual != expected) throw new AssertionError(
                "expected " + expected + " but got " + actual);
    }
    public static void main(String[] args) {
        equal(0, BackspaceSwipePolicy.wordsForSwipe(10, 0, 32, 75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(-55, 0, 32, 75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(55, 80, 32, 75));
        equal(1, BackspaceSwipePolicy.wordsForSwipe(35, 3, 32, 75));
        equal(2, BackspaceSwipePolicy.wordsForSwipe(107, 0, 32, 75));
        equal(8, BackspaceSwipePolicy.wordsForSwipe(1200, 0, 32, 75));
        equal(0, BackspaceSwipePolicy.charsToDelete("hello world", 0));
        equal(5, BackspaceSwipePolicy.charsToDelete("hello world", 1));
        equal(11, BackspaceSwipePolicy.charsToDelete("hello world", 2));
        equal(6, BackspaceSwipePolicy.charsToDelete("hello world ", 1));
        equal(4, BackspaceSwipePolicy.charsToDelete("a b ", 2));
        equal(4, BackspaceSwipePolicy.charsToDelete("test", 7));
        equal(0, BackspaceSwipePolicy.charsToDelete("", 2));
        equal(0, BackspaceSwipePolicy.charsToDelete(null, 2));
        equal(3, BackspaceSwipePolicy.charsToDelete("a 😊", 1));
        equal(5, BackspaceSwipePolicy.charsToDelete("a 😊", 2));
        System.out.println("BackspaceSwipePolicyTest passed");
    }
}
