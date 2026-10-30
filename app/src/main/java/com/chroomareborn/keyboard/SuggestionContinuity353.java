package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Candidate 353: one consistent explicit-suggestion word boundary.
 *
 * A successful tap ends with ONE separator where appropriate; never invent
 * a second space before an existing separator or before punctuation.
 * The cursor-word strip uses [correction | original | correction], preserving
 * the already-tested three permanent TextViews of Candidate 345.
 *
 * Pure, offline, transient policy; no editor text stored or learned.
 */
final class SuggestionContinuity353 {
    private SuggestionContinuity353(){}

    static String spaceAfter(CharSequence following) {
        // Unknown suffix: fail closed instead of potentially doubling a
        // separator. All ordinary Android editors provide at least 1 char.
        if(following==null)return "";
        if(following.length()==0)return " ";
        int cp=Character.codePointAt(following,0);
        if(Character.isWhitespace(cp))return "";
        // A selected word before commas/periods/etc should not acquire
        // an ungrammatical separator before its closing punctuation.
        if(",.;:!?)]}’”»".indexOf(cp)>=0)return "";
        return " ";
    }

    static List<String> centerCurrentWord(String original,List<String> candidates) {
        if(original==null || original.isEmpty())return Collections.emptyList();
        ArrayList<String> others=new ArrayList<>();
        String normalized=original.toLowerCase(Locale.ROOT);
        if(candidates!=null)for(String candidate:candidates){
            if(candidate==null || candidate.trim().isEmpty())continue;
            if(candidate.equalsIgnoreCase(original))continue;
            if(candidate.indexOf(' ')>=0 || candidate.indexOf('\n')>=0)continue;
            boolean exists=false;
            for(String prior:others)if(prior.equalsIgnoreCase(candidate)) {
                exists=true;break;
            }
            if(!exists)others.add(candidate);
            if(others.size()>=2)break;
        }
        // Empty ends are deliberate *invisible* chip slots, not blank
        // tappable candidates. Keep the user's word in index 1 from the
        // first immediate render through every async update.
        return java.util.Arrays.asList(
                others.size()>0?others.get(0):"",
                original,
                others.size()>1?others.get(1):"");
    }

    static boolean resumeNextWordsAtDocumentEnd(boolean staleEditing,
            boolean composingEmpty,boolean hasSelection,
            boolean textAfterCaret,String wordAtCaret) {
        // The Android editor can send late selection callbacks after a
        // long message. An empty caret word immediately after whitespace at
        // the document end is a safe, verified next-word boundary, even
        // when an earlier callback left editingInMiddle stuck true.
        return staleEditing && composingEmpty && !hasSelection &&
                !textAfterCaret && (wordAtCaret==null || wordAtCaret.isEmpty());
    }
}
