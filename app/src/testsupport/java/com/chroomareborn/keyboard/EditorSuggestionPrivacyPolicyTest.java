package com.chroomareborn.keyboard;

/** Synthetic editor flag combinations only. No editor text, package telemetry. */
public final class EditorSuggestionPrivacyPolicyTest {
    private static int assertions=0;
    private static void check(boolean actual,String label) {
        assertions++;
        if(!actual)throw new AssertionError(label);
    }
    private static EditorSuggestionPrivacyPolicy.Decision decide(String pkg,
            boolean text,boolean password,boolean noSuggest,boolean noLearn) {
        return EditorSuggestionPrivacyPolicy.decide(pkg,text,password,noSuggest,noLearn);
    }
    public static void main(String[] args) {
        final String keep=EditorSuggestionPrivacyPolicy.GOOGLE_KEEP_PACKAGE;
        EditorSuggestionPrivacyPolicy.Decision normal=decide(keep,true,false,false,false);
        check(!normal.sensitive,"Keep normal notes must suggest");
        check(normal.allowPersonalLearning,"Ordinary Keep notes honor user learning preference");
        check(!normal.keepCompatibility,"No exception required for normal Keep text");

        EditorSuggestionPrivacyPolicy.Decision hint=decide(keep,true,false,true,false);
        check(!hint.sensitive,"Keep NO_SUGGESTIONS is not a credential type");
        check(!hint.allowPersonalLearning,"Keep NO_SUGGESTIONS must never learn");
        check(hint.keepCompatibility,"Keep flag exception recorded");

        EditorSuggestionPrivacyPolicy.Decision privacy=decide(keep,true,false,false,true);
        check(!privacy.sensitive,"No personalized learning must not hide suggestions");
        check(!privacy.allowPersonalLearning,"IME no-personalized-learning must be honored");

        EditorSuggestionPrivacyPolicy.Decision both=decide(keep,true,false,true,true);
        check(!both.sensitive,"Keep flags combined may show ephemeral suggestions");
        check(!both.allowPersonalLearning,"No personalized learning always wins");

        for(String sensitivePkg:new String[]{keep,"com.bank.secure","com.google.android.keepx",null,""}) {
            EditorSuggestionPrivacyPolicy.Decision secret=decide(sensitivePkg,true,true,true,false);
            check(secret.sensitive,"Passwords/identifiers must stay private");
            check(!secret.allowPersonalLearning,"Passwords must never learn");
            check(!secret.keepCompatibility,"Never bypass password protections");
        }
        for(String otherPkg:new String[]{"com.bank.secure","com.example.notes","com.google.android.keepx",null,""}) {
            EditorSuggestionPrivacyPolicy.Decision other=decide(otherPkg,true,false,true,false);
            check(other.sensitive,"NO_SUGGESTIONS respected outside exact Keep");
            check(!other.allowPersonalLearning,"Other apps NO_SUGGESTIONS never learn");
        }
        for(String nonTextPkg:new String[]{keep,"com.bank.secure",null}) {
            EditorSuggestionPrivacyPolicy.Decision bad=decide(nonTextPkg,false,false,true,false);
            check(bad.sensitive,"Nontext editor must remain blocked");
            check(!bad.allowPersonalLearning,"Nontext editor must not learn");
        }
        EditorSuggestionPrivacyPolicy.Decision generic=decide("com.example.notes",
                true,false,false,false);
        check(!generic.sensitive,"Normal editor still predicts");
        check(generic.allowPersonalLearning,"Normal editor may use opt-in learning");
        System.out.println("PASS: EditorSuggestionPrivacyPolicyTest "+assertions+" assertions");
    }
}
