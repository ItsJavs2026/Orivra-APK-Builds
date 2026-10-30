package com.chroomareborn.keyboard;

public final class FinalComposingDelete338Test {
    private static int assertions;
    private static void check(boolean ok,String label) {
        assertions++;if(!ok)throw new AssertionError(label);
    }
    static final class FakeEditor implements FinalComposingDelete338.Editor {
        String text;
        boolean session=true,replaceAccepted=true,mutateOnFalse=false,
                finishAccepted=true,deleteAccepted=true;
        int setCalls,finishCalls,deleteCalls;
        FakeEditor(String initial){text=initial;}
        @Override public boolean sameSession(){return session;}
        @Override public CharSequence before(){return text;}
        @Override public boolean replaceComposingWithEmpty(){
            setCalls++;
            if(replaceAccepted || mutateOnFalse)
                text=text.substring(0,text.length()-1);
            return replaceAccepted;
        }
        @Override public boolean finishComposing(){finishCalls++;return finishAccepted;}
        @Override public boolean deleteOneCodePoint(){
            deleteCalls++;
            if(deleteAccepted && !text.isEmpty())
                text=text.substring(0,text.length()-1);
            return deleteAccepted;
        }
    }
    public static void main(String[] args) {
        // Replays the user screenshot: asking -> askin -> ... -> a -> "".
        String text="Where you at lol Greg over here asking";
        String prefix="Where you at lol Greg over here ";
        for(int n="asking".length();n>=1;n--) {
            if(n>1)text=text.substring(0,text.length()-1);
            else {
                FakeEditor editor=new FakeEditor(text);
                check(FinalComposingDelete338.apply(editor,"a")==
                        FinalComposingDelete338.Result.APPLIED,
                        "one Backspace removes last composing character");
                check(prefix.equals(editor.text),
                        "last a really removed, not just finalized");
                check(editor.setCalls==1 && editor.finishCalls==0 &&
                        editor.deleteCalls==0,
                        "correct setComposingText-empty path; never finish only");
            }
        }
        FakeEditor rejected=new FakeEditor("reply a");
        rejected.replaceAccepted=false;
        check(FinalComposingDelete338.apply(rejected,"a")==
                FinalComposingDelete338.Result.APPLIED,
                "custom editor safe fallback deletes one");
        check("reply ".equals(rejected.text),"fallback text correct");
        check(rejected.finishCalls==1&&rejected.deleteCalls==1,
                "one finalization and one actual delete");
        FakeEditor falseButEdited=new FakeEditor("reply a");
        falseButEdited.replaceAccepted=false;
        falseButEdited.mutateOnFalse=true;
        check(FinalComposingDelete338.apply(falseButEdited,"a")==
                FinalComposingDelete338.Result.APPLIED,
                "false-returning editor still edits; detect rather than retry");
        check(falseButEdited.deleteCalls==0,
                "never double delete on false-returning mutator");
        FakeEditor noDelete=new FakeEditor("reply a");
        noDelete.replaceAccepted=false;
        noDelete.deleteAccepted=false;
        check(FinalComposingDelete338.apply(noDelete,"a")==
                FinalComposingDelete338.Result.REJECTED,
                "rejected underlying delete is never called success");
        check("reply a".equals(noDelete.text),"rejection preserves text");
        FakeEditor badSnapshot=new FakeEditor("reply b");
        check(FinalComposingDelete338.apply(badSnapshot,"a")==
                FinalComposingDelete338.Result.STALE,
                "never delete an unrelated final character");
        check(badSnapshot.setCalls==0,"stale editor never mutated");
        FakeEditor detached=new FakeEditor("reply a");
        detached.session=false;
        check(FinalComposingDelete338.apply(detached,"a")==
                FinalComposingDelete338.Result.REJECTED,
                "editor session must match");
        check(detached.setCalls==0,"detached editor not mutated");
        System.out.println("PASS FinalComposingDelete338Test "+assertions+" checks");
    }
}
