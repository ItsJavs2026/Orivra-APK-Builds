package com.chroomareborn.keyboard;

import java.text.Normalizer;
import java.util.Locale;

/** Privacy-bounded, offline EN/ES words for explicit learning and dictionary.
 * No decoder changes: preserves English V1 keys and distinct Spanish accents.
 * Prohibits whitespace, numbers, URLs, emails and invisible punctuation.
 */
final class PersonalWordCanonical {
    private PersonalWordCanonical() {}
    private static final String LETTERS="A-Za-zÁÉÍÓÚÜÑáéíóúüñ";
    private static final String COMPLETE="["+LETTERS+"]+(?:'["+LETTERS+"]+)*";
    private static final String PREFIX=COMPLETE+"'?";
    static String canonical(String raw) {
        if(raw==null)return null;
        return validated(raw.trim(),false);
    }
    static String prefix(String raw) {
        if(raw==null)return null;
        return validated(raw,true);
    }
    private static String validated(String raw,boolean allowTrailingMark) {
        String word=Normalizer.normalize(raw.replace('’','\''),Normalizer.Form.NFC);
        if(word.length()<(allowTrailingMark?1:2)||word.length()>32)return null;
        if(!word.matches(allowTrailingMark?PREFIX:COMPLETE))return null;
        int letters=0;
        for(int i=0;i<word.length();i++)
            if(Character.isLetter(word.charAt(i)))letters++;
        if(letters<(allowTrailingMark?1:2)||letters>28)return null;
        return word.toLowerCase(Locale.ROOT);
    }
}
