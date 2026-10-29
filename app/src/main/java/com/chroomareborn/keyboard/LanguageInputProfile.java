package com.chroomareborn.keyboard;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Research-only, pure Java classification for a future offline EN/ES predictor.
 * Does not change editor text, cursor offsets, or production prediction.
 *
 * IMPORTANT: normalized text can have a different UTF-16 length, so NEVER
 * use normalized character indices to issue InputConnection text edits.
 */
final class LanguageInputProfile {
    enum Kind { JOINED_LATIN, SPACED_LATIN, PUNCTUATED_LATIN,
                PROTECTED, UNSUPPORTED }
    static final class Result {
        final Kind kind;
        final String original;
        final String nfc;
        final String lookup;
        final boolean containsSpanishLetters;
        Result(Kind k,String orig,String canonical,boolean spanish) {
            kind=k;original=orig;nfc=canonical;
            lookup=canonical.toLowerCase(Locale.ROOT);
            containsSpanishLetters=spanish;
        }
        boolean eligibleForLanguageResearch() {
            return kind==Kind.JOINED_LATIN || kind==Kind.SPACED_LATIN ||
                    kind==Kind.PUNCTUATED_LATIN;
        }
    }
    private LanguageInputProfile() {}

    static Result inspect(String input) {
        if(input==null || input.isEmpty() || input.length()>128)
            return new Result(Kind.PROTECTED,input,"",false);
        String nfc=Normalizer.normalize(input,Normalizer.Form.NFC);
        String lowered=nfc.toLowerCase(Locale.ROOT);
        if(lowered.contains("://") || lowered.startsWith("www.") ||
                lowered.indexOf('@')>=0)
            return new Result(Kind.PROTECTED,input,nfc,false);

        boolean hasSpace=false,hasPunctuation=false,hasLetter=false;
        boolean lastWasLetter=false,spanish=false;
        for(int i=0;i<nfc.length();) {
            char first=nfc.charAt(i);
            if(Character.isHighSurrogate(first)) {
                if(i+1>=nfc.length() || !Character.isLowSurrogate(nfc.charAt(i+1)))
                    return new Result(Kind.PROTECTED,input,nfc,false);
            }else if(Character.isLowSurrogate(first))
                return new Result(Kind.PROTECTED,input,nfc,false);
            int cp=nfc.codePointAt(i);
            i+=Character.charCount(cp);
            int type=Character.getType(cp);
            if(Character.isDigit(cp) || Character.isISOControl(cp) &&
                    cp!='\n' && cp!='\t')
                return new Result(Kind.PROTECTED,input,nfc,false);
            if(Character.isLetter(cp)) {
                if(Character.UnicodeScript.of(cp)!=Character.UnicodeScript.LATIN)
                    return new Result(Kind.UNSUPPORTED,input,nfc,false);
                hasLetter=true;lastWasLetter=true;
                if(cp=='ñ'||cp=='Ñ'||cp=='á'||cp=='é'||cp=='í'||cp=='ó'||
                   cp=='ú'||cp=='ü'||cp=='Á'||cp=='É'||cp=='Í'||cp=='Ó'||
                   cp=='Ú'||cp=='Ü')spanish=true;
            }else if(type==Character.NON_SPACING_MARK ||
                    type==Character.COMBINING_SPACING_MARK) {
                if(!lastWasLetter)return new Result(Kind.UNSUPPORTED,input,nfc,false);
                hasLetter=true;
            }else if(Character.isWhitespace(cp)) {
                hasSpace=true;lastWasLetter=false;
            }else if(cp=='.'||cp==','||cp=='!'||cp=='?'||cp=='¿'||cp=='¡'||
                    cp=='\''||cp=='’'||cp=='-'||cp==';'||cp==':') {
                hasPunctuation=true;lastWasLetter=false;
            }else {
                // Emoji, pictographs, standalone symbols and other scripts
                // require safe token boundaries before prediction.
                return new Result(Kind.PROTECTED,input,nfc,spanish);
            }
        }
        if(!hasLetter)return new Result(Kind.PROTECTED,input,nfc,false);
        Kind kind=hasPunctuation?Kind.PUNCTUATED_LATIN:
                hasSpace?Kind.SPACED_LATIN:Kind.JOINED_LATIN;
        return new Result(kind,input,nfc,spanish);
    }
}
