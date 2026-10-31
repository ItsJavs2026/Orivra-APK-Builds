package com.chroomareborn.keyboard;

/**
 * Build 416B: selected-text Backspace uses a single explicit replacement.
 *
 * Android's deleteSurroundingText(1,0) is a character edit and is not
 * guaranteed to remove a highlighted selection. commitText("",1) replaces
 * exactly the selected region (including large selections) when the editor
 * honors InputConnection semantics. Never finalize composing first, delete
 * surrounding characters, synthesize DELETE or retry a false-returning edit.
 *
 * This transaction never reads/stores/logs selected text or user content.
 */
final class SelectedBackspace416B {
    private SelectedBackspace416B() {}

    enum Outcome { NOT_SELECTED, STALE, APPLIED, UNCERTAIN }

    interface Editor {
        boolean sameSession();
        boolean selectionKnown();
        boolean replaceSelectionOnce();
    }

    static Outcome apply(Editor editor) {
        if(editor==null || !editor.sameSession())return Outcome.STALE;
        if(!editor.selectionKnown())return Outcome.NOT_SELECTED;
        try {
            // A focus change while a Backspace callback is pending must not
            // delete text in the next editor.
            if(!editor.sameSession())return Outcome.STALE;
            boolean accepted=editor.replaceSelectionOnce();
            // A false return may be false-but-applied by a remote editor:
            // no fallback, no retries, no surrounding-character deletion.
            if(!editor.sameSession())return Outcome.STALE;
            return accepted?Outcome.APPLIED:Outcome.UNCERTAIN;
        } catch(RuntimeException disconnected) {
            return Outcome.UNCERTAIN;
        }
    }
}
