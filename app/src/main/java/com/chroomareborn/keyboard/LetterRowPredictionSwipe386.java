package com.chroomareborn.keyboard;

import java.util.List;

/**
 * Research 386: prediction selection gestures originating on actual letter
 * keys, never on the suggestion strip. No glide-to-spell, editor access,
 * touch logging, network or persistent pointer traces.
 */
final class LetterRowPredictionSwipe386 {
    static final String PREF="prediction_letter_row_swipe_mode_386";
    static final int OFF=0,TOP_ONLY=1,MIDDLE_ONLY=2,TOP_AND_MIDDLE=3;
    enum Direction { NONE,LEFT,RIGHT,DOWN,CANCEL }

    static int normalize(int mode){return mode>=OFF && mode<=TOP_AND_MIDDLE?mode:OFF;}
    static int next(int mode){
        switch(normalize(mode)){
            case OFF:return MIDDLE_ONLY;
            case MIDDLE_ONLY:return TOP_ONLY;
            case TOP_ONLY:return TOP_AND_MIDDLE;
            default:return OFF;
        }
    }
    static String label(int mode){
        switch(normalize(mode)){
            case MIDDLE_ONLY:return "ASDF middle row";
            case TOP_ONLY:return "QWERTY top row";
            case TOP_AND_MIDDLE:return "QWERTY + ASDF rows";
            default:return "OFF";
        }
    }
    static boolean enabledForRow(int mode,int rowIndex){
        int normalized=normalize(mode);
        return rowIndex==0 && (normalized==TOP_ONLY||normalized==TOP_AND_MIDDLE) ||
                rowIndex==1 && (normalized==MIDDLE_ONLY||normalized==TOP_AND_MIDDLE);
    }
    static String chosen(Direction direction,List<String> visible){
        if(visible==null || direction==null)return null;
        switch(direction){
            case DOWN:
                // Strict center slot: no fallback to an adjacent candidate.
                return visible.size()>1 && PredictionSwipe372.valid(visible.get(1))?
                        visible.get(1):null;
            case LEFT:
                return !visible.isEmpty() && PredictionSwipe372.valid(visible.get(0))?
                        visible.get(0):null;
            case RIGHT:
                for(int index=Math.min(2,visible.size()-1);index>=1;index--)
                    if(PredictionSwipe372.valid(visible.get(index)))
                        return visible.get(index);
                return null;
            default:return null;
        }
    }

    private final int pointer;
    private final float x0,y0,density;
    private Direction locked=Direction.NONE;
    private boolean active=true;
    LetterRowPredictionSwipe386(int pointerId,float x,float y,float density){
        if(pointerId<0||!Float.isFinite(x)||!Float.isFinite(y)||
                !Float.isFinite(density)||density<=0f)
            throw new IllegalArgumentException("Invalid letter-row touch");
        pointer=pointerId;x0=x;y0=y;this.density=density;
    }
    int pointerId(){return pointer;}
    boolean active(){return active;}
    Direction preview(){return active?locked:Direction.CANCEL;}
    void cancel(){active=false;}
    boolean move(int id,float x,float y){
        if(!active)return false;
        if(id!=pointer||!Float.isFinite(x)||!Float.isFinite(y)){
            active=false;return false;
        }
        float dx=x-x0,dy=y-y0;
        if(dy < -24f*density || dy>120f*density){
            active=false;return false;
        }
        Direction next=axis(dx,dy);
        if(next!=Direction.NONE){
            if(locked!=Direction.NONE && locked!=next){
                active=false;return false;
            }
            locked=next;
        }
        return true;
    }
    Direction release(int id,float x,float y){
        if(!move(id,x,y))return Direction.CANCEL;
        active=false;
        Direction finished=axis(x-x0,y-y0);
        return locked!=Direction.NONE && finished==locked?locked:Direction.NONE;
    }
    private Direction axis(float dx,float dy){
        float ax=Math.abs(dx),ay=Math.abs(dy);
        if(dy>=42f*density&&ay>=ax*1.55f)return Direction.DOWN;
        if(ax>=46f*density&&ax>=ay*1.65f)
            return dx<0f?Direction.LEFT:Direction.RIGHT;
        return Direction.NONE;
    }
}
