package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Display-only promotion of an explicitly tappable learned phrase. */
final class SessionPhraseDisplay340 {
    private SessionPhraseDisplay340() {}

    static List<String> promote(List<String> learnedPhrase,
            List<String> singleWords,int limit) {
        ArrayList<String> result=new ArrayList<>();
        if(limit<=0)return result;
        if(learnedPhrase!=null)for(String candidate:learnedPhrase) {
            if(candidate==null || candidate.length()>72 ||
                    !candidate.matches("[A-Za-z']+(?: [A-Za-z']+){1,3}"))
                continue;
            result.add(candidate);
            break; // only one evidence-backed multiword chip
        }
        if(singleWords!=null)for(String word:singleWords) {
            if(result.size()>=limit)break;
            if(word==null || !word.matches("[A-Za-z][A-Za-z'’-]{0,31}"))
                continue;
            boolean duplicate=false;
            for(String existing:result)
                if(existing.toLowerCase(Locale.ROOT)
                        .equals(word.toLowerCase(Locale.ROOT)))duplicate=true;
            if(!duplicate)result.add(word);
        }
        return result;
    }
}
