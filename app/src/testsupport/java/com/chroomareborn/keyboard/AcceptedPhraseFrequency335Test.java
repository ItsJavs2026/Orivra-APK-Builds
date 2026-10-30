package com.chroomareborn.keyboard;

import java.util.List;
public final class AcceptedPhraseFrequency335Test {
    private static int count;
    private static void ok(boolean yes,String msg){
        count++;if(!yes)throw new AssertionError(msg);
    }
    public static void main(String[] args) {
        ok(AcceptedPhraseFrequencyPolicy335.words("Please continue")
                .equals(List.of("please","continue")),
                "whole-sentence tap reinforces individual words");
        ok(AcceptedPhraseFrequencyPolicy335.words("I can continue")
                .equals(List.of("can","continue")),
                "one-letter I is skipped, other words retained");
        ok(AcceptedPhraseFrequencyPolicy335.words("a very good day")
                .equals(List.of("very","good","day")),
                "a is skipped without invalidating the rest");
        ok(AcceptedPhraseFrequencyPolicy335.words(
                "continue continue continuing")
                .equals(List.of("continue","continuing")),
                "same word only earns one reinforcement per tap");
        ok(AcceptedPhraseFrequencyPolicy335.words("Continue")
                .equals(List.of("continue")),
                "standalone word still supported");
        ok(AcceptedPhraseFrequencyPolicy335.words("can't stop")
                .equals(List.of("can't","stop")),
                "contractions remain individual words");
        ok(AcceptedPhraseFrequencyPolicy335.words("a's where")
                .equals(List.of("a's","where")),
                "valid single-token apostrophe remains eligible");
        ok(AcceptedPhraseFrequencyPolicy335.words(
                "my account@example.com").isEmpty(),
                "never learn email addresses from a chosen string");
        ok(AcceptedPhraseFrequencyPolicy335.words(
                "my 12345 balance").isEmpty(),
                "never learn numbers/financial data from a chosen string");
        ok(AcceptedPhraseFrequencyPolicy335.words(
                "please  continue").isEmpty(),
                "unexpected whitespace rejected");
        ok(AcceptedPhraseFrequencyPolicy335.words(
                "hello\ncontinue").isEmpty(),
                "multiple lines rejected");
        ok(AcceptedPhraseFrequencyPolicy335.words(
                "my private/path").isEmpty(),
                "URLs and paths rejected");
        ok(AcceptedPhraseFrequencyPolicy335.words(
                "please continue!").isEmpty(),
                "arbitrary punctuation requires separate reviewed parser");
        ok(AcceptedPhraseFrequencyPolicy335.words(null).isEmpty(),
                "null rejected");
        ok(AcceptedPhraseFrequencyPolicy335.words("I a").isEmpty(),
                "only one-letter words have no frequency signal");
        System.out.println("PASS AcceptedPhraseFrequency335Test "+count+" checks");
    }
}
