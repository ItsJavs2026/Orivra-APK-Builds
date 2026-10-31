package com.chroomareborn.keyboard;

/**
 * Pure-Java contract tests: selection transaction itself cannot touch
 * neighboring characters or retry after a remote false result.
 */
public final class SelectedBackspace416BTest {
    private static int checks;
    private static void expect(boolean ok,String message) {
        checks++;
        if(!ok)throw new AssertionError(message);
    }
    private static final class Editor implements SelectedBackspace416B.Editor {
        StringBuilder text;
        int begin,end,commits;
        boolean sameSession=true, returnsTrue=true, apply=true, throwOnCommit,
                disconnectAtCommit;
        Editor(String text,int begin,int end) {
            this.text=new StringBuilder(text);
            this.begin=begin;this.end=end;
        }
        @Override public boolean sameSession(){return sameSession;}
        @Override public boolean selectionKnown(){return begin!=end;}
        @Override public boolean replaceSelectionOnce(){
            commits++;
            if(throwOnCommit)throw new IllegalStateException("editor detached");
            if(apply) {
                int lo=Math.min(begin,end), hi=Math.max(begin,end);
                text.delete(lo,hi);begin=end=lo;
            }
            if(disconnectAtCommit)sameSession=false;
            return returnsTrue;
        }
    }
    private static SelectedBackspace416B.Outcome run(Editor e) {
        return SelectedBackspace416B.apply(e);
    }
    public static void main(String[] args) {
        Editor selected=new Editor("before SELECTED after",7,15);
        expect(run(selected)==SelectedBackspace416B.Outcome.APPLIED,"selection applied");
        expect("before  after".equals(selected.text.toString()),"neighbor text preserved");
        expect(selected.commits==1,"one commit");
        expect(selected.begin==7&&selected.end==7,"caret at replacement");
        Editor reversed=new Editor("first kitten last",12,6);
        expect(run(reversed)==SelectedBackspace416B.Outcome.APPLIED,"reversed range");
        expect("first  last".equals(reversed.text.toString()),"reverse deletion exact");
        Editor huge=new Editor("START "+"word ".repeat(2000)+"END",6,10006);
        expect(run(huge)==SelectedBackspace416B.Outcome.APPLIED,"very long selection");
        expect("START END".equals(huge.text.toString()),"whole long selection removed");
        expect(huge.commits==1,"no per-character loop");
        Editor emoji=new Editor("left 🥰👍🏽 right",5,12);
        expect(run(emoji)==SelectedBackspace416B.Outcome.APPLIED,"emoji selection");
        expect("left  right".equals(emoji.text.toString()),"unicode preserved");
        Editor noSelection=new Editor("one two",4,4);
        expect(run(noSelection)==SelectedBackspace416B.Outcome.NOT_SELECTED,"no selected text");
        expect(noSelection.commits==0&&"one two".equals(noSelection.text.toString()),
                "no mutation when not selected");
        Editor disconnected=new Editor("one two",0,3);
        disconnected.sameSession=false;
        expect(run(disconnected)==SelectedBackspace416B.Outcome.STALE,
                "stale editor");
        expect(disconnected.commits==0,"never edit stale session");
        Editor rejected=new Editor("one two",0,3);
        rejected.apply=false;rejected.returnsTrue=false;
        expect(run(rejected)==SelectedBackspace416B.Outcome.UNCERTAIN,
                "remote reject reported uncertain");
        expect(rejected.commits==1,"no fallback after reject");
        expect("one two".equals(rejected.text.toString()),"no collateral changes");
        Editor falseApplied=new Editor("one two",0,3);
        falseApplied.returnsTrue=false;
        expect(run(falseApplied)==SelectedBackspace416B.Outcome.UNCERTAIN,
                "false-but-applied preserved as uncertain");
        expect(" two".equals(falseApplied.text.toString()),"false can still apply");
        expect(falseApplied.commits==1,"no duplicate delete after false");
        Editor detached=new Editor("one two",0,3);
        detached.throwOnCommit=true;
        expect(run(detached)==SelectedBackspace416B.Outcome.UNCERTAIN,
                "exception stops mutation");
        expect(detached.commits==1,"no replay on error");
        Editor moved=new Editor("one two",0,3);
        moved.disconnectAtCommit=true;
        expect(run(moved)==SelectedBackspace416B.Outcome.STALE,
                "editor switched during mutation");
        expect(moved.commits==1,"no second editor edited");
        expect(run(null)==SelectedBackspace416B.Outcome.STALE,
                "null editor");
        System.out.println("SelectedBackspace416BTest PASS ("+checks+" checks)");
    }
}
