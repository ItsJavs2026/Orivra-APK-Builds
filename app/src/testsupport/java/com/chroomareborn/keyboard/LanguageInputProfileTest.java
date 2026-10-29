package com.chroomareborn.keyboard;

/** Pure Java input classification regression tests. No user text retained. */
public final class LanguageInputProfileTest {
    private static int checks=0;
    private static void expect(boolean passes,String meaning) {
        checks++;
        if(!passes)throw new AssertionError(meaning);
    }
    private static void kind(String input,LanguageInputProfile.Kind desired) {
        LanguageInputProfile.Result r=LanguageInputProfile.inspect(input);
        expect(r.kind==desired,"wrong classification for "+desired);
    }
    public static void main(String[] unused) {
        kind("helldthere",LanguageInputProfile.Kind.JOINED_LATIN);
        kind("the new keyboard",LanguageInputProfile.Kind.SPACED_LATIN);
        kind("¿Cómo estás?",LanguageInputProfile.Kind.PUNCTUATED_LATIN);
        kind("mañana",LanguageInputProfile.Kind.JOINED_LATIN);
        kind("it's working",LanguageInputProfile.Kind.PUNCTUATED_LATIN);
        kind("wait…",LanguageInputProfile.Kind.PROTECTED);
        kind("🙂",LanguageInputProfile.Kind.PROTECTED);
        kind("hi👩‍💻",LanguageInputProfile.Kind.PROTECTED);
        kind("name@example.com",LanguageInputProfile.Kind.PROTECTED);
        kind("https://example.com",LanguageInputProfile.Kind.PROTECTED);
        kind("www.example.com",LanguageInputProfile.Kind.PROTECTED);
        kind("2026-10-29",LanguageInputProfile.Kind.PROTECTED);
        kind("Olá мир",LanguageInputProfile.Kind.UNSUPPORTED);
        kind("¿!?",LanguageInputProfile.Kind.PROTECTED);
        kind("a\n b",LanguageInputProfile.Kind.SPACED_LATIN);
        kind("\uD800",LanguageInputProfile.Kind.PROTECTED);
        kind("\uDC00",LanguageInputProfile.Kind.PROTECTED);
        kind("e\u0301",LanguageInputProfile.Kind.JOINED_LATIN);
        LanguageInputProfile.Result composed=LanguageInputProfile.inspect("cafe\u0301");
        expect(composed.nfc.equals("café"),"NFC must preserve accent");
        expect(composed.original.length()!=composed.nfc.length(),
                "normalized offsets must never be reused for editor mutations");
        expect(composed.lookup.equals("café"),"lookup preserves accented form");
        expect(LanguageInputProfile.inspect("niño").lookup.equals("niño"),"ñ is not n");
        expect(LanguageInputProfile.inspect("niño").containsSpanishLetters,
                "Spanish letter flag");
        expect(!LanguageInputProfile.inspect("hello").containsSpanishLetters,
                "English letters unchanged");
        expect(LanguageInputProfile.inspect("¡Hola!").eligibleForLanguageResearch(),
                "punctuated Spanish research eligibility");
        expect(!LanguageInputProfile.inspect("3rd").eligibleForLanguageResearch(),
                "digits are protected");
        expect(!LanguageInputProfile.inspect("a@b").eligibleForLanguageResearch(),
                "email-ish tokens protected");
        StringBuilder tooLong=new StringBuilder();
        for(int i=0;i<129;i++)tooLong.append('a');
        kind(tooLong.toString(),LanguageInputProfile.Kind.PROTECTED);
        System.out.println("PASS: LanguageInputProfileTest "+checks+" assertions");
    }
}
