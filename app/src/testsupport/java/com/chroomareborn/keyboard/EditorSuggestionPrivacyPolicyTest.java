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
        // Privacy regression: a name or physical address field must remain
        // editable, and can display ephemeral suggestions, but cannot learn.
        for(String field:new String[]{"person-name","postal-address","phonetic-name"}) {
            EditorSuggestionPrivacyPolicy.Decision personal=
                    EditorSuggestionPrivacyPolicy.decide(
                            "com.example.contacts",true,false,false,false,
                            false,true);
            check(!personal.sensitive,field+" is not a password editor");
            check(personal.allowSuggestions,field+" keeps suggestions");
            check(!personal.allowPersonalLearning,field+" never learns");
            EditorSuggestionPrivacyPolicy.Decision searchPersonal=
                    EditorSuggestionPrivacyPolicy.decide(
                            "com.example.search",true,false,true,false,
                            true,true);
            check(!searchPersonal.sensitive,field+" gestures remain available");
            check(searchPersonal.allowSuggestions,field+" explicit Search UI");
            check(!searchPersonal.allowPersonalLearning,field+" cannot learn from Search");
        }
        EditorSuggestionPrivacyPolicy.Decision normal=decide(keep,true,false,false,false);
        check(!normal.sensitive,"Keep normal notes must suggest");
        check(normal.allowSuggestions,"Keep normal notes display suggestions");
        check(normal.allowPersonalLearning,"Ordinary Keep notes honor user learning preference");
        check(!normal.keepCompatibility,"No exception required for normal Keep text");

        EditorSuggestionPrivacyPolicy.Decision hint=decide(keep,true,false,true,false);
        check(!hint.sensitive,"Keep NO_SUGGESTIONS is not a credential type");
        check(hint.allowSuggestions,"Keep note UI suggestions remain enabled");
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
        for(String otherPkg:new String[]{
                "com.google.android.googlequicksearchbox","com.android.chrome",
                "com.example.search","com.bank.secure","com.example.notes",
                "com.google.android.keepx",null,""}) {
            EditorSuggestionPrivacyPolicy.Decision other=decide(
                    otherPkg,true,false,true,false);
            check(!other.sensitive,
                    "NO_SUGGESTIONS alone is not a credential marker: "+otherPkg);
            check(!other.allowPersonalLearning,
                    "NO_SUGGESTIONS must not write learned words: "+otherPkg);
            check(!other.allowSuggestions,
                    "A non-search editor may still opt out of suggestion UI");
            check(!other.keepCompatibility,
                    "Keep compatibility is scoped to exact Keep package");
            // With normal text, Backspace swipe and spacebar cursor movement
            // must no longer be blocked by the prediction UI preference.
            check(CursorReleaseSuggestionPolicy.classify("Search", "",
                    false,other.sensitive).mode==
                    CursorReleaseSuggestionPolicy.Mode.CORRECT_WORD,
                    "ordinary search caret remains eligible for cursor controls");
            check(BackspaceSwipeSnapshotGuard.capture("some search words",null,
                    "",false,1)!=null,
                    "noSuggestions must not block a safe snapshot");
        }
        for(String pkg:new String[]{
                keep,"com.google.android.googlequicksearchbox",
                "com.example.search","com.bank.secure",null
        })for(boolean noSuggest:new boolean[]{false,true})
          for(boolean noLearn:new boolean[]{false,true}) {
            EditorSuggestionPrivacyPolicy.Decision d=decide(
                    pkg,true,true,noSuggest,noLearn);
            check(d.sensitive,"credential types remain private in every app");
            check(!d.allowPersonalLearning,"credential types never learn");
        }
        for(String searchPkg:new String[]{
                "com.google.android.googlequicksearchbox","com.android.chrome",
                "com.example.search"}) {
            EditorSuggestionPrivacyPolicy.Decision search=
                    EditorSuggestionPrivacyPolicy.decide(searchPkg,true,
                            false,true,false,true);
            check(!search.sensitive,"Search never classified as credential");
            check(search.allowSuggestions,"Explicit Search/Go UI exception");
            check(!search.allowPersonalLearning,"Search hint prevents learning");
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
