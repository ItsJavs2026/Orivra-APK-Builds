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
 * Research 381: real Android ViewGroup split-pointer dispatch, but FAKE editor
 * and isolated experimental MixedThumbOrder380 policy. No InputMethodService,
 * no host editor, no Samsung touch claims, no user text or persisted traces.
 */
@RunWith(AndroidJUnit4.class)
public final class MixedLetterSpace381AndroidTest {
    private static final class Press implements View.OnTouchListener {
        private final MixedThumbOrder380 order;
        private final StringBuilder output;
        private final boolean space;
        private long token=-1;
        private int owner=-1;
        private ThumbTapGuard377 guard;
        private SpacebarCursorGesture cursor;

        Press(MixedThumbOrder380 order,StringBuilder output,boolean space){
            this.order=order;this.output=output;this.space=space;
        }
        @Override public boolean onTouch(View view,MotionEvent event){
            final int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN) {
                owner=event.getPointerId(event.getActionIndex());
                token=order.down();
                if(space){
                    cursor=new SpacebarCursorGesture(24f,18f,40f);
                    cursor.start(event.getX(),event.getY());
                }else{
                    guard=new ThumbTapGuard377(owner,view.getWidth(),
                            view.getHeight(),event.getX(),event.getY(),1f);
                }
                return true;
            }
            if(action==MotionEvent.ACTION_MOVE){
                int index=event.findPointerIndex(owner);
                if(index<0){
                    if(space && cursor!=null)cursor.cancel();
                    if(!space && guard!=null)guard.cancel();
                }else if(space && cursor!=null){
                    cursor.move(event.getX(index),event.getY(index));
                }else if(guard!=null){
                    guard.move(owner,event.getX(index),event.getY(index));
                }
                return true;
            }
            if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP){
                if(owner!=event.getPointerId(event.getActionIndex()))
                    return true;
                int index=event.findPointerIndex(owner);
                if(space){
                    if(cursor!=null && index>=0)cursor.move(event.getX(index),event.getY(index));
                    final boolean accept=index>=0 && cursor!=null && cursor.finish();
                    order.up(token,accept?()->output.append(' '):null);
                }else{
                    final boolean accept=index>=0 && guard!=null &&
                            guard.release(owner,event.getX(index),event.getY(index));
                    order.up(token,accept?()->output.append('a'):null);
                }
                cursor=null;guard=null;token=-1;
                return true;
            }
            if(action==MotionEvent.ACTION_CANCEL){
                if(cursor!=null)cursor.cancel();
                if(guard!=null)guard.cancel();
                order.cancel(token);
                token=-1;guard=null;cursor=null;
                return true;
            }
            return true;
        }
    }
    private static final class Harness {
        final LinearLayout row;
        final MixedThumbOrder380 order=new MixedThumbOrder380();
        final StringBuilder output=new StringBuilder();
        final long start=SystemClock.uptimeMillis();
        long tick;
        Harness(){
            Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            row=new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setMotionEventSplittingEnabled(true);
            row.addView(button(context,false),new LinearLayout.LayoutParams(200,88));
            row.addView(button(context,true),new LinearLayout.LayoutParams(200,88));
            row.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(88,View.MeasureSpec.EXACTLY));
            row.layout(0,0,400,88);
            assertEquals(400,row.getWidth());
        }
        private Button button(Context ctx,boolean space){
            Button button=new Button(ctx);
            button.setMinHeight(0);button.setMinimumHeight(0);
            button.setMinWidth(0);button.setMinimumWidth(0);
            button.setPadding(0,0,0,0);
            button.setOnTouchListener(new Press(order,output,space));
            return button;
        }
        boolean send(int action,int[] ids,float[] xs){
            assertEquals(ids.length,xs.length);
            MotionEvent.PointerProperties[] properties=
                    new MotionEvent.PointerProperties[ids.length];
            MotionEvent.PointerCoords[] coordinates=
                    new MotionEvent.PointerCoords[ids.length];
            for(int i=0;i<ids.length;i++){
                properties[i]=new MotionEvent.PointerProperties();
                properties[i].id=ids[i];
                properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
                coordinates[i]=new MotionEvent.PointerCoords();
                coordinates[i].x=xs[i];coordinates[i].y=44f;
                coordinates[i].pressure=1f;coordinates[i].size=1f;
            }
            MotionEvent e=MotionEvent.obtain(start,start+(++tick)*16,
                    action,ids.length,properties,coordinates,
                    0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
            try{return row.dispatchTouchEvent(e);}finally{e.recycle();}
        }
        boolean downLetter(){return send(MotionEvent.ACTION_DOWN,new int[]{0},new float[]{40});}
        boolean downSpaceFirst(){return send(MotionEvent.ACTION_DOWN,new int[]{1},new float[]{340});}
        boolean addSpace(){
            return send(MotionEvent.ACTION_POINTER_DOWN|
                    (1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0,1},new float[]{40,340});
        }
        boolean addLetter(){
            return send(MotionEvent.ACTION_POINTER_DOWN|
                    (1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{1,0},new float[]{340,40});
        }
        boolean releaseSpaceFirst(){
            return send(MotionEvent.ACTION_POINTER_UP|
                    (1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0,1},new float[]{40,340});
        }
        boolean releaseLetterFirst(){
            return send(MotionEvent.ACTION_POINTER_UP,new int[]{0,1},
                    new float[]{40,340});
        }
    }
    private static void ui(Runnable work){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(work);
    }
    @Test public void spaceLiftFirstWaitsForEarlierLetter(){
        ui(()->{
            Harness h=new Harness();
            assertTrue(h.downLetter());assertTrue(h.addSpace());
            assertTrue(h.releaseSpaceFirst());
            assertEquals("",h.output.toString());
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{40}));
            assertEquals("a ",h.output.toString());
            assertFalse(h.order.hasOpenPresses());
        });
    }
    @Test public void ordinaryLetterThenSpaceLiftsInSameOrder(){
        ui(()->{
            Harness h=new Harness();
            assertTrue(h.downLetter());assertTrue(h.addSpace());
            assertTrue(h.releaseLetterFirst());
            assertEquals("a",h.output.toString());
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{1},new float[]{340}));
            assertEquals("a ",h.output.toString());
        });
    }
    @Test public void deliberateSpaceCursorDragNeverInsertsSpace(){
        ui(()->{
            Harness h=new Harness();
            assertTrue(h.downLetter());assertTrue(h.addSpace());
            assertTrue(h.send(MotionEvent.ACTION_MOVE,
                    new int[]{0,1},new float[]{40,395}));
            assertTrue(h.send(MotionEvent.ACTION_POINTER_UP|
                    (1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0,1},new float[]{40,395}));
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{40}));
            assertEquals("a",h.output.toString());
        });
    }
    @Test public void spacePressedFirstRemainsBeforeLaterLetter(){
        ui(()->{
            Harness h=new Harness();
            assertTrue(h.downSpaceFirst());assertTrue(h.addLetter());
            assertTrue(h.send(MotionEvent.ACTION_POINTER_UP|
                    (1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{1,0},new float[]{340,40}));
            assertEquals("",h.output.toString());
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{1},new float[]{340}));
            assertEquals(" a",h.output.toString());
        });
    }
    @Test public void cancellationClearsTouchTransactions(){
        ui(()->{
            Harness h=new Harness();
            assertTrue(h.downLetter());assertTrue(h.addSpace());
            assertTrue(h.send(MotionEvent.ACTION_CANCEL,
                    new int[]{0,1},new float[]{40,340}));
            assertEquals("",h.output.toString());
            assertFalse(h.order.hasOpenPresses());
            assertTrue(h.downLetter());
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{40}));
            assertEquals("a",h.output.toString());
        });
    }
    @Test public void slightLetterDriftStillOrdersBeforeSpace(){
        ui(()->{
            Harness h=new Harness();
            assertTrue(h.send(MotionEvent.ACTION_DOWN,new int[]{0},new float[]{190}));
            // Keep the first pointer at its actual original x=190 when
            // delivering the second finger's DOWN. A synthetic coordinate
            // jump to x=40 here would falsely cancel the thumb guard.
            assertTrue(h.send(MotionEvent.ACTION_POINTER_DOWN|
                    (1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    new int[]{0,1},new float[]{190,340}));
            assertTrue(h.send(MotionEvent.ACTION_MOVE,new int[]{0,1},new float[]{211,340}));
            assertTrue(h.releaseSpaceFirst());
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{211}));
            assertEquals("a ",h.output.toString());
        });
    }
    @Test public void oldSessionActionsNeverReachFakeEditor(){
        ui(()->{
            Harness h=new Harness();
            assertTrue(h.downLetter());assertTrue(h.addSpace());
            assertTrue(h.releaseSpaceFirst());
            h.order.cancelAll();
            assertTrue(h.send(MotionEvent.ACTION_UP,new int[]{0},new float[]{40}));
            assertEquals("",h.output.toString());
        });
    }
}
