package com.chroomareborn.keyboard;

/**
 * Research-only, pure Java letter-key touch guard for broad-thumb drift.
 *
 * A key owns only the pointer that began on its existing full-cell target.
 * A small excursion beyond that target is tolerated, but a deliberate slide,
 * invalid coordinate, wrong pointer, cancellation, or stale second release
 * never commits. Once canceled, a re-entry cannot resurrect a tap.
 *
 * This policy has NO Android or InputConnection dependency, no text storage,
 * no prediction, no dynamic key resizing, and is NOT wired into the IME.
 */
final class ThumbTapGuard377 {
    static final String PREF="fat_thumb_drift_378";
    private final int pointerId;
    private final float width, height, downX, downY;
    private final float horizontalGrace, verticalGrace;
    private final float maxHorizontalTravel, maxVerticalTravel;
    private boolean active;

    ThumbTapGuard377(int pointerId, float widthPx, float heightPx,
                     float startX, float startY, float density) {
        if (pointerId < 0 || !positive(widthPx) || !positive(heightPx) ||
                !positive(density) || !finite(startX) || !finite(startY) ||
                startX < 0f || startY < 0f ||
                startX > widthPx || startY > heightPx)
            throw new IllegalArgumentException("Invalid initial touch bounds");
        this.pointerId=pointerId;
        width=widthPx;
        height=heightPx;
        downX=startX;
        downY=startY;
        horizontalGrace=Math.min(14f*density,0.38f*width);
        verticalGrace=Math.min(18f*density,0.28f*height);
        maxHorizontalTravel=Math.min(0.80f*width,52f*density);
        maxVerticalTravel=Math.min(0.65f*height,60f*density);
        active=true;
    }

    /** Only dispatch MOVE events for the pointer that started this key. */
    boolean move(int pointer, float x, float y) {
        if (!active) return false;
        if (pointer!=pointerId || !finite(x) || !finite(y) ||
                x < -horizontalGrace || x > width+horizontalGrace ||
                y < -verticalGrace || y > height+verticalGrace ||
                Math.abs(x-downX)>maxHorizontalTravel ||
                Math.abs(y-downY)>maxVerticalTravel) {
            active=false;
        }
        return active;
    }

    /** Validate even if Android batched movement and only supplied an UP. */
    boolean release(int pointer,float x,float y) {
        boolean commit=move(pointer,x,y);
        active=false;
        return commit;
    }

    void cancel(){active=false;}
    boolean isActive(){return active;}
    private static boolean finite(float f){return Float.isFinite(f);}
    private static boolean positive(float f){return finite(f)&&f>0f;}
}
