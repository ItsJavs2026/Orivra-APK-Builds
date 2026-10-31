package com.chroomareborn.keyboard;

/**
 * Research 395: zero-allocation fast paths for already-canonical, stored
 * learning records. No cache, mutation, network, session text, disk I/O or
 * hidden learning. Legacy/noncanonical frequencies ALWAYS fall through to
 * the original strict validator rather than being silently discarded.
 */
final class FastStoredLearningPolicy395 {
    private FastStoredLearningPolicy395(){}

    /**
     * Only return true when the original strict prefix eligibility check
     * CANNOT succeed. Valid imported frequency keys are already canonical
     * ASCII lower words, so most irrelevant entries skip expensive regex.
     * For weird/corrupt/legacy entries never guess: use the old slow path.
     */
    static boolean definitelyNotPrefix(String normalizedPrefix,String storedWord){
        if(normalizedPrefix==null || normalizedPrefix.isEmpty() ||
                storedWord==null || storedWord.length()<2 ||
                storedWord.length()>28)return false;
        for(int i=0;i<storedWord.length();i++){
            char ch=storedWord.charAt(i);
            if((ch>='a' && ch<='z') || ch=='\'')continue;
            return false;
        }
        return storedWord.length()<=normalizedPrefix.length() ||
                !storedWord.startsWith(normalizedPrefix);
    }

    /**
     * Equivalent to target.equals(AcceptedContextPolicy319.acceptedWord(target)).
     * The old validator admits only ASCII A-Z and internal straight
     * apostrophes and returns a lowercase canonical string. No curly quote,
     * uppercase, spaces, digits, trailing quote or doubled quote can match
     * the original equality test.
     */
    static boolean canonicalContextTarget(String target) {
        if(target==null || target.length()<2 || target.length()>26)return false;
        boolean priorLetter=false;
        int letters=0;
        for(int i=0;i<target.length();i++){
            char ch=target.charAt(i);
            if(ch>='a' && ch<='z') {
                letters++;
                priorLetter=true;
            } else if(ch=='\'' && priorLetter && i+1<target.length() &&
                    target.charAt(i+1)>='a' && target.charAt(i+1)<='z') {
                priorLetter=false;
            } else return false;
        }
        return priorLetter && letters>=2;
    }
}
