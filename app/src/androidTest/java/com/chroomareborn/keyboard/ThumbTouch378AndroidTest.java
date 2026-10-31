package com.chroomareborn.keyboard;

import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public final class ThumbTouch378AndroidTest {
    private ThumbTapGuard377 key(int id,float x,float y){
        return new ThumbTapGuard377(id,40f,72f,x,y,1f);
    }
    @Test public void smallEndPointOvershootStillCommitsOnce(){
        ThumbTapGuard377 g=key(0,39,35);
        MotionEvent up=MotionEvent.obtain(0L,20L,MotionEvent.ACTION_UP,52f,35f,0);
        try{
            assertTrue(g.release(up.getPointerId(0),up.getX(),up.getY()));
            assertFalse(g.release(0,39f,35f));
        }finally{up.recycle();}
    }
    @Test public void deliberateSlideAndReentryNeverCommit(){
        ThumbTapGuard377 g=key(1,5,35);
        assertFalse(g.move(1,53f,35f));
        assertFalse(g.release(1,6f,35f));
    }
    @Test public void wrongPointerAndCancelFailClosed(){
        ThumbTapGuard377 g=key(1,20,35);
        assertFalse(g.move(2,20,35));
        assertFalse(g.release(1,20,35));
        g=key(1,20,35);g.cancel();
        assertFalse(g.release(1,20,35));
    }
    @Test public void invalidCoordinateNeverCommits(){
        assertFalse(key(1,20,35).release(1,Float.NaN,35));
    }
    @Test public void verticalDriftHasConservativeLimit(){
        assertTrue(key(1,20,71).release(1,20,88));
        assertFalse(key(1,20,71).release(1,20,92));
    }
    @Test public void independentThumbsCommitInDownOrderDespiteReverseLift(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        ThumbTapGuard377 left=key(1,10,35),right=key(2,30,35);
        long leftToken=q.begin(),rightToken=q.begin();
        StringBuilder out=new StringBuilder();
        assertTrue(right.release(2,31,35));
        q.complete(rightToken,()->out.append("b"));
        assertEquals("",out.toString());
        assertTrue(left.release(1,11,35));
        q.complete(leftToken,()->out.append("a"));
        assertEquals("ab",out.toString());
        assertFalse(q.hasOpenPresses());
    }
    @Test public void cancelOneThumbLeavesOtherConfirmedLetter(){
        HintKeyPressOrder353 q=new HintKeyPressOrder353();
        long first=q.begin(),second=q.begin();
        StringBuilder out=new StringBuilder();
        q.complete(second,()->out.append('b'));
        q.cancel(first);
        assertEquals("b",out.toString());
    }
    @Test public void edgeWeightsKeepEachRowWidthAndTouchCellsPositive(){
        int[] columns={10,9,7};
        for(int row=0;row<3;row++){
            float sum=0;
            for(int col=0;col<columns[row];col++){
                float width=EdgeKeyWeights378.weight(row,col,columns[row]);
                assertTrue(width>0f);
                sum+=width;
            }
            assertEquals((float)columns[row],sum,.0001f);
        }
    }
}
