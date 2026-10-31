package com.chroomareborn.keyboard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Research 383: real Android main Looper and EditText InputConnection.
 * No InputMethodService, production integration, user text, telemetry, or
 * external storage. Editor text is fictional and confined to a test object.
 */
@RunWith(AndroidJUnit4.class)
public final class DeferredEditorAction383AndroidTest {
    private static final class Target {
        final EditText view;
        final InputConnection connection;
        Target(Context context,String initial){
            view=new EditText(context);
            view.setText(initial);
            view.setSelection(view.length());
            EditorInfo info=new EditorInfo();
            connection=view.onCreateInputConnection(info);
            assertNotNull("EditText must return a real InputConnection",connection);
        }
        String text(){return view.getText().toString();}
    }
    private static final class Scenario {
        final Handler handler=new Handler(Looper.getMainLooper());
        final MixedThumbOrder380 order=new MixedThumbOrder380();
        final AtomicReference<Throwable> error;
        Target active;
        long generation=1L;
        Runnable verify;

        Scenario(AtomicReference<Throwable> error){
            this.error=error;
            active=new Target(InstrumentationRegistry.getInstrumentation()
                    .getTargetContext(),"");
        }
        void resetText(String value){
            active.view.setText(value);
            active.view.setSelection(active.view.length());
        }
        void post(Runnable action){
            if(!handler.post(()->{
                try{action.run();}
                catch(Throwable throwable){error.compareAndSet(null,throwable);}
            }))throw new AssertionError("Main Looper refused callback");
        }
        void accepted(long token,Target captured,long recordedGeneration,
                      Runnable operation){
            order.up(token,()->post(()->{
                if(active==captured && generation==recordedGeneration)
                    operation.run();
            }));
        }
        void assertText(String expected){assertEquals(expected,active.text());}
        void setVerify(Runnable assertion){verify=assertion;}
    }
    private static void scenario(Consumer<Scenario> steps) throws Exception {
        final CountDownLatch completed=new CountDownLatch(1);
        final AtomicReference<Throwable> error=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try {
                Scenario s=new Scenario(error);
                steps.accept(s);
                if(s.verify==null)throw new AssertionError("Missing final assertion");
                s.handler.post(()->{
                    try{s.verify.run();}
                    catch(Throwable t){error.compareAndSet(null,t);}
                    finally {completed.countDown();}
                });
            }catch(Throwable t){
                error.compareAndSet(null,t);
                completed.countDown();
            }
        });
        assertTrue("Android main queue did not drain",
                completed.await(15,TimeUnit.SECONDS));
        if(error.get()!=null)
            throw new AssertionError("Android editor-action research failed",error.get());
    }

    @Test public void naiveTouchOnlyBarrierCanReorderActualInputConnectionWrites()
            throws Exception {
        scenario(s->{
            Target editor=s.active;
            long a=s.order.down(),space=s.order.down();
            s.order.up(space,()->assertTrue(editor.connection.commitText(" ",1)));
            s.order.up(a,()->s.post(()->assertTrue(
                    editor.connection.commitText("a",1))));
            s.setVerify(()->s.assertText(" a"));
        });
    }
    @Test public void queuedLetterThenSpaceCommitsInOriginalPressOrder()
            throws Exception {
        scenario(s->{
            Target editor=s.active;
            long gen=s.generation;
            long letter=s.order.down(),space=s.order.down();
            s.accepted(space,editor,gen,()->assertTrue(
                    editor.connection.commitText(" ",1)));
            s.accepted(letter,editor,gen,()->assertTrue(
                    editor.connection.commitText("a",1)));
            s.setVerify(()->{
                s.assertText("a ");
                assertFalse(s.order.hasOpenPresses());
            });
        });
    }
    @Test public void threeKeysReverseLiftStillWriteInPressOrder()
            throws Exception {
        scenario(s->{
            Target e=s.active; long gen=s.generation;
            long a=s.order.down(),space=s.order.down(),b=s.order.down();
            s.accepted(b,e,gen,()->assertTrue(e.connection.commitText("b",1)));
            s.accepted(space,e,gen,()->assertTrue(e.connection.commitText(" ",1)));
            s.accepted(a,e,gen,()->assertTrue(e.connection.commitText("a",1)));
            s.setVerify(()->s.assertText("a b"));
        });
    }
    @Test public void correctionMustInspectFinalTextAtDeferredExecution()
            throws Exception {
        scenario(s->{
            s.resetText("k");
            Target e=s.active;long gen=s.generation;
            long letter=s.order.down(),space=s.order.down();
            s.accepted(space,e,gen,()->{
                CharSequence before=e.connection.getTextBeforeCursor(2,0);
                assertEquals("ke",before==null?null:before.toString());
                assertTrue(e.connection.deleteSurroundingText(2,0));
                assertTrue(e.connection.commitText("keyboard ",1));
            });
            s.accepted(letter,e,gen,()->assertTrue(
                    e.connection.commitText("e",1)));
            s.setVerify(()->s.assertText("keyboard "));
        });
    }
    @Test public void fieldSwitchInvalidatesPreviouslyPostedEditorActions()
            throws Exception {
        scenario(s->{
            Target old=s.active;long gen=s.generation;
            long letter=s.order.down(),space=s.order.down();
            s.accepted(space,old,gen,()->assertTrue(
                    old.connection.commitText(" ",1)));
            s.accepted(letter,old,gen,()->assertTrue(
                    old.connection.commitText("a",1)));
            s.active=new Target(old.view.getContext(),"new");
            s.generation++;
            s.setVerify(()->{
                assertEquals("",old.text());
                s.assertText("new");
            });
        });
    }
    @Test public void canceledFirstLetterCannotBlockLaterSpace()
            throws Exception {
        scenario(s->{
            Target e=s.active;long gen=s.generation;
            long letter=s.order.down(),space=s.order.down();
            s.accepted(space,e,gen,()->assertTrue(e.connection.commitText(" ",1)));
            s.order.cancel(letter);
            s.setVerify(()->{
                s.assertText(" ");
                assertFalse(s.order.hasOpenPresses());
            });
        });
    }
    @Test public void cursorSwipeNeverEnqueuesLiteralSpace()
            throws Exception {
        scenario(s->{
            Target e=s.active;long gen=s.generation;
            long letter=s.order.down(),space=s.order.down();
            SpacebarCursorGesture g=new SpacebarCursorGesture(24f,18f,40f);
            g.start(30f,44f);
            g.move(88f,44f);
            assertTrue(g.isNavigating());
            assertFalse(g.finish());
            s.order.cancel(space);
            s.accepted(letter,e,gen,()->assertTrue(e.connection.commitText("a",1)));
            s.setVerify(()->s.assertText("a"));
        });
    }
    @Test public void duplicateFingerReleaseAndQueuedLongHoldWriteOnlyOnce()
            throws Exception {
        scenario(s->{
            Target e=s.active;long gen=s.generation;
            long letter=s.order.down(),space=s.order.down();
            s.accepted(space,e,gen,()->assertTrue(e.connection.commitText(" ",1)));
            s.accepted(letter,e,gen,()->assertTrue(e.connection.commitText("#",1)));
            s.accepted(letter,e,gen,()->assertTrue(e.connection.commitText("a",1)));
            s.setVerify(()->{
                s.assertText("# ");
                assertFalse(s.order.hasOpenPresses());
            });
        });
    }
}
