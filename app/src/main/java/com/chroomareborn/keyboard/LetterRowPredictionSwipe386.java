package com.chroomareborn.keyboard;

import java.util.List;

/**
 * Research 386: choose existing suggestion chips by an intentional
 * horizontal gesture started on a real QWERTY or ASDF letter key.
 *
 * Distinct from character glide-to-spell input and the prediction-strip
 * gesture. No input editor access, analytics, network or stored touch paths.
 */
final class LetterRowPredictionSwipe386 {
    static final String PREF="prediction_letter_row_swipe_mode_386";
    static final int OFF=0,TOP_ONLY=1,TOP_AND_MIDDLE=2;
    enum Direction { NONE, LEFT, RIGHT, CANCEL }

    static int normalize(int mode) {
        return mode==TOP_ONLY || mode==TOP_AND_MIDDLE ? mode:OFF;
    }
    static int next(int mode){return (normalize(mode)+1)%3;}
    static String label(int mode) {
        switch(normalize(mode)){
            case TOP_ONLY:return "QWERTY row";
            case TOP_AND_MIDDLE:return "QWERTY + ASDF rows";
            default:return "OFF";
        }
    }
    static boolean enabledForRow(int mode,int rowIndex) {
        return rowIndex==0 && normalize(mode)>=TOP_ONLY ||
                rowIndex==1 && normalize(mode)==TOP_AND_MIDDLE;
    }
    static String chosen(Direction direction,List<String> visible){
        if(visible==null || direction==null)return null;
        if(direction==Direction.LEFT) {
            if(!visible.isEmpty() && PredictionSwipe372.valid(visible.get(0)))
                return visible.get(0);
            return null;
        }
        if(direction==Direction.RIGHT) {
            for(int index=Math.min(2,visible.size()-1);index>=1;index--){
                String value=visible.get(index);
                if(PredictionSwipe372.valid(value))return value;
            }
        }
        return null;
    }

    private final int pointer;
    private final float x0,y0,density;
    private float farX,farY;
    private Direction locked=Direction.NONE;
    private boolean active=true;
    LetterRowPredictionSwipe386(int pointerId,float x,float y,float density){
        if(pointerId<0 || !Float.isFinite(x)||!Float.isFinite(y)||
                !Float.isFinite(density)||density<=0)
            throw new IllegalArgumentException("Invalid letter-row pointer");
        pointer=pointerId;x0=x;y0=y;this.density=density;
    }
    int pointerId(){return pointer;}
    boolean active(){return active;}
    void cancel(){active=false;}
    boolean move(int id,float x,float y){
        if(!active)return false;
        if(id!=pointer || !Float.isFinite(x) || !Float.isFinite(y)) {
            active=false;
            return false;
        }
        float dx=x-x0,dy=y-y0,ax=Math.abs(dx),ay=Math.abs(dy);
        farX=Math.max(farX,ax);
        farY=Math.max(farY,ay);
        if(ay>24f*density){
            active=false;return false;
        }
        Direction current=axis(dx,dy);
        if(current!=Direction.NONE) {
            if(locked!=Direction.NONE && locked!=current){
                active=false;return false;
            }
            locked=current;
        }
        return true;
    }
    Direction release(int id,float x,float y){
        if(!move(id,x,y))return Direction.CANCEL;
        active=false;
        Direction finish=axis(x-x0,y-y0);
        return locked!=Direction.NONE && locked==finish ?
                locked:Direction.NONE;
    }
    private Direction axis(float dx,float dy){
        float ax=Math.abs(dx),ay=Math.abs(dy);
        if(ax<46f*density || ax<ay*1.65f)return Direction.NONE;
        return dx<0?Direction.LEFT:Direction.RIGHT;
    }
}
