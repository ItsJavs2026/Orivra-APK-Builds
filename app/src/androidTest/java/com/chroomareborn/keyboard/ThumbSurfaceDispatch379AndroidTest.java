package com.chroomareborn.keyboard;

import android.content.Context;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research 379: dispatches actual multi-pointer Android MotionEvents through
 * measured ViewGroup + adjacent Buttons, not directly to policy alone.
 * Confirms parent event splitting and the existing ordering/pointer guard
 * contract. Does NOT instantiate InputMethodService or phone hardware.
 * No editor reads, keystroke logs or persisted touch locations.
 */
@RunWith(AndroidJUnit4.class)
public final class ThumbSurfaceDispatch379AndroidTest {
    private static final class Press implements View.OnTouchListener {
        final HintKeyPressOrder353 order;
        final StringBuilder typed;
        final String letter;
        ThumbTapGuard377 guard;
        long token=-1L;
        int pointerId=-1;

        Press(HintKeyPressOrder353 order,StringBuilder typed,String letter) {
            this.order=order; this.typed=typed; this.letter=letter;
        }
        @Override public boolean onTouch(View view,MotionEvent event) {
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN) {
                int index=event.getActionIndex();
                pointerId=event.getPointerId(index);
                token=order.begin();
                guard=new ThumbTapGuard377(pointerId,view.getWidth(),
                        view.getHeight(),event.getX(index),event.getY(index),
                        1f);
                return true;
            }
            if(action==MotionEvent.ACTION_MOVE) {
                int index=event.findPointerIndex(pointerId);
                if(guard!=null) {
                    if(index<0)guard.cancel();
                    else guard.move(pointerId,event.getX(index),
                            event.getY(index));
                }
                return true;
            }
            if(action==MotionEvent.ACTION_POINTER_DOWN)return true;
            if(action==MotionEvent.ACTION_UP ||
                    action==MotionEvent.ACTION_POINTER_UP) {
                if(event.getPointerId(event.getActionIndex())!=pointerId)
                    return true;
                int index=event.findPointerIndex(pointerId);
                boolean accepted=guard!=null && index>=0 &&
                        guard.release(pointerId,event.getX(index),
                                event.getY(index));
                order.complete(token,accepted?()->typed.append(letter):null);
                token=-1L;
                guard=null;
                return true;
            }
            if(action==MotionEvent.ACTION_CANCEL) {
                if(guard!=null)guard.cancel();
                order.cancel(token);
                token=-1L;
                guard=null;
                return true;
            }
            return true;
        }
    }

    private static final class Harness {
        final LinearLayout row;
        final HintKeyPressOrder353 order=new HintKeyPressOrder353();
        final StringBuilder typed=new StringBuilder();
        final Press left, right;
        final long epoch=SystemClock.uptimeMillis();
        long offset=0L;

        Harness() {
            Context context=InstrumentationRegistry.getInstrumentation()
                    .getTargetContext();
            row=new LinearLayout(context);
            row.setMotionEventSplittingEnabled(true);
            row.setOrientation(LinearLayout.HORIZONTAL);
            left=new Press(order,typed,"a");
            right=new Press(order,typed,"b");
            Button first=button(context,left),second=button(context,right);
            row.addView(first,new LinearLayout.LayoutParams(200,88));
            row.addView(second,new LinearLayout.LayoutParams(200,88));
            int width=View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY);
            int height=View.MeasureSpec.makeMeasureSpec(88,View.MeasureSpec.EXACTLY);
            row.measure(width,height);
            row.layout(0,0,400,88);
            assertEquals(200,first.getWidth());
            assertEquals(200,second.getWidth());
        }
        private static Button button(Context context,Press press) {
            Button b=new Button(context);
            b.setMinWidth(0);b.setMinimumWidth(0);
            b.setMinHeight(0);b.setMinimumHeight(0);
            b.setPadding(0,0,0,0);
            b.setOnTouchListener(press);
            return b;
        }
        boolean send(int action,int[] ids,float[] xs) {
            assertEquals(ids.length,xs.length);
            MotionEvent.PointerProperties[] props=
                    new MotionEvent.PointerProperties[ids.length];
            MotionEvent.PointerCoords[] points=
                    new MotionEvent.PointerCoords[ids.length];
            for(int i=0;i<ids.length;i++){
                props[i]=new MotionEvent.PointerProperties();
                props[i].id=ids[i];
                props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
                points[i]=new MotionEvent.PointerCoords();
                points[i].x=xs[i];points[i].y=44f;
                points[i].pressure=1f;points[i].size=1f;
            }
            MotionEvent event=MotionEvent.obtain(epoch,epoch+(++offset)*16,
                    action,ids.length,props,points,0,0,1f,1f,
                    0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
            try{return row.dispatchTouchEvent(event);}
            finally {event.recycle();}
        }
        void twoDown(){
            assertTrue(send(MotionEvent.ACTION_DOWN,new int[]{0},new float[]{40}));
            assertTrue(send(MotionEvent.ACTION_POINTER_DOWN|
                    (1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0,1},new float[]{40,340}));
            assertTrue(order.hasOpenPresses());
        }
        void upRightFirst(){
            assertTrue(send(MotionEvent.ACTION_POINTER_UP|
                    (1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0,1},new float[]{40,340}));
            assertTrue(send(MotionEvent.ACTION_UP,new int[]{0},new float[]{40}));
        }
    }
    private static void onUi(Runnable action){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    @Test public void reverseLiftAcrossTwoRealAndroidButtonsPreservesDownOrder(){
        onUi(()->{
            Harness h=new Harness();
            h.twoDown();
            assertEquals("",h.typed.toString());
            assertTrue(h.send(MotionEvent.ACTION_POINTER_UP|
                    (1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0,1},new float[]{40,340}));
            assertEquals("",h.typed.toString());
            assertTrue(h.order.hasOpenPresses());
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{40}));
            assertEquals("ab",h.typed.toString());
            assertFalse(h.order.hasOpenPresses());
        });
    }
    @Test public void normalLiftAcrossTwoRealAndroidButtonsTypesExactlyOnce(){
        onUi(()->{
            Harness h=new Harness();
            h.twoDown();
            assertTrue(h.send(MotionEvent.ACTION_POINTER_UP,
                    new int[]{0,1},new float[]{40,340}));
            assertEquals("a",h.typed.toString());
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{1},new float[]{340}));
            assertEquals("ab",h.typed.toString());
        });
    }
    @Test public void slightDriftCrossesCellBoundaryButStaysWithOriginalKey(){
        onUi(()->{
            Harness h=new Harness();
            assertTrue(h.send(MotionEvent.ACTION_DOWN,new int[]{0},new float[]{190}));
            assertTrue(h.send(MotionEvent.ACTION_MOVE,new int[]{0},new float[]{211}));
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{211}));
            assertEquals("a",h.typed.toString());
        });
    }
    @Test public void largerCrossKeySlideDoesNotCommitOriginalOrNeighbor(){
        onUi(()->{
            Harness h=new Harness();
            assertTrue(h.send(MotionEvent.ACTION_DOWN,new int[]{0},new float[]{190}));
            assertTrue(h.send(MotionEvent.ACTION_MOVE,new int[]{0},new float[]{238}));
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{190}));
            assertEquals("",h.typed.toString());
            assertFalse(h.order.hasOpenPresses());
        });
    }
    @Test public void androidCancelCannotProduceGhostLetterAndNewTapWorks(){
        onUi(()->{
            Harness h=new Harness();
            assertTrue(h.send(MotionEvent.ACTION_DOWN,new int[]{0},new float[]{40}));
            assertTrue(h.send(MotionEvent.ACTION_CANCEL,new int[]{0},new float[]{40}));
            assertEquals("",h.typed.toString());
            assertFalse(h.order.hasOpenPresses());
            assertTrue(h.send(MotionEvent.ACTION_DOWN,new int[]{0},new float[]{40}));
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{40}));
            assertEquals("a",h.typed.toString());
        });
    }
    @Test public void editorSessionQueueRevocationDiscardsOldRelease(){
        onUi(()->{
            Harness h=new Harness();
            assertTrue(h.send(MotionEvent.ACTION_DOWN,new int[]{0},new float[]{40}));
            h.order.cancelAll();
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{40}));
            assertEquals("",h.typed.toString());
            assertFalse(h.order.hasOpenPresses());
        });
    }
    @Test public void weightedEdgeLettersOwnWiderMeasuredCells(){
        onUi(()->{
            Context ctx=InstrumentationRegistry.getInstrumentation().getTargetContext();
            LinearLayout row=new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Button[] keys=new Button[10];
            for(int i=0;i<10;i++) {
                keys[i]=new Button(ctx);
                keys[i].setMinWidth(0);keys[i].setMinimumWidth(0);
                keys[i].setMinHeight(0);keys[i].setMinimumHeight(0);
                keys[i].setPadding(0,0,0,0);
                row.addView(keys[i],new LinearLayout.LayoutParams(0,72,
                        EdgeKeyWeights378.weight(0,i,10)));
            }
            row.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(72,View.MeasureSpec.EXACTLY));
            row.layout(0,0,1000,72);
            assertTrue(keys[0].getWidth()>keys[1].getWidth());
            assertTrue(keys[9].getWidth()>keys[8].getWidth());
            assertEquals(1000,keys[9].getRight());
            for(Button key:keys)assertTrue(key.getWidth()>0);
        });
    }
}
