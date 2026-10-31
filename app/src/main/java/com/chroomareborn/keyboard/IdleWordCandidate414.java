package com.chroomareborn.keyboard;

/**
 * Selects an already locally typed current word for a deferred learning
 * observation. No InputConnection reads and no content persistence.
 */
final class IdleWordCandidate414 {
    private IdleWordCandidate414(){}

    static String current(String composing,String lastTyped,
            boolean typedSessionCurrent,boolean eligible) {
        if(!eligible)return "";
        if(composing!=null && !composing.isEmpty())return composing;
        return typedSessionCurrent && lastTyped!=null?lastTyped:"";
    }
}
