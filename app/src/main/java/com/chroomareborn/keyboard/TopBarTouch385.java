package com.chroomareborn.keyboard;

/**
 * Research 385: candidate-strip hit-target geometry and conservative,
 * opt-in prediction swipe recognizer. No editor, keyboard, network, storage
 * or persistent input data. Never changes prediction rank or text.
 */
final class TopBarTouch385 {
    static final String PREF="thumb_topbar_touch_385";

    static int normalHeightDp(boolean enabled,boolean phrase) {
        return enabled?48:phrase?48:44;
    }
    static int normalVerticalMarginDp(boolean enabled) {
        return enabled?0:2;
    }
    static int normalHorizontalMarginDp(boolean enabled) {
        return enabled?1:2;
    }
    static int riseHeightDp(boolean enabled){return enabled?64:60;}
    static int riseVerticalMarginDp(boolean enabled){return enabled?0:1;}
    static int riseHorizontalMarginDp(boolean enabled){return enabled?1:2;}

    enum Decision { NONE,TAP,LEFT,RIGHT,UP,CANCEL }
    private final int owner;
    private final float downX,downY,density;
    private float maxDx,maxDy;
    private boolean active=true;
    private Decision locked=Decision.NONE;

    TopBarTouch385(int pointerId,float x,float y,float pixelDensity){
        if(pointerId<0 || !finite(x) || !finite(y) ||
                !finite(pixelDensity) || pixelDensity<=0f)
            throw new IllegalArgumentException("Invalid top-bar pointer");
        owner=pointerId;downX=x;downY=y;density=pixelDensity;
    }
    int pointerId(){return owner;}
    boolean active(){return active;}
    Decision preview(){return active?locked:Decision.CANCEL;}
    void cancel(){active=false;}

    boolean move(int pointerId,float x,float y,boolean riseVisible){
        if(!active)return false;
        if(pointerId!=owner || !finite(x) || !finite(y)){
            active=false;
            return false;
        }
        float dx=x-downX,dy=y-downY;
        maxDx=Math.max(maxDx,Math.abs(dx));
        maxDy=Math.max(maxDy,Math.abs(dy));
        Decision next=axis(dx,dy,riseVisible);
        if(next!=Decision.NONE){
            if(locked!=Decision.NONE && locked!=next){
                active=false; // no direction flip or accidental re-entry
                return false;
            }
            locked=next;
        }
        return true;
    }
    Decision release(int pointerId,float x,float y,boolean riseVisible){
        if(!move(pointerId,x,y,riseVisible))return Decision.CANCEL;
        float dx=x-downX,dy=y-downY;
        active=false;
        if(locked!=Decision.NONE) {
            // A substantial swipe that returns near origin must not become
            // a tap, and a reversed/diagonal lift must not select a chip.
            return axis(dx,dy,riseVisible)==locked?
                    locked:Decision.CANCEL;
        }
        float tap=18f*density;
        return maxDx<=tap && maxDy<=tap &&
                Math.abs(dx)<=tap && Math.abs(dy)<=tap ?
                Decision.TAP:Decision.CANCEL;
    }
    private Decision axis(float dx,float dy,boolean riseVisible) {
        float horizontal=34f*density;
        float upward=40f*density;
        float ax=Math.abs(dx),ay=Math.abs(dy);
        if(ax>=horizontal && ax>=ay*1.4f)
            return dx<0f?Decision.LEFT:Decision.RIGHT;
        if(riseVisible && dy<=-upward && ay>=ax*1.4f)
            return Decision.UP;
        return Decision.NONE;
    }
    private static boolean finite(float f){return Float.isFinite(f);}
}
