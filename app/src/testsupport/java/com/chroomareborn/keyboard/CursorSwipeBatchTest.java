package com.chroomareborn.keyboard;

/** Fast-swipe regression: coalescing and cancellation must be bounded. */
public final class CursorSwipeBatchTest {
    private static int checks;
    private static void expect(boolean pass,String name) {
        checks++;
        if(!pass)throw new AssertionError(name);
    }

    public static void main(String[] args) {
        CursorSwipeBatch batch=new CursorSwipeBatch();
        expect(!batch.hasPending(),"initial empty");
        batch.offer(1);
        batch.offer(2);
        batch.offer(4);
        expect(batch.takeFrame()==7,"coalesce events into one editor call");
        expect(!batch.hasPending(),"drains normally");

        batch.offer(10000);
        expect(batch.takeFrame()==8,"never flood editor on one frame");
        expect(batch.takeFrame()==8,"bounded second frame");
        expect(batch.takeFinal()==16,"remaining movement bounded");
        expect(!batch.hasPending(),"final drains queue");

        batch.offer(-10000);
        expect(batch.takeFrame()==-8,"left swipe frame limited");
        expect(batch.takeFinal()==-24,"left pending bounded");

        batch.offer(20);
        batch.offer(-18);
        expect(batch.takeFinal()==2,"reversal nets pending steps");

        batch.offer(Integer.MAX_VALUE);
        batch.offer(Integer.MAX_VALUE);
        expect(batch.takeFinal()==32,"positive overflow saturated");
        batch.offer(Integer.MIN_VALUE);
        batch.offer(Integer.MIN_VALUE);
        expect(batch.takeFinal()==-32,"negative overflow saturated");

        batch.offer(25);
        batch.clear();
        expect(batch.takeFinal()==0,"editor change discards stale gestures");

        SpacebarCursorGesture gesture=new SpacebarCursorGesture(12,16,28);
        // Simulates 2,000 rapidly delivered MOVE events on each side.
        // No editor calls happen until the frame scheduler drains the queue.
        for(int round=0;round<4;round++) {
            gesture.start(200,30);
            for(int i=0;i<2000;i++) {
                float x=round%2==0 ? 200+i*4f : 200-i*4f;
                batch.offer(gesture.move(x,30));
                expect(batch.takeFinal()<=32,"synthetic movement bounded");
                // Offer a new burst again to stress coalescing / reversal.
            }
            expect(!gesture.finish(),"fast swipe never inserts spaces");
            batch.clear();
        }
        gesture.start(200,30);
        for(int i=0;i<2000;i++)
            batch.offer(gesture.move(200+i*4f,30));
        int frames=0;
        while(batch.hasPending()) {
            int step=batch.takeFrame();
            expect(Math.abs(step)<=8,"per-frame budget");
            frames++;
            expect(frames<=4,"maximum pending uses at most four frames");
        }
        expect(!gesture.finish(),"bounded burst does not type text");
        System.out.println("CURSOR_SWIPE_BURST checks="+checks+" passed");
    }
}
