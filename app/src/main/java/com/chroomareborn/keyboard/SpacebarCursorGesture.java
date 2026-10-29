package com.chroomareborn.keyboard;

/**
 * Pure-Java recognizer for Gboard-style spacebar cursor swipes.
 * It never edits text. One tap commits a space on release; horizontal
 * movement above the activation threshold consumes the gesture instead.
 *
 * A hold may show a hint but does not commit or move the cursor by itself.
 * Fractional movement is accumulated, so slowly reversing direction works.
 */
final class SpacebarCursorGesture {
    private final float activationPx, pixelsPerCharacter, verticalCancelPx;
    private float downX, downY, lastStepX;
    private boolean touching, navigating, cancelled, held;

    SpacebarCursorGesture(float activationPx, float pixelsPerCharacter,
                           float verticalCancelPx) {
        if (!Float.isFinite(activationPx) || !Float.isFinite(pixelsPerCharacter)
                || !Float.isFinite(verticalCancelPx)
                || activationPx <= 0 || pixelsPerCharacter <= 0
                || verticalCancelPx <= 0)
            throw new IllegalArgumentException("Invalid spacebar gesture scale");
        this.activationPx=activationPx;
        this.pixelsPerCharacter=pixelsPerCharacter;
        this.verticalCancelPx=verticalCancelPx;
    }

    void start(float x,float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y))
            throw new IllegalArgumentException("Invalid starting coordinates");
        touching=true;
        navigating=false;
        cancelled=false;
        held=false;
        downX=lastStepX=x;
        downY=y;
    }

    boolean hold() {
        if (!touching || cancelled || navigating)return false;
        held=true;
        return true;
    }

    int move(float x,float y) {
        if (!touching || cancelled || !Float.isFinite(x) || !Float.isFinite(y))
            return 0;
        float dx=x-downX,dy=y-downY;
        if (!navigating) {
            if (Math.abs(dy)>=verticalCancelPx &&
                    Math.abs(dy)>Math.abs(dx)*1.35f) {
                cancelled=true;
                return 0;
            }
            if (Math.abs(dx)<activationPx ||
                    Math.abs(dx)<Math.abs(dy)*1.25f) return 0;
            navigating=true;
        }
        float accumulated=x-lastStepX;
        int steps=(int)(accumulated/pixelsPerCharacter);
        if (steps!=0)lastStepX+=steps*pixelsPerCharacter;
        return steps;
    }

    /** True means insert one space; never true after a recognized swipe. */
    boolean finish() {
        boolean insert=touching && !cancelled && !navigating;
        cancel();
        return insert;
    }

    void cancel() {
        touching=false;
        navigating=false;
        cancelled=false;
        held=false;
    }

    boolean isTouching(){return touching;}
    boolean isNavigating(){return touching && navigating;}
    boolean isHeld(){return touching && held;}
    boolean isCancelled(){return touching && cancelled;}
}
