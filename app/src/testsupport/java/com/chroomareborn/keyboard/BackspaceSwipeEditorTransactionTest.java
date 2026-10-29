package com.chroomareborn.keyboard;

import java.util.Random;

public final class BackspaceSwipeEditorTransactionTest {
    private static long assertions;
    private static void check(boolean yes,String reason) {
        assertions++;
        if(!yes)throw new AssertionError(reason);
    }
    /**
     * Local Android InputConnection-like model: text, insertion cursor,
     * selection, composition finalization, and host rejection/focus changes.
     * Never touches the actual keyboard, UI, or user data.
     */
    private static final class FakeEditor
            implements BackspaceSwipeEditorTransaction.Editor {
        final StringBuilder content;
        int start,end,deleteCalls,finishCalls,batches;
        boolean connected=true,allowFinish=true,allowDelete=true;
        boolean mutateOnFinish=false,moveCursorOnFinish=false;
        boolean switchSessionOnFinish=false,throwOnRead=false;
        FakeEditor(String text) {
            content=new StringBuilder(text);
            start=end=text.length();
        }
        void select(int from,int to) {start=from;end=to;}
        int left() {return Math.min(start,end);}
        int right() {return Math.max(start,end);}
        String text() {return content.toString();}
        @Override public boolean sameSession() {return connected;}
        @Override public boolean selectionExpected() {return start!=end;}
        @Override public CharSequence before(int limit) {
            if(throwOnRead)throw new IllegalStateException("host disconnected");
            int pos=left();
            return content.substring(Math.max(0,pos-limit),pos);
        }
        @Override public CharSequence selected() {
            return start==end?null:content.substring(left(),right());
        }
        @Override public CharSequence after(int limit) {
            int pos=right();
            return content.substring(pos,Math.min(content.length(),pos+limit));
        }
        @Override public void beginBatchEdit() {batches++;}
        @Override public boolean finishComposition() {
            finishCalls++;
            if(!allowFinish)return false;
            if(mutateOnFinish)content.append("!");
            if(moveCursorOnFinish)start=end=0;
            if(switchSessionOnFinish)connected=false;
            return true;
        }
        @Override public boolean deleteBeforeCursor(int length) {
            deleteCalls++;
            if(!allowDelete || length<0 || length>left())return false;
            content.delete(left()-length,left());
            start=end=left()-length;
            return true;
        }
        @Override public boolean deleteSelection() {
            deleteCalls++;
            if(!allowDelete || start==end)return false;
            int pos=left();
            content.delete(left(),right());
            start=end=pos;
            return true;
        }
        @Override public void endBatchEdit() {batches--;}
        BackspaceSwipeSnapshotGuard.Ticket ticket(int words) {
            return BackspaceSwipeSnapshotGuard.capture(
                    before(BackspaceSwipeSnapshotGuard.BEFORE_LIMIT),
                    selected(),after(BackspaceSwipeSnapshotGuard.AFTER_LIMIT),
                    selectionExpected(),words);
        }
    }
    private static BackspaceSwipeEditorTransaction.Result apply(
            FakeEditor editor,int words,String composing) {
        return BackspaceSwipeEditorTransaction.apply(
                editor,editor.ticket(words),words,composing);
    }
    private static void succeeds(String initial,String composed,int words) {
        FakeEditor editor=new FakeEditor(initial);
        BackspaceSwipeSnapshotGuard.Ticket ticket=editor.ticket(words);
        check(ticket!=null,"no deletion ticket for "+initial);
        int count=BackspaceSwipePolicy.safeCharsToDelete(initial,composed,words);
        String expected=initial.substring(0,initial.length()-count);
        check(BackspaceSwipeEditorTransaction.apply(editor,ticket,words,composed)
                ==BackspaceSwipeEditorTransaction.Result.APPLIED,
                "valid swipe rejected for "+initial);
        check(expected.equals(editor.text()),"wrong text after swipe");
        check(editor.deleteCalls==1,"swipe must commit exactly one delete");
        check(editor.finishCalls==1,"must finalize composition once");
        check(editor.batches==0,"batch not balanced");
    }
    private static void rejectedWithoutDelete(FakeEditor editor,int words,
            String composing,BackspaceSwipeSnapshotGuard.Ticket ticket) {
        int before=editor.deleteCalls;
        check(BackspaceSwipeEditorTransaction.apply(editor,ticket,words,composing)
                !=BackspaceSwipeEditorTransaction.Result.APPLIED,
                "unexpected deletion accepted");
        check(editor.deleteCalls==before,"stale editor still deleted text");
        check(editor.batches==0,"rejected batch not balanced");
    }
    public static void main(String[] args) {
        succeeds("likethis","likethis",1);
        succeeds("heythere","heythere",1);
        succeeds("likethis","",1); // committed text
        succeeds("hello likethis","likethis",1);
        succeeds("hello likethis","likethis",2);
        succeeds("hello world","",1);
        succeeds("中文 😊","😊",1);
        succeeds("foo\nlikethis","likethis",1);
        // Selection is removed without touching either neighboring phrase.
        FakeEditor selection=new FakeEditor("before like this after");
        selection.select(7,16);
        check(apply(selection,1,"")==BackspaceSwipeEditorTransaction.Result.APPLIED,
                "selection edit rejected");
        check("before  after".equals(selection.text()),
                "selected phrase deletion modified adjacent words");
        check(selection.batches==0 && selection.deleteCalls==1,
                "selection mutation count wrong");

        FakeEditor changed=new FakeEditor("likethis");
        BackspaceSwipeSnapshotGuard.Ticket t=changed.ticket(1);
        changed.content.append("!");
        rejectedWithoutDelete(changed,1,"likethis",t);

        FakeEditor cursor=new FakeEditor("likethis");
        t=cursor.ticket(1);
        cursor.select(3,3);
        rejectedWithoutDelete(cursor,1,"likethis",t);

        FakeEditor after=new FakeEditor("likethis tail");
        after.select(8,8);
        t=after.ticket(1);
        after.content.append("!");
        rejectedWithoutDelete(after,1,"likethis",t);

        FakeEditor alteredOnFinish=new FakeEditor("likethis");
        alteredOnFinish.mutateOnFinish=true;
        t=alteredOnFinish.ticket(1);
        rejectedWithoutDelete(alteredOnFinish,1,"likethis",t);
        check(alteredOnFinish.finishCalls==1,
                "finish-composing drift was not tested");

        FakeEditor movedOnFinish=new FakeEditor("likethis");
        movedOnFinish.moveCursorOnFinish=true;
        t=movedOnFinish.ticket(1);
        rejectedWithoutDelete(movedOnFinish,1,"likethis",t);

        FakeEditor switched=new FakeEditor("likethis");
        switched.switchSessionOnFinish=true;
        t=switched.ticket(1);
        rejectedWithoutDelete(switched,1,"likethis",t);

        FakeEditor unavailable=new FakeEditor("likethis");
        unavailable.allowFinish=false;
        t=unavailable.ticket(1);
        rejectedWithoutDelete(unavailable,1,"likethis",t);

        FakeEditor failedDelete=new FakeEditor("likethis");
        failedDelete.allowDelete=false;
        t=failedDelete.ticket(1);
        check(BackspaceSwipeEditorTransaction.apply(failedDelete,t,1,"likethis")
                ==BackspaceSwipeEditorTransaction.Result.REJECTED,
                "rejected host deletion must not report success");
        check("likethis".equals(failedDelete.text()) && failedDelete.batches==0,
                "rejected deletion mutated text or left batch active");

        FakeEditor thrown=new FakeEditor("likethis");
        t=thrown.ticket(1);
        thrown.throwOnRead=true;
        rejectedWithoutDelete(thrown,1,"likethis",t);

        FakeEditor invalid=new FakeEditor("likethis");
        rejectedWithoutDelete(invalid,1,"likethis",null);

        Random random=new Random(29280013L);
        String[] tokens={"likethis","heythere","x","one","two","world!",
                "中文","café","😊","👍🏽","12345","abc_def"};
        String[] separators={" ","  ","\n","\r\n","\t"};
        for(int i=0;i<30000;i++) {
            StringBuilder phrase=new StringBuilder();
            int prefixCount=random.nextInt(10);
            for(int j=0;j<prefixCount;j++) {
                phrase.append(tokens[random.nextInt(tokens.length)])
                        .append(separators[random.nextInt(separators.length)]);
            }
            String last=tokens[random.nextInt(tokens.length)];
            phrase.append(last);
            String initial=phrase.toString();
            int words=1+random.nextInt(8);
            succeeds(initial,last,words);
            if(i%3==0)succeeds(initial,"",words);
            // A changed draft or cursor within the same editor must abort.
            if(i%5==0) {
                FakeEditor stale=new FakeEditor(initial);
                BackspaceSwipeSnapshotGuard.Ticket ticket=stale.ticket(words);
                stale.select(Math.max(0,stale.left()-1),
                        Math.max(0,stale.left()-1));
                rejectedWithoutDelete(stale,words,last,ticket);
            }
        }
        System.out.println("BackspaceSwipeEditorTransactionTest passed: 30k "+
                "seeded composing/committed cases, "+assertions+" assertions");
    }
}
