package com.chroomareborn.keyboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Offline randomized letter-row gesture and Space-compatible commit QA. */
public final class LetterRowPredictionSwipe386Research {
    private static long checks;
    private static void ok(boolean yes,String message) {
        checks++;
        if(!yes)throw new AssertionError(message);
    }
    private static void modes(){
        int[] sequence={0,2,1,3,0};
        int current=0;
        for(int i=1;i<sequence.length;i++){
            current=LetterRowPredictionSwipe386.next(current);
            ok(current==sequence[i],"A/B mode order");
        }
        ok(LetterRowPredictionSwipe386.enabledForRow(2,1),"ASDF enabled");
        ok(!LetterRowPredictionSwipe386.enabledForRow(2,0),"QWERTY disabled in ASDF mode");
        ok(LetterRowPredictionSwipe386.enabledForRow(1,0),"QWERTY enabled");
        ok(!LetterRowPredictionSwipe386.enabledForRow(1,1),"ASDF disabled in QWERTY mode");
        ok(LetterRowPredictionSwipe386.enabledForRow(3,0) &&
                LetterRowPredictionSwipe386.enabledForRow(3,1),"both rows");
        ok(!LetterRowPredictionSwipe386.enabledForRow(3,2),"never ZXC row");
        ok(LetterRowPredictionSwipe386.normalize(-1)==0,"unknown setting fails off");
    }
    private static void center(){
        List<String> options=Arrays.asList("there","the","them");
        ok("the".equals(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,options)),
                "down forces physical center");
        ok("there".equals(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.LEFT,options)),"left chip");
        ok("them".equals(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.RIGHT,options)),"right chip");
        ok(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,
                Arrays.asList("there","","them"))==null,"blank center fails closed");
        ok(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,
                Collections.singletonList("there"))==null,"no center means no commit");
        ok("the".equals(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,
                Arrays.asList("there","the",""))),"no side fallback");
    }
    private static void centerOnlyRegressions(){
        List<String> single=Arrays.asList("","a","");
        ok(LetterRowPredictionSwipe386.hasSelectable(single),
                "Center-only suggestion must arm a letter-row gesture");
        ok("a".equals(LetterRowPredictionSwipe386.chosen(
                LetterRowPredictionSwipe386.Direction.DOWN,single)),
                "DOWN must choose center-only word");
        ok(!LetterRowPredictionSwipe386.hasSelectable(
                Arrays.asList("","","")), "Empty strip fails closed");
        ok(!LetterRowPredictionSwipe386.hasSelectable(null),
                "Missing strip fails closed");
        Random rng=new Random(38720261030L);
        for(int i=0;i<30000;i++){
            final String center=(i%4==0?"a":i%4==1?"I":i%4==2?"te":"t");
            List<String> options=Arrays.asList("",center,"");
            ok(LetterRowPredictionSwipe386.hasSelectable(options),
                    "Center-only accepted for all short prefixes");
            LetterRowPredictionSwipe386 g=
                    new LetterRowPredictionSwipe386(i%11,100f,30f,1f);
            int down=43+rng.nextInt(60);
            ok(g.release(i%11,100f,30f+down)==
                    LetterRowPredictionSwipe386.Direction.DOWN,
                    "Explicit DOWN recognizes only-center");
            ok(center.equals(LetterRowPredictionSwipe386.chosen(
                    LetterRowPredictionSwipe386.Direction.DOWN,options)),
                    "DOWN returns exact existing center slot");
        }
    }
    private static void directionalAndCancellation(){
        LetterRowPredictionSwipe386 down=new LetterRowPredictionSwipe386(2,30,20,1);
        ok(down.release(2,31,68)==LetterRowPredictionSwipe386.Direction.DOWN,
                "42dp+ vertical down");
        LetterRowPredictionSwipe386 borderline=new LetterRowPredictionSwipe386(3,30,20,1);
        ok(borderline.release(3,30,60)==LetterRowPredictionSwipe386.Direction.NONE,
                "40dp down is normal not triggered");
        LetterRowPredictionSwipe386 right=new LetterRowPredictionSwipe386(1,40,20,1);
        ok(right.release(1,100,20)==LetterRowPredictionSwipe386.Direction.RIGHT,
                "horizontal right");
        LetterRowPredictionSwipe386 left=new LetterRowPredictionSwipe386(1,100,20,1);
        ok(left.release(1,40,20)==LetterRowPredictionSwipe386.Direction.LEFT,
                "horizontal left");
        LetterRowPredictionSwipe386 reverse=new LetterRowPredictionSwipe386(0,50,30,1);
        ok(reverse.move(0,110,30),"first direction valid");
        ok(!reverse.move(0,50,86),"change to down cancels");
        ok(reverse.release(0,50,86)==LetterRowPredictionSwipe386.Direction.CANCEL,
                "cannot reverse into down commit");
        LetterRowPredictionSwipe386 other=new LetterRowPredictionSwipe386(0,30,20,1);
        ok(!other.move(1,30,65),"second pointer cancels");
        ok(other.release(0,30,65)==LetterRowPredictionSwipe386.Direction.CANCEL,
                "no stale pointer commit");
    }
    private static class FakeEditor implements SentenceSuggestionCommit.Editor {
        final String prefix,original,suffix;
        String output;
        int commitCount;
        int batchDepth;
        FakeEditor(String prefix,String original,String suffix){
            this.prefix=prefix;this.original=original;this.suffix=suffix;
        }
        @Override public CharSequence before(int n){
            String value=prefix+original;
            return value.substring(Math.max(0,value.length()-n));
        }
        @Override public CharSequence after(int n){
            return suffix.substring(0,Math.min(suffix.length(),n));
        }
        @Override public void beginBatch(){batchDepth++;}
        @Override public void endBatch(){batchDepth--;}
        @Override public boolean commit(String value){
            commitCount++;output=prefix+value+suffix;return true;
        }
    }
    private static void assertSingleCommit(String typed,String chosen,String suffix,
            String expected){
        FakeEditor editor=new FakeEditor("",typed,suffix);
        ok(SentenceSuggestionCommit.insert(editor,typed,chosen)==
                SentenceSuggestionCommit.Result.APPLIED,"single safe editor commit");
        ok(expected.equals(editor.output),"correct spacing for '"+typed+"'");
        ok(editor.commitCount==1 && editor.batchDepth==0,
                "never commit correction then another literal Space");
    }
    private static void shortWords(){
        assertSingleCommit("t","the","","the ");
        assertSingleCommit("te","tell","","tell ");
        assertSingleCommit("a","and","","and ");
        assertSingleCommit("a","a","","a ");
        assertSingleCommit("I","I will","","I will ");
        assertSingleCommit("te","tell"," ","tell ");
        assertSingleCommit("te","tell",",","tell,");
        assertSingleCommit("t","the",".","the.");
        assertSingleCommit("te","tell","!","tell!");
        assertSingleCommit("te","tell",")","tell)");
        assertSingleCommit("a","and","now","and now");
        FakeEditor stale=new FakeEditor("","t","");
        ok(SentenceSuggestionCommit.insert(stale,"te","tell")==
                SentenceSuggestionCommit.Result.STALE,"changed prefix is stale");
        ok(stale.commitCount==0,"stale prefix does not type");
        // Space key highlighted-completion eligibility is still unchanged.
        boolean[] args={true,true,true,true,true,false,false,false,false,false,false,false};
        ok(HighlightedSpace372.eligible("t","t","the",
                true,true,true,true,true,false,false,false,false,false,false,false),
                "one-letter prefix eligible on normal Space key");
        ok(HighlightedSpace372.eligible("te","te","tell",
                true,true,true,true,true,false,false,false,false,false,false,false),
                "two-letter prefix eligible on normal Space key");
        ok(HighlightedSpace372.eligible("a","a","and",
                true,true,true,true,true,false,false,false,false,false,false,false),
                "short a-prefix eligible");
        ok(!HighlightedSpace372.eligible("a","a","a",
                true,true,true,true,true,false,false,false,false,false,false,false),
                "unchanged word does not auto-complete on Space key");
    }
    private static void randomized(){
        Random rng=new Random(38620261030L);
        for(int i=0;i<100000;i++){
            int ptr=i%7;
            LetterRowPredictionSwipe386 gesture=new LetterRowPredictionSwipe386(
                    ptr,110f,25f,1f);
            float lateral=(rng.nextBoolean()?1f:-1f)*(48f+rng.nextInt(65));
            if(i%4==0){
                ok(gesture.release(ptr,110f,25f+45f+rng.nextInt(65))==
                        LetterRowPredictionSwipe386.Direction.DOWN,
                        "random down commit");
            } else if(i%4==1){
                ok(gesture.release(ptr,110f+lateral,25f)==
                        (lateral<0?
                                LetterRowPredictionSwipe386.Direction.LEFT:
                                LetterRowPredictionSwipe386.Direction.RIGHT),
                        "random left/right");
            } else if(i%4==2){
                ok(gesture.release(ptr,110f,25f+1f+rng.nextInt(31))==
                        LetterRowPredictionSwipe386.Direction.NONE,
                        "short movement not auto-completed");
            } else {
                ok(gesture.move(ptr,110f+lateral,25f),"initial swipe");
                ok(gesture.release(ptr,110f+1f,25f)==
                        LetterRowPredictionSwipe386.Direction.NONE,
                        "return to origin cannot commit");
            }
        }
    }
    public static void main(String[] args){
        modes();center();centerOnlyRegressions();
        directionalAndCancellation();shortWords();randomized();
        System.out.println("PASS LetterRowPredictionSwipe386 "+checks+
                " assertions across 130000 gesture cases + center-only short-word Space rules");
    }
}
