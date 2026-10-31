package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Offline parity on the actual packaged PUBLIC phrase corpus. */
@RunWith(AndroidJUnit4.class)
public final class PublicPhraseIndex397AndroidTest {
    private static String[] phrases()throws Exception {
        Field f=PredictionLite.class.getDeclaredField("COMMON_PHRASES");
        f.setAccessible(true);
        return ((String[])f.get(null)).clone();
    }
    private static PublicPhraseIndex397 current()throws Exception {
        Field f=PredictionLite.class.getDeclaredField("PUBLIC_PHRASES_397");
        f.setAccessible(true);
        return (PublicPhraseIndex397)f.get(null);
    }
    private static List<String> strictStarting(String[] corpus,String word){
        ArrayList<String> original=new ArrayList<>();
        for(String phrase:corpus){
            String[] parts=phrase.split(" ");
            if(parts.length<2||parts.length>5)continue;
            if(!parts[0].replaceAll("[^A-Za-z]","").equalsIgnoreCase(word))
                continue;
            original.add(phrase);
        }
        return original;
    }
    @Test public void publicCorpusIndexedWithoutChangingCompactOrOrderedPrefix()throws Exception {
        String[] corpus=phrases();
        PublicPhraseIndex397 index=current();
        assertEquals(corpus.length,index.size());
        for(int i=0;i<corpus.length;i++){
            assertEquals(corpus[i].toLowerCase(Locale.US).replaceAll("[^a-z]",""),
                    index.compactAt(i));
            String[] parts=corpus[i].split(" ");
            if(parts.length>=2&&parts.length<=5){
                String head=parts[0].replaceAll("[^A-Za-z]","").toLowerCase(Locale.US);
                assertEquals(strictStarting(corpus,head),index.startingWith(head));
            }
        }
        Random r=new Random(397123L);
        for(int i=0;i<30_000;i++){
            int length=1+r.nextInt(12);
            StringBuilder word=new StringBuilder(length);
            for(int j=0;j<length;j++)
                word.append((char)('a'+r.nextInt(26)));
            String query=word.toString();
            assertEquals("case "+i,strictStarting(corpus,query),index.startingWith(query));
        }
    }
    @Test public void splitModePreservesExactOldChoicesAndCapitalization()throws Exception {
        final String[] corpus=phrases();
        Method display=PredictionLite.class.getDeclaredMethod(
                "caseDisplay",String.class,String.class);
        display.setAccessible(true);
        String[] input={"how","How","HELLO","hello","i","I","what","What's",
                "im","Im","I'm","thank","thanks","good","what","can","the",
                "notaword","", "h","W","howareyou", "hello there"};
        for(String typed:input){
            List<String> strict=new ArrayList<>();
            if(typed.matches("[A-Za-z]{1,24}")){
                String word=typed.toLowerCase(Locale.US);
                if(PredictionLite.publicWordForContext(word)){
                    for(String phrase:strictStarting(corpus,word)){
                        String value=(String)display.invoke(null,typed,phrase);
                        if(!strict.contains(value))strict.add(value);
                        if(strict.size()==3)break;
                    }
                }
            }
            assertEquals("Original split-mode display for "+typed,
                    strict,PredictionLite.splitPhraseSuggestions(typed));
        }
    }
    @Test public void phraseIndexIsImmutableEvenWithSharedPrefix()throws Exception {
        PublicPhraseIndex397 index=current();
        assertTrue(index.startingWith("hello").size()>0);
        try{
            index.startingWith("hello").add("not allowed");
            fail("Public phrase list must be read only");
        }catch(UnsupportedOperationException expected){}
        assertEquals(phrases().length,index.size());
        assertTrue(index.startingWith("personal phrase").isEmpty());
    }
}
