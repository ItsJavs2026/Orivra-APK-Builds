package com.chroomareborn.keyboard;

import java.util.List;

/** Synthetic context extraction, no real messages are retained or logged. */
public final class AcceptedContext319PolicyTest {
    static int n;
    static void ok(boolean b,String text){n++;if(!b)throw new AssertionError(text);}
    public static void main(String[] args) {
        ok("please".equals(AcceptedContextPolicy319.precedingForReplacement(
                "Please tekk","tekk")),"visible accepted correction learns please -> tell");
        ok("please".equals(AcceptedContextPolicy319.precedingForNextWord(
                "please ")),"explicit next-word tap learns preceding safe word");
        ok("my".equals(AcceptedContextPolicy319.precedingForReplacement(
                "writing my nove","nove")),"manual saved new word can learn usage");
        for(String raw:new String[]{"bob@example.com ","https://private ","1234 ",
                "FamilyName ","1 please","secret.code ","person: ",
                "foo_bar ","LastName ","a@b.com ","hello\n","user/ ",
                "hello? ","9999"}) {
            ok(AcceptedContextPolicy319.precedingForNextWord(raw)==null,
                    "reject unsafe previous word or punctuation");
        }
        ok(AcceptedContextPolicy319.precedingForReplacement(
                "please text","tekk")==null,"stale cursor word never learned");
        ok(AcceptedContextPolicy319.precedingForReplacement(
                "please tell","tell me")==null,"no phrases in a single word pair");
        ok("that's".equals(AcceptedContextPolicy319.acceptedWord("that's")),
                "English contractions can be contextual targets");
        ok(AcceptedContextPolicy319.acceptedWord("word@example.com")==null,
                "privacy sentinel never becomes a word");
        ok(AcceptedContextPolicy319.acceptedWord("new word")==null,
                "full phrase not stored as target");
        ok(AcceptedContextPolicy319.safeWord("privateName")==null,
                "names not accepted as context");
        ok(AcceptedContextPolicy319.safeWord("javier")==null,
                "proper names not in closed context vocabulary");
        ok(AcceptedContextPolicy319.phraseWords("Yes that's").equals(
                List.of("Yes","that's")),"chosen phrase can learn safe pairs");
        ok(AcceptedContextPolicy319.phraseWords("my new word").equals(
                List.of("my","new","word")),"learn usage from accepted sentence");
        ok(AcceptedContextPolicy319.phraseWords("my email@example.com").isEmpty(),
                "never learn phrase with email");
        ok(AcceptedContextPolicy319.phraseWords("my\nname").isEmpty(),
                "newlines do not form cross-sentence context");
        ok(AcceptedContextPolicy319.phraseWords("single").isEmpty(),
                "single word learning handled separately");
        ok("tell".equals(AcceptedContextPolicy319.precedingForNextWord(
                "please tell ")),"accepted multiword continuation: tell -> me");
        ok("like".equals(AcceptedContextPolicy319.precedingForNextWord(
                "we like ")),"manually saved words learn use after like");
        ok("please".equals(SplitNextWordContextPolicy320.safePrevious(
                "Please",true,false,false,false,false)),
                "split right-side suggestion qualifies as explicit learned tap");
        ok(SplitNextWordContextPolicy320.safePrevious(
                "please",false,false,false,false,false)==null,
                "learning toggle off blocks split context");
        ok(SplitNextWordContextPolicy320.safePrevious(
                "please",true,true,false,false,false)==null,
                "private editor blocks split context");
        ok(SplitNextWordContextPolicy320.safePrevious(
                "please",true,false,true,false,false)==null,
                "editing in middle blocks split context");
        ok(SplitNextWordContextPolicy320.safePrevious(
                "please",true,false,false,true,false)==null,
                "selected text blocks split context");
        ok(SplitNextWordContextPolicy320.safePrevious(
                "please",true,false,false,false,true)==null,
                "text after caret blocks split context");
        ok(SplitNextWordContextPolicy320.safePrevious(
                "privateName",true,false,false,false,false)==null,
                "personal name not included in split context");
        ok(SplitNextWordContextPolicy320.safePrevious(
                "please call",true,false,false,false,false)==null,
                "raw multi-word composing text never persisted");
        System.out.println("PASS: AcceptedContext319PolicyTest "+n+" cases");
    }
}
