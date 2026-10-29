package com.chroomareborn.keyboard;

/**
 * Coalesces bursty 120Hz+ spacebar motion into small bounded cursor moves.
 * Only counts are retained: never user text, keystrokes, or editor contents.
 * Supports direction reversals without allowing integer overflow or a backlog
 * of hundreds of editor IPC calls.
 */
final class CursorSwipeBatch {
    static final int MAX_PENDING=32;
    static final int MAX_FRAME_STEPS=8;
    private int pending;

    void offer(int steps) {
        long next=(long)pending + steps;
        pending=(int)Math.max(-MAX_PENDING,Math.min(MAX_PENDING,next));
    }

    int takeFrame() {
        int take=Math.max(-MAX_FRAME_STEPS,Math.min(MAX_FRAME_STEPS,pending));
        pending-=take;
        return take;
    }

    int takeFinal() {
        int take=pending;
        pending=0;
        return take;
    }

    boolean hasPending() {return pending!=0;}

    void clear() {pending=0;}
}
