package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.os.Handler;
import android.view.ViewConfiguration;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.HapticFeedbackConstants;
import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.os.Build;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.SurroundingText;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.List;

public final class RebornImeService extends InputMethodService {
    private LinearLayout base, keys, candidates, paletteBar, paletteControls, themePanel;
    private HorizontalScrollView suggestionScroll;
    private TextView swipeDeletePreview;
    private TextView paletteLabel;
    private Button paletteButton, sizeButton, predictionsToggle, animationSpeedButton, splitModeButton, nextWordButton;
    private LinearLayout predictionHeader, expandedSuggestions;
    private boolean predictionsExpanded=false;
    private boolean freezeRiseDuringDelete = false;
    // Smart Rise 2.0: retain the panel briefly through transient candidate gaps.
    private final Runnable delayedRiseDismiss=()->{
        if(!predictionsExpanded || freezeRiseDuringDelete)return;
        predictionsExpanded=false;
        updatePredictionPanel();
    };
    private static final String SPLIT_PREDICTIONS_PREF="orivra_split_predictions";
    // Opt-in, download-free next-word seeds. No keystrokes are stored here.
    private static final String OFFLINE_NEXT_WORD_PREF="orivra_offline_next_word_v1";
    private NextWordSeedModel offlineNextWordSeed;
    private boolean offlineNextWordSeedLoadAttempted=false;
    private boolean offlineNextWordEnabled() {
        return getSharedPreferences("keyboard_feedback",MODE_PRIVATE)
                .getBoolean(OFFLINE_NEXT_WORD_PREF,false);
    }
    private NextWordSeedModel offlineNextWordSeed() {
        if(offlineNextWordSeedLoadAttempted)return offlineNextWordSeed;
        offlineNextWordSeedLoadAttempted=true;
        try(java.io.InputStreamReader reader=new java.io.InputStreamReader(
                getAssets().open("lm/swiftfloris_en_bigrams.tsv"),
                java.nio.charset.StandardCharsets.UTF_8)) {
            offlineNextWordSeed=NextWordSeedModel.fromTsv(reader);
        } catch(java.io.IOException ignored) {
            // Missing/corrupt optional model must never disable typing.
            offlineNextWordSeed=null;
        }
        return offlineNextWordSeed;
    }

    private boolean splitPredictionsEnabled() {
        return getSharedPreferences("keyboard_feedback",MODE_PRIVATE).getBoolean(SPLIT_PREDICTIONS_PREF,true);
    }
    private List<String> splitPhrases=java.util.Collections.emptyList();
    private List<String> splitNextWords=java.util.Collections.emptyList();
    private String splitSnapshot340=null;
    private List<String> splitPhrasesCache340=java.util.Collections.emptyList();
    private List<String> splitNextCache340=java.util.Collections.emptyList();
    private boolean panelWasVisible=false;
    private boolean riseChipPressed=false;
    private String riseRenderedSignature="";
    private boolean manuallyCollapsedPredictions=false;
    private List<String> latestOptions=java.util.Collections.emptyList();
    private String latestSnapshot="";
    private String manualCollapseSnapshot="";
    private boolean toolbarExpanded = false;
    private static final String HAPTIC_PREF = "orivra_haptic_enabled";
    // 1=Light, 2=Standard, 3=Crisp. 0 is handled by the existing on/off preference.
    private static final String HAPTIC_STYLE_PREF = "orivra_haptic_style";
    private static final String ANIMATION_PREF = "sentenceflow_rise_ms";
    private long lastTypingTime=0L;
    private double averageTypingInterval=0;
    private int riseMode() {
        int mode=getSharedPreferences("keyboard_feedback", MODE_PRIVATE)
                .getInt(ANIMATION_PREF,600);
        return mode==400||mode==600||mode==800||mode==1000?mode:600;
    }
    private void recordTypingRhythm() {
        long now=android.os.SystemClock.uptimeMillis();
        if(lastTypingTime>0) {
            long interval=now-lastTypingTime;
            if(interval>=45 && interval<=1600) {
                averageTypingInterval=averageTypingInterval==0?interval:
                        averageTypingInterval*.75+interval*.25;
            } else if(interval>2500) averageTypingInterval=0;
        }
        lastTypingTime=now;
    }
    private int riseDurationMs() {
        int mode=riseMode();
        return mode;
    }
    private boolean hapticsEnabled() {
        return getSharedPreferences("keyboard_feedback", MODE_PRIVATE).getBoolean(HAPTIC_PREF, true);
    }
    private int hapticStyle() {
        int style=getSharedPreferences("keyboard_feedback", MODE_PRIVATE)
                .getInt(HAPTIC_STYLE_PREF,KeyboardHapticStyle.STANDARD);
        return style>=KeyboardHapticStyle.LIGHT && style<=KeyboardHapticStyle.CRISP ?
                style:KeyboardHapticStyle.STANDARD;
    }
    private void keyHaptic(View view) {
        if(view==null || !hapticsEnabled())return;
        // Use Android's system-tuned haptics rather than VIBRATE permission.
        // Actual strength is device-dependent and respects system settings.
        final int effect=hapticStyle()==1 ? HapticFeedbackConstants.CLOCK_TICK :
                hapticStyle()==3 && Build.VERSION.SDK_INT>=30 ?
                        HapticFeedbackConstants.CONFIRM :
                        HapticFeedbackConstants.KEYBOARD_TAP;
        view.performHapticFeedback(effect);
    }
    private int currentRowHeightDp=72;
    // Candidate 329 research: one completed-word observation per stable
    // composition; never store intermediate keystrokes or full sentences.
    private Runnable pendingStableWord329;
    private String stableWordCounted329="";
    private long stableWordSession329=-1L;
    private void cancelStableWord329() {
        if(pendingStableWord329!=null)
            suggestionHandler.removeCallbacks(pendingStableWord329);
        pendingStableWord329=null;
    }
    private void scheduleStableWord329() {
        cancelStableWord329();
        // Some editors (including custom chat/search controls) commit each
        // keystroke instead of keeping an Android composing span. The live
        // word-under-caret supplies the same bounded, snapshot-verified input.
        String visible=composing.isEmpty()?wordAtCursor():composing;
        String expected=StableWordUsagePolicy329.candidate(
                visible,PersonalLearning.enabled(this),
                editorAllowsPersonalLearning,sensitive,editingInMiddle,
                hasSelection(),cursorHasTextAfter(),symbols,numericEditor);
        if(expected==null || (expected.equals(stableWordCounted329) &&
                stableWordSession329==inputSessionGeneration))return;
        final InputConnection editor=getCurrentInputConnection();
        final long session=inputSessionGeneration;
        if(editor==null)return;
        final Runnable candidate=()->{
            pendingStableWord329=null;
            if(!PersonalLearning.enabled(this) || !editorAllowsPersonalLearning ||
                    sensitive || editingInMiddle || hasSelection() ||
                    cursorHasTextAfter() || symbols || numericEditor)return;
            try {
                String stillVisible=composing.isEmpty()?wordAtCursor():composing;
                if(StableWordUsagePolicy329.sameVisibleWord(expected,
                        stillVisible,editor.getTextBeforeCursor(64,0),
                        editor==getCurrentInputConnection(),
                        session==inputSessionGeneration)) {
                    PersonalWordFrequency328.recordCommitted(this,expected);
                    personalPrefixCache340.clear();
                    stableWordCounted329=expected;
                    stableWordSession329=session;
                }
            } catch(RuntimeException detached){/* ignore stale input editor */}
        };
        pendingStableWord329=candidate;
        // Idle observation is weaker than an explicit tap. Combined with
        // minimum repeat thresholds, a pause mid-word won't auto-replace it.
        long stableDelayMs=StableWordTiming334.milliseconds(expected);
        if(stableDelayMs>0)
            suggestionHandler.postDelayed(candidate,stableDelayMs);
    }
    // A synchronous InputConnection callback must not tear down the very
    // composing edit currently being verified. Selection changes initiated
    // by the user still follow onUpdateSelection's normal checks.
    private boolean composingTransaction340=false;
    private String composing = "";
    private void appendComposing340(InputConnection ic,String addition) {
        final long session=inputSessionGeneration;
        final String previous=composing;
        ComposingMutation340.Result outcome;
        composingTransaction340=true;
        try {
            outcome=ComposingMutation340.append(
                    new ComposingMutation340.Editor() {
                @Override public boolean sameSession() {
                    return session==inputSessionGeneration &&
                            ic==getCurrentInputConnection();
                }
                @Override public CharSequence before() {
                    return ic.getTextBeforeCursor(
                            ComposingMutation340.READ_WINDOW,0);
                }
                @Override public boolean setComposing(String next) {
                    return ic.setComposingText(next,1);
                }
                @Override public boolean finishComposing() {
                    return ic.finishComposingText();
                }
                @Override public boolean commit(String text) {
                    return ic.commitText(text,1);
                }
            },previous,addition);
        } finally {
            composingTransaction340=false;
        }
        if(outcome==ComposingMutation340.Result.COMPOSING) {
            composing=previous+addition;
        } else {
            // A false-but-applied remote commit is NOT retried. In particular
            // it must not add another highlighted word or character.
            composing="";
            if(outcome==ComposingMutation340.Result.PRECONDITION &&
                    session==inputSessionGeneration &&
                    ic==getCurrentInputConnection()) {
                // A caret move detached the previous composing word. The
                // user's current key still belongs at the new caret, once.
                ic.finishComposingText();
                if(session==inputSessionGeneration &&
                        ic==getCurrentInputConnection())
                    ic.commitText(addition,1);
            }
        }
    }
    private boolean sensitive, shift, symbols, capsLock;
    // NO_SUGGESTIONS is not a password marker in Google Keep note editors.
    // Honor NO_PERSONALIZED_LEARNING separately even when suggestions appear.
    private boolean editorAllowsPersonalLearning=false;
    // Separate from credential privacy: NO_SUGGESTIONS can suppress the UI
    // without disabling Backspace, spacebar cursor or Search/Go actions.
    private boolean editorAllowsSuggestions=true;
    // Build 303 research: optional, default-OFF large exact-word dictionary.
    // This pack is never loaded on touch or inside the 40ms prediction worker.
    // Unlike personal dictionary, this contains only publicly sourced words.
    private OfflineLanguagePackWorker optionalDictionaryLoader;
    private ExactOnlyLanguagePack readyOptionalDictionary;
    private boolean optionalDictionaryAttempted=false;
    private boolean optionalDictionaryEnabled() {
        return getSharedPreferences(OfflineDictionaryActivationPolicy.PREFS,MODE_PRIVATE)
                .getBoolean(OfflineDictionaryActivationPolicy.KEY,false);
    }
    private void closeOptionalDictionary() {
        if(optionalDictionaryLoader!=null){
            optionalDictionaryLoader.close();
            optionalDictionaryLoader=null;
        }
        optionalDictionaryAttempted=false;
        readyOptionalDictionary=null;
        PredictionLite.setResearchExactOnlyPack(null);
    }
    private void reconcileOptionalDictionary() {
        boolean optedIn=optionalDictionaryEnabled();
        if(!optedIn){
            closeOptionalDictionary();
            return;
        }
        if(!editorAllowsSuggestions ||
                !OfflineDictionaryActivationPolicy.usable(optedIn,sensitive)){
            // Protected fields NEVER use the supplemental dictionary.
            PredictionLite.setResearchExactOnlyPack(null);
            return;
        }
        if(readyOptionalDictionary!=null){
            PredictionLite.setResearchExactOnlyPack(readyOptionalDictionary);
            return;
        }
        // Always retain the old predictor until a verified asset loads.
        PredictionLite.setResearchExactOnlyPack(null);
        if(!OfflineDictionaryActivationPolicy.startWorker(optedIn,sensitive,
                optionalDictionaryAttempted,readyOptionalDictionary!=null))return;
        optionalDictionaryAttempted=true;
        final OfflineLanguagePackWorker loader=new OfflineLanguagePackWorker();
        optionalDictionaryLoader=loader;
        if(!loader.start(this,result->{
            // A completed old worker must never clear a newer editor's pack.
            if(optionalDictionaryLoader!=loader)return;
            boolean eligible=OfflineDictionaryActivationPolicy.acceptCallback(
                    optionalDictionaryEnabled(),sensitive,true,
                    result!=null && result.pack!=null);
            if(eligible){
                readyOptionalDictionary=result.pack;
                PredictionLite.setResearchExactOnlyPack(result.pack);
                // No editor reads or direct UI refresh: next ordinary
                // prediction request safely uses the newly ready pack.
            }else{
                PredictionLite.setResearchExactOnlyPack(null);
                // This worker cannot be reused; permit retry on a later
                // normal field if a private editor interrupted its result.
                optionalDictionaryLoader=null;
                optionalDictionaryAttempted=false;
                loader.close();
            }
        })){
            loader.close();
            if(optionalDictionaryLoader==loader)optionalDictionaryLoader=null;
        }
    }
    // Prevent detaching a pressed letter key while its long-press symbol
    // is committed. Refresh any changed Shift appearance after finger lift.
    private boolean symbolHoldInProgress=false;
    private boolean extraSymbols=false;

    private boolean numericEditor=false;
    private boolean numericAlphabeticOverride=false;
    // Selection callbacks can arrive after an IME write. Only an actual
    // editor selection outside our composing span should end composition.
    private boolean editingInMiddle = false;
    private int lastSelectionStart = -1, lastSelectionEnd = -1;
    // Build 283 Candidate: short-lived ticket for editing chips only.
    // Never persisted, logged, or shared with another editor.
    private long cursorEditingRevision = 1L;
    private CursorEditSnapshotGuard.Ticket visibleEditingTicket;
    private void invalidateEditingTicket() {
        cursorEditingRevision++;
        visibleEditingTicket=null;
    }
    private CursorEditSnapshotGuard.Ticket captureEditingTicket(String word) {
        if(sensitive || hasSelection() || !composing.isEmpty())return null;
        try {
            InputConnection connection=getCurrentInputConnection();
            if(connection==null)return null;
            return CursorEditSnapshotGuard.capture(word,
                connection.getTextBeforeCursor(CursorEditSnapshotGuard.WINDOW,0),
                connection.getTextAfterCursor(CursorEditSnapshotGuard.WINDOW,0),
                cursorEditingRevision,lastSelectionStart,lastSelectionEnd,
                hasSelection(),!composing.isEmpty(),sensitive);
        } catch(RuntimeException detachedEditor) { return null; }
    }
    private boolean isEditingTicketCurrent(CursorEditSnapshotGuard.Ticket ticket) {
        if(ticket==null || sensitive || hasSelection() || !composing.isEmpty())return false;
        try {
            InputConnection connection=getCurrentInputConnection();
            return connection!=null && CursorEditSnapshotGuard.current(ticket,
                connection.getTextBeforeCursor(CursorEditSnapshotGuard.WINDOW,0),
                connection.getTextAfterCursor(CursorEditSnapshotGuard.WINDOW,0),
                cursorEditingRevision,lastSelectionStart,lastSelectionEnd,
                hasSelection(),!composing.isEmpty(),sensitive);
        } catch(RuntimeException detachedEditor) { return false; }
    }
    private int background = 0xFF192232, keyBackground = 0xFF38445C, accent = 0xFF5A79DE;
    private int keyTextColor=Color.WHITE,secondaryTextColor=0xFFC6CEDB;
    private int borderColor=0xFF38445C,specialBackground=0xFF39455C;
    private int suggestionBackground=0xFF38445C,suggestionTextColor=Color.WHITE;
    private int rippleColor=0x665A79DE;
    // Predictions never run on the keyboard UI thread. Result callbacks are guarded
    // against stale editor text and posted to the main thread before touching views.
    private final PredictionDispatcher predictionDispatcher = new PredictionDispatcher();
    private final PrefixCandidateCache340 personalPrefixCache340=
            new PrefixCandidateCache340();
    // Research 340: opt-in, strictly transient word-by-word next-word
    // learning, including new words. Does not alter the portable V3 format.
    private final PersonalNextWordModel tappedSequence340=
            new PersonalNextWordModel();
    private String lastTappedWord340="";
    private long lastTappedSession340=-1L;
    private void clearTapSequence340() {
        lastTappedWord340="";
        lastTappedSession340=-1L;
        tappedSequence340.resetContext();
    }
    private List<String> personalizedPrefix340(String prefix) {
        return personalPrefixCache340.get(prefix,
                word->PersonalWordFrequency328.matching(this,word,3));
    }
    private final Handler suggestionHandler = new Handler(Looper.getMainLooper());
    // A single tap deletes once; holding for 380 ms starts repeated deletion.
    // All callbacks run on the IME main thread and are cancelled on release/exit.
    private static final long DELETE_HOLD_DELAY_MS = 380L;
    private static final long DELETE_REPEAT_MS = 65L;
    private final Handler deleteHandler = new Handler(Looper.getMainLooper());
    // Gboard-style horizontal cursor drag belongs to spacebar only.
    // Navigation never adds/removes characters or changes a selection's text.
    private SpacebarCursorGesture spacebarCursorGesture;
    private Button spacebarButton;
    private int spacebarPointerId=-1;
    private boolean spacebarTouchActive=false;
    private boolean cursorSwipeNavigating=false;
    // One post-lift refresh after Android has processed final setSelection.
    // This also suppresses stale onUpdateSelection callbacks during release.
    private static final long CURSOR_RELEASE_SETTLE_MS=32L;
    private boolean cursorReleaseSuggestionsPending=false;
    private final Runnable cursorReleaseSuggestions=()->{
        cursorReleaseSuggestionsPending=false;
        if(!spacebarTouchActive)refreshSuggestionsAfterCursorRelease();
    };
    private final CursorSwipeBatch cursorSwipeBatch=new CursorSwipeBatch();
    private static final long CURSOR_SWIPE_FRAME_DELAY_MS=24L;
    private boolean cursorSwipeFrameScheduled=false;
    private final Runnable cursorSwipeFrame=new Runnable() {
        @Override public void run() {
            cursorSwipeFrameScheduled=false;
            if(!spacebarTouchActive || !cursorSwipeNavigating) {
                cursorSwipeBatch.clear();
                return;
            }
            int steps=cursorSwipeBatch.takeFrame();
            if(steps!=0)moveCursorBySwipe(steps);
            if(cursorSwipeBatch.hasPending() && spacebarTouchActive)
                scheduleCursorSwipeFrame();
        }
    };
    private static final long SPACEBAR_HINT_DELAY_MS=270L;
    private final Runnable spacebarHoldHint=()->{
        if(spacebarTouchActive && spacebarCursorGesture!=null &&
                spacebarCursorGesture.hold() && spacebarButton!=null) {
            spacebarButton.setText("← slide to move →");
        }
    };
    private boolean deleteHeld = false;
    // Physical Backspace contact and repeat arming are independent: a
    // cancelled swipe can stop repeating but still owns its touch target.
    private boolean backspaceTouchActive = false;
    // Enter's touched key must also remain mounted until ACTION_UP/CANCEL.
    // A composing/prediction callback may request refresh while SEND arms.
    private boolean enterTouchActive = false;
    private InputConnection deleteRepeatEditor;
    private long deleteRepeatSession;
    // No Enter action is committed while a finger is still held down.
    private static final long ENTER_SEND_HOLD_MS = 650L;
    private Runnable enterHoldPending;
    private void stopEnterHold() {
        if(enterHoldPending!=null) {
            suggestionHandler.removeCallbacks(enterHoldPending);
            enterHoldPending=null;
        }
    }
    private boolean sameEnterSession(long session, InputConnection editor) {
        return BackspaceSwipePolicy.sameEditorSession(
                session,inputSessionGeneration,editor,getCurrentInputConnection());
    }
    // Guard deferred touch-release deletion against editor and view lifecycle
    // changes. A stale callback must never delete in the next text field.
    private long inputSessionGeneration = 0L;
    // Cancel the initial hold timer when a swipe wins or the editor closes.
    private Runnable deleteHoldPending;
    private final Runnable deleteRepeat = new Runnable() {
        @Override public void run() {
            if (!BackspaceRepeatGuard.repeatAllowed(
                    backspaceTouchActive,deleteHeld,
                    deleteRepeatSession,inputSessionGeneration,
                    deleteRepeatEditor,getCurrentInputConnection())) {
                stopDeleteRepeat();
                return;
            }
            backspace();
            deleteHandler.postDelayed(this, DELETE_REPEAT_MS);
        }
    };
    private void stopDeleteRepeat() {
        deleteHeld = false;
        deleteRepeatEditor = null;
        if (deleteHoldPending != null) {
            deleteHandler.removeCallbacks(deleteHoldPending);
            deleteHoldPending = null;
        }
        deleteHandler.removeCallbacks(deleteRepeat);
    }
    private void invalidateBackspaceGestureSession() {
        inputSessionGeneration++;
        backspaceTouchActive=false;
        enterTouchActive=false;
        stopDeleteRepeat();
        stopEnterHold();
        freezeRiseDuringDelete = false;
        hideSwipeDeletePreview();
    }

    private void stopSpacebarCursorGesture() {
        cancelCursorReleaseSuggestions();
        suggestionHandler.removeCallbacks(spacebarHoldHint);
        suggestionHandler.removeCallbacks(cursorSwipeFrame);
        cursorSwipeFrameScheduled=false;
        cursorSwipeBatch.clear();
        if(spacebarCursorGesture!=null)spacebarCursorGesture.cancel();
        spacebarTouchActive=false;
        cursorSwipeNavigating=false;
        spacebarPointerId=-1;
        if(spacebarButton!=null) {
            spacebarButton.setPressed(false);
            spacebarButton.setText("EN");
        }
    }

    private void cancelCursorReleaseSuggestions() {
        suggestionHandler.removeCallbacks(cursorReleaseSuggestions);
        cursorReleaseSuggestionsPending=false;
    }

    private void scheduleCursorReleaseSuggestions() {
        cancelCursorReleaseSuggestions();
        if(sensitive || !editorAllowsSuggestions) {
            predictionDispatcher.cancel();
            renderSuggestions("",java.util.Collections.emptyList());
            return;
        }
        cursorReleaseSuggestionsPending=true;
        suggestionHandler.postDelayed(cursorReleaseSuggestions,
                CURSOR_RELEASE_SETTLE_MS);
    }

    /**
     * Runs once after the finger lifts, never during ACTION_MOVE.
     * Correct a word the caret touches; only show next-word choices at a
     * real document-end boundary. A selection or an empty mid-document
     * gap gets no automatic insertion proposals.
     */
    private void refreshSuggestionsAfterCursorRelease() {
        if(candidates==null || spacebarTouchActive)return;
        if(sensitive || !editorAllowsSuggestions) {
            predictionDispatcher.cancel();
            renderSuggestions("",java.util.Collections.emptyList());
            return;
        }
        if(!composing.isEmpty()) {
            // A small gesture can activate cursor mode without actually
            // moving the caret. Keep an unfinished composition intact.
            showSuggestions();
            return;
        }
        CursorReleaseSuggestionPolicy.Decision decision;
        try {
            InputConnection ic=getCurrentInputConnection();
            if(ic==null)return;
            decision=CursorReleaseSuggestionPolicy.classify(
                    ic.getTextBeforeCursor(64,0),
                    ic.getTextAfterCursor(64,0),
                    hasSelection(),sensitive);
        } catch(RuntimeException detachedEditor) {
            predictionDispatcher.cancel();
            return;
        }

        if(decision.mode==CursorReleaseSuggestionPolicy.Mode.CLEAR) {
            editingInMiddle=true;
            predictionDispatcher.cancel();
            renderSuggestions("",java.util.Collections.emptyList());
            return;
        }
        editingInMiddle=decision.mode==
                CursorReleaseSuggestionPolicy.Mode.CORRECT_WORD;
        boolean oldShift=shift;
        updateSentenceShift();
        showSuggestions();
        // Rebuilding the entire keyboard for a cursor move is unnecessary.
        // Only rebuild keys if sentence capitalization actually changed.
        if(oldShift!=shift)refreshView();
    }

    private void scheduleCursorSwipeFrame() {
        if(cursorSwipeFrameScheduled || !spacebarTouchActive ||
                !cursorSwipeNavigating || !cursorSwipeBatch.hasPending())return;
        cursorSwipeFrameScheduled=true;
        suggestionHandler.postDelayed(cursorSwipeFrame,CURSOR_SWIPE_FRAME_DELAY_MS);
    }

    private void flushFinalCursorSwipe() {
        suggestionHandler.removeCallbacks(cursorSwipeFrame);
        cursorSwipeFrameScheduled=false;
        // At most 32 character-steps in the release event, never hundreds.
        // Prefer one setSelection call; the legacy DPAD fallback is even
        // more conservative (at most four key events per batch).
        int steps=cursorSwipeBatch.takeFinal();
        if(steps!=0 && spacebarTouchActive && cursorSwipeNavigating)
            moveCursorBySwipe(steps);
    }

    /**
     * Moves the insertion point without committing, replacing, or deleting text.
     * API 31+ gives a bounded snapshot and absolute offset, allowing a single
     * selection operation per movement event. Editors that don't expose it use
     * standard left/right DPAD keys; secure fields never request text.
     */
    private boolean moveCursorBySwipe(int requestedSteps) {
        if(requestedSteps==0 || !spacebarTouchActive || !cursorSwipeNavigating)
            return false;
        InputConnection ic=getCurrentInputConnection();
        if(ic==null) {
            stopSpacebarCursorGesture();
            return false;
        }
        int steps=Math.max(-CursorSwipeBatch.MAX_PENDING,
                Math.min(CursorSwipeBatch.MAX_PENDING,requestedSteps));
        // Do not trust chips captured before an in-flight cursor movement.
        invalidateEditingTicket();
        try {
            if(!composing.isEmpty())currentFinish();
            if(!sensitive && Build.VERSION.SDK_INT>=31) {
                // Only once per 24ms frame, rather than for each ACTION_MOVE.
                // A successful bounded snapshot lets us send one selection IPC.
                SurroundingText surrounding=ic.getSurroundingText(160,160,0);
                if(surrounding!=null && surrounding.getText()!=null &&
                        surrounding.getOffset()>=0) {
                    CharSequence text=surrounding.getText();
                    int start=surrounding.getSelectionStart();
                    int end=surrounding.getSelectionEnd();
                    int local=steps<0?Math.min(start,end):Math.max(start,end);
                    if(local>=0 && local<=text.length()) {
                        int target=CursorMovementPolicy.move(text,local,steps);
                        // Already at the edge of the editor snapshot: don't
                        // generate a flood of DPAD events as a false fallback.
                        if(target==local)return true;
                        long absolute=(long)surrounding.getOffset()+target;
                        if(absolute>=0 && absolute<=Integer.MAX_VALUE &&
                                ic.setSelection((int)absolute,(int)absolute)) {
                            lastSelectionStart=lastSelectionEnd=(int)absolute;
                            return true;
                        }
                    }
                }
            }
            // Do not ask password/private editors for surrounding text.
            // A safe navigation fallback cannot infer character boundaries
            // there, so disable the gesture rather than read sensitive data.
            if(sensitive)return false;
            // Some custom editors cannot provide an absolute selection
            // offset (SurroundingText.getOffset()==-1). The DPAD fallback
            // MUST NOT send RIGHT at the end of the text: some editors move
            // keyboard focus to the next UI control and dismiss the IME.
            //
            // Require a live one-sided snapshot. If the editor cannot return
            // that text, fail closed instead of risking focus navigation.
            CharSequence adjacent=steps<0?
                    ic.getTextBeforeCursor(4,0):ic.getTextAfterCursor(4,0);
            int arrowEvents=CursorFallbackBoundaryPolicy.allowedArrowEvents(
                    steps,adjacent);
            if(arrowEvents==0)return true;
            int key=steps<0?KeyEvent.KEYCODE_DPAD_LEFT:
                    KeyEvent.KEYCODE_DPAD_RIGHT;
            boolean moved=false;
            for(int n=0;n<arrowEvents;n++) {
                if(!ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key)))
                    break;
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));
                moved=true;
            }
            return moved;
        } catch(RuntimeException editorDisconnectedOrRejected) {
            // The editor can detach mid-swipe. Never allow its failure to
            // crash the IME or carry a queued gesture into the next editor.
            stopSpacebarCursorGesture();
            return false;
        }
    }

    private void moveSpacebarDuringTouch(MotionEvent event) {
        if(!spacebarTouchActive || spacebarCursorGesture==null)return;
        int index=event.findPointerIndex(spacebarPointerId);
        if(index<0) {
            stopSpacebarCursorGesture();
            return;
        }
        int steps=spacebarCursorGesture.move(event.getX(index),event.getY(index));
        if(spacebarCursorGesture.isNavigating()) {
            suggestionHandler.removeCallbacks(spacebarHoldHint);
            if(!cursorSwipeNavigating) {
                cursorSwipeNavigating=true;
                predictionDispatcher.cancel();
                if(spacebarButton!=null) {
                    spacebarButton.setText("◀ cursor ▶");
                    keyHaptic(spacebarButton);
                }
            }
        }
        // Touch events can arrive much faster than the editor can process
        // InputConnection IPC. Aggregate motion and dispatch at most once
        // per short frame, with a capped backlog and directional reversal.
        if(steps!=0 && cursorSwipeNavigating) {
            cursorSwipeBatch.offer(steps);
            scheduleCursorSwipeFrame();
        }
    }

    /**
     * Overrides the generic ACTION_DOWN key handler. A normal spacebar tap
     * inserts one space on ACTION_UP; a horizontal drag moves the cursor
     * instead and cannot accidentally type a space.
     */
    private void attachSpacebarCursorControl(Button key) {
        spacebarButton=key;
        key.setText("EN");
        key.setContentDescription(
                "Space. Slide left or right on the spacebar to move the cursor.");
        key.setOnClickListener(v->space()); // accessibility / programmatic click
        final int touchSlop=ViewConfiguration.get(this).getScaledTouchSlop();
        final float activate=Math.max(dp(12),touchSlop*2f);
        final float step=Math.max(dp(16),touchSlop*1.5f);
        final float vertical=Math.max(dp(28),touchSlop*3f);
        spacebarCursorGesture=new SpacebarCursorGesture(activate,step,vertical);
        key.setOnTouchListener((view,event)->{
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    stopSpacebarCursorGesture();
                    spacebarButton=key;
                    spacebarTouchActive=true;
                    spacebarPointerId=event.getPointerId(0);
                    spacebarCursorGesture.start(event.getX(0),event.getY(0));
                    view.setPressed(true);
                    animateKeyPress(view,true);
                    keyHaptic(view);
                    suggestionHandler.postDelayed(spacebarHoldHint,SPACEBAR_HINT_DELAY_MS);
                    return true;
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_POINTER_UP:
                    // A second pointer must not turn a multi-finger gesture
                    // into an accidental space or editor-navigation command.
                    stopSpacebarCursorGesture();
                    animateKeyPress(view,false);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    moveSpacebarDuringTouch(event);
                    return true;
                case MotionEvent.ACTION_UP: {
                    if(!spacebarTouchActive)return true;
                    moveSpacebarDuringTouch(event); // consume final pixels
                    boolean wasDragging=spacebarCursorGesture.isNavigating();
                    boolean insertSpace=spacebarCursorGesture.finish();
                    if(wasDragging)flushFinalCursorSwipe();
                    stopSpacebarCursorGesture();
                    animateKeyPress(view,false);
                    if(insertSpace) {
                        space();
                    } else if(wasDragging) {
                        // Let final editor selection callbacks settle before
                        // requesting corrections once, never per swipe step.
                        scheduleCursorReleaseSuggestions();
                    }
                    return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    stopSpacebarCursorGesture();
                    animateKeyPress(view,false);
                    return true;
                default:return true;
            }
        });
    }

    // Reuse a single interpolator. Do not allocate extra Animators per key,
    // mutate layout bounds, or animate suggestion panels during fast typing.
    private static final android.view.animation.Interpolator KEY_MOTION_EASE=
            new android.view.animation.DecelerateInterpolator(1.35f);
    private void animateKeyPress(View view, boolean pressed) {
        if(view==null)return;
        view.animate().cancel();
        // Respect OS animation disable/reduced-motion settings.
        boolean enabled=android.animation.ValueAnimator.areAnimatorsEnabled();
        float scale=TypingMotionPolicy.scale(pressed,enabled);
        if(!enabled || !view.isAttachedToWindow()) {
            view.setScaleX(TypingMotionPolicy.REST_SCALE);
            view.setScaleY(TypingMotionPolicy.REST_SCALE);
            return;
        }
        view.animate().scaleX(scale).scaleY(scale)
                .setInterpolator(KEY_MOTION_EASE)
                .setDuration(TypingMotionPolicy.duration(pressed,true)).start();
    }
    private void hideSwipeDeletePreview() {
        if(swipeDeletePreview!=null) {
            // Clear the transient text immediately; never retain a draft
            // fragment after the gesture finishes or the editor changes.
            swipeDeletePreview.setText("");
            swipeDeletePreview.setVisibility(View.GONE);
        }
        if(suggestionScroll!=null)suggestionScroll.setVisibility(View.VISIBLE);
    }
    private BackspaceSwipeSnapshotGuard.Ticket captureSwipeDeleteTicket(
            InputConnection editor,int words) {
        if(sensitive || editor==null || editor!=getCurrentInputConnection())return null;
        try {
            return BackspaceSwipeSnapshotGuard.capture(
                    editor.getTextBeforeCursor(BackspaceSwipeSnapshotGuard.BEFORE_LIMIT,0),
                    editor.getSelectedText(0),
                    editor.getTextAfterCursor(BackspaceSwipeSnapshotGuard.AFTER_LIMIT,0),
                    hasSelection(),words);
        } catch(RuntimeException detachedEditor) {
            return null;
        }
    }
    private boolean isSwipeDeleteTicketCurrent(InputConnection editor,
            BackspaceSwipeSnapshotGuard.Ticket ticket,int words) {
        if(ticket==null || sensitive || editor==null ||
                editor!=getCurrentInputConnection())return false;
        try {
            return BackspaceSwipeSnapshotGuard.isCurrent(ticket,
                    editor.getTextBeforeCursor(BackspaceSwipeSnapshotGuard.BEFORE_LIMIT,0),
                    editor.getSelectedText(0),
                    editor.getTextAfterCursor(BackspaceSwipeSnapshotGuard.AFTER_LIMIT,0),
                    hasSelection(),words);
        } catch(RuntimeException detachedEditor) {
            return false;
        }
    }
    private void showSwipeDeletePreview(InputConnection editor,int words,
            BackspaceSwipeSnapshotGuard.Ticket ticket) {
        if(editor==null || editor!=getCurrentInputConnection() || words<=0 ||
                swipeDeletePreview==null || suggestionScroll==null || sensitive) {
            hideSwipeDeletePreview();
            return;
        }
        // Only display text from the same ephemeral snapshot later checked
        // before deletion. Never promise a deletion when the editor rejects
        // a reliable snapshot; the gesture will safely do nothing.
        boolean selectionAvailable=BackspaceSwipeSnapshotGuard.previewsSelection(ticket);
        String target=BackspaceSwipeSnapshotGuard.previewText(ticket,38);
        String prefix=selectionAvailable ? "Delete selection: " :
                "Delete "+words+(words==1?" word: ":" words: ");
        if(target.isEmpty()) {
            swipeDeletePreview.setText("Swipe delete unavailable");
        } else {
            android.text.SpannableStringBuilder preview=
                    new android.text.SpannableStringBuilder(prefix);
            int start=preview.length();
            preview.append(target);
            preview.setSpan(new android.text.style.BackgroundColorSpan(
                            Color.argb(95,Color.red(accent),Color.green(accent),
                                    Color.blue(accent))),
                    start,preview.length(),
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            preview.setSpan(new android.text.style.StyleSpan(
                            android.graphics.Typeface.BOLD),
                    start,preview.length(),
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            swipeDeletePreview.setText(preview);
        }
        suggestionScroll.setVisibility(View.GONE);
        swipeDeletePreview.setVisibility(View.VISIBLE);
    }
    private void attachRepeatingBackspace(Button key) {
        // Tap: one character; hold: repeat; slide left: delete whole words.
        // A tap is committed AFTER release, so a swipe never accidentally
        // removes the first character before gesture recognition.
        key.setContentDescription("Delete. Hold to repeat, or slide left to delete words");
        key.setOnClickListener(v -> backspace()); // accessibility fallback
        final float[] touchStart={0f,0f};
        final InputConnection[] gestureEditor={null};
        final long[] gestureSession={0L};
        final boolean[] swiping={false}, repeating={false}, cancelled={false}, moved={false};
        final int[] swipeWords={0};
        final BackspaceSwipeSnapshotGuard.Ticket[] swipeTicket={null};
        key.setOnTouchListener((view,event)->{
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    stopDeleteRepeat();
                    hideSwipeDeletePreview();
                    gestureEditor[0]=getCurrentInputConnection();
                    gestureSession[0]=inputSessionGeneration;
                    deleteRepeatEditor=gestureEditor[0];
                    deleteRepeatSession=gestureSession[0];
                    backspaceTouchActive=true;
                    touchStart[0]=event.getRawX();
                    touchStart[1]=event.getRawY();
                    swiping[0]=false;
                    repeating[0]=false;
                    cancelled[0]=false;
                    moved[0]=false;
                    swipeWords[0]=0;
                    swipeTicket[0]=null;
                    deleteHeld=true; // suppress key-view rebuild during gesture
                    key.setPressed(true);
                    animateKeyPress(key,true);
                    keyHaptic(key);
                    freezeRiseDuringDelete=true;
                    Runnable hold=()->{
                        deleteHoldPending=null;
                        if(!BackspaceRepeatGuard.startAllowed(
                                backspaceTouchActive,deleteHeld,cancelled[0],
                                swiping[0],moved[0],gestureSession[0],
                                inputSessionGeneration,gestureEditor[0],
                                getCurrentInputConnection()))return;
                        repeating[0]=true;
                        backspace();
                        deleteHandler.postDelayed(deleteRepeat,DELETE_REPEAT_MS);
                    };
                    deleteHoldPending=hold;
                    deleteHandler.postDelayed(hold,DELETE_HOLD_DELAY_MS);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if(cancelled[0] || repeating[0])return true;
                    float left=touchStart[0]-event.getRawX();
                    float vertical=event.getRawY()-touchStart[1];
                    // Let a deliberate slow slide win over the 380ms hold
                    // before it reaches the larger 34dp whole-word threshold.
                    // A released gesture that moved beyond tap slop is not a tap.
                    if(BackspaceSwipePolicy.beyondTapSlop(left,vertical,dp(12))) {
                        moved[0]=true;
                        if(deleteHoldPending!=null) {
                            deleteHandler.removeCallbacks(deleteHoldPending);
                            deleteHoldPending=null;
                        }
                    }
                    int words=BackspaceSwipePolicy.wordsForSwipe(
                            left,vertical,dp(34),dp(78));
                    if(words>0) {
                        if(!swiping[0]) {
                            swiping[0]=true;
                            // Stop hold detection; restore the active gesture flag.
                            if(deleteHoldPending!=null) {
                                deleteHandler.removeCallbacks(deleteHoldPending);
                                deleteHoldPending=null;
                            }
                            keyHaptic(key);
                        }
                        // Avoid querying editor text/redrawing on every
                        // motion frame: refresh only as the word count changes.
                        if(swipeWords[0]!=words) {
                            swipeWords[0]=words;
                            swipeTicket[0]=captureSwipeDeleteTicket(gestureEditor[0],words);
                            showSwipeDeletePreview(gestureEditor[0],words,swipeTicket[0]);
                            key.setText("⌫"+words);
                            key.setContentDescription("Delete "+words+
                                    (words==1?" word":" words")+" on release");
                        }
                    } else if(swiping[0]) {
                        // Sliding back toward the original key cancels deletion.
                        swipeWords[0]=0;
                        swipeTicket[0]=null;
                        hideSwipeDeletePreview();
                        key.setText("⌫");
                        key.setContentDescription("Swipe deletion cancelled");
                    } else if(Math.abs(left)>dp(60) || Math.abs(vertical)>dp(60)) {
                        cancelled[0]=true;
                        stopDeleteRepeat();
                        hideSwipeDeletePreview();
                    }
                    return true;
                }
                case MotionEvent.ACTION_POINTER_DOWN:
                    cancelled[0]=true;
                    swipeTicket[0]=null;
                    stopDeleteRepeat();
                    hideSwipeDeletePreview();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    // Some hosts coalesce fast motion into the final UP event.
                    // Check its coordinates without allowing a cancelled or
                    // already repeating hold to turn into swipe deletion.
                    if(event.getActionMasked()==MotionEvent.ACTION_UP &&
                            !cancelled[0] && !repeating[0]) {
                        float left=touchStart[0]-event.getRawX();
                        float vertical=event.getRawY()-touchStart[1];
                        if(BackspaceSwipePolicy.beyondTapSlop(left,vertical,dp(12)))
                            moved[0]=true;
                        int finalWords=BackspaceSwipePolicy.wordsForSwipe(
                                left,vertical,dp(34),dp(78));
                        if(finalWords>0 && !swiping[0]) {
                            // Coalesced MOVE: a real swipe may first be visible
                            // at release. No editor mutation happens here.
                            swiping[0]=true;
                            swipeWords[0]=finalWords;
                            swipeTicket[0]=captureSwipeDeleteTicket(
                                    gestureEditor[0],finalWords);
                        } else if(swiping[0] && finalWords!=swipeWords[0]) {
                            // Finger moved after the last preview. Never delete
                            // an unpreviewed amount, even if it is larger.
                            swipeWords[0]=0;
                            swipeTicket[0]=null;
                        }
                    }
                    final InputConnection editor=gestureEditor[0];
                    final long sessionAtDown=gestureSession[0];
                    boolean sameSession=BackspaceSwipePolicy.sameEditorSession(
                            sessionAtDown,inputSessionGeneration,
                            editor,getCurrentInputConnection());
                    boolean shouldCommit=event.getActionMasked()==MotionEvent.ACTION_UP &&
                            !cancelled[0] && sameSession;
                    int words=swiping[0]?swipeWords[0]:0;
                    final BackspaceSwipeSnapshotGuard.Ticket ticket=swipeTicket[0];
                    boolean tap=shouldCommit && !swiping[0] && !repeating[0] && !moved[0];
                    boolean swipe=shouldCommit && swiping[0] && words>0 && ticket!=null;
                    swipeTicket[0]=null; // transient content cannot outlive the gesture
                    stopDeleteRepeat();
                    backspaceTouchActive=false;
                    freezeRiseDuringDelete=false;
                    hideSwipeDeletePreview();
                    key.setText("⌫");
                    key.setContentDescription(
                            "Delete. Hold to repeat, or slide left to delete words");
                    key.setPressed(false);
                    animateKeyPress(key,false);
                    // Android 17 Build 287 crash: DO NOT change the view tree
                    // or editor while the Backspace touch event is dispatching.
                    suggestionHandler.post(()->{
                        if(!BackspaceSwipePolicy.sameEditorSession(
                                sessionAtDown,inputSessionGeneration,
                                editor,getCurrentInputConnection()))return;
                        if(swipe)deleteSwipeWords(words,editor,ticket,sessionAtDown);
                        else if(tap)backspace();
                        // refreshView already updates the suggestion strip.
                        refreshView();
                    });
                    return true;
                }
                default: return true;
            }
        });
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable rounded(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(11));
        d.setStroke(Math.max(1,dp(.7f)),borderColor);
        return d;
    }
    private RippleDrawable feedbackBackground(int normalColor) {
        // Android platform ripple animation: no heavy per-tap ValueAnimator
        // and no moving touch targets during fast typing.
        GradientDrawable content=rounded(normalColor);
        GradientDrawable mask=new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(11));
        return new RippleDrawable(ColorStateList.valueOf(rippleColor),content,mask);
    }
    private String themeSource = "Neutral fallback";
    private boolean themeDark = true;
    private String themeDetails() {
        int mode=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                .getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
        return ThemeStatusLabel.details(AppPalette.modeName(mode),themeDark,
                AppPalette.followsSystem(this),themeSource);
    }
    private void updateThemeStatus() {
        if(paletteLabel==null)return;
        int mode=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                .getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
        paletteLabel.setText(ThemeStatusLabel.compact(
                AppPalette.modeName(mode),themeDark));
        paletteLabel.setTextColor(secondaryTextColor);
        paletteLabel.setContentDescription(themeDetails()+". Tap for full theme information.");
    }
    private void setColors(EditorInfo editor) {
        AppPalette.Result result=AppPalette.resolve(this,editor==null?null:editor.packageName);
        background=result.background;
        keyBackground=result.keyBackground;
        accent=result.accent;
        themeSource=result.label;
        themeDark=result.dark;
        updateThemeStatus();
        keyTextColor=result.keyTextColor;
        secondaryTextColor=result.secondaryTextColor;
        borderColor=result.borderColor;
        specialBackground=result.specialBackground;
        suggestionBackground=result.suggestionBackground;
        suggestionTextColor=result.suggestionTextColor;
        rippleColor=result.rippleColor;
        syncNavigationBar();
    }
    private void syncNavigationBar() {
        Window window=getWindow()==null?null:getWindow().getWindow();
        if(window==null)return;
        window.setNavigationBarColor(background);
        if(Build.VERSION.SDK_INT>=29) window.setNavigationBarContrastEnforced(false);
        int flags=window.getDecorView().getSystemUiVisibility();
        boolean light=AppPaletteText.contrast(background)==Color.BLACK;
        window.getDecorView().setSystemUiVisibility(light ?
                flags|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR :
                flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    @Override public View onCreateInputView() {
        base = new LinearLayout(this);
        base.setOrientation(LinearLayout.VERTICAL);
        base.setMotionEventSplittingEnabled(true);
        base.setPadding(dp(3), dp(3), dp(3), dp(7));
        suggestionScroll = new HorizontalScrollView(this);
        suggestionScroll.setHorizontalScrollBarEnabled(false);
        suggestionScroll.setFillViewport(true);
        candidates = new LinearLayout(this);
        candidates.setOrientation(LinearLayout.HORIZONTAL);
        candidates.setGravity(Gravity.CENTER_VERTICAL);
        suggestionScroll.addView(candidates);
        // Keep the prediction strip as the only always-visible top row.
        // Theme details and the theme characters/buttons occupy separate
        // rows in a panel that becomes completely GONE when collapsed.
        themePanel = new LinearLayout(this);
        themePanel.setOrientation(LinearLayout.VERTICAL);
        themePanel.setMotionEventSplittingEnabled(true);
        paletteBar = new LinearLayout(this);
        paletteBar.setGravity(Gravity.CENTER_VERTICAL);
        paletteBar.setMotionEventSplittingEnabled(true);
        paletteControls = new LinearLayout(this);
        paletteControls.setGravity(Gravity.CENTER_VERTICAL);
        paletteControls.setMotionEventSplittingEnabled(true);
        paletteLabel = new TextView(this);
        paletteLabel.setTextColor(secondaryTextColor);
        paletteLabel.setSingleLine(true);
        paletteLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        paletteLabel.setTextSize(10);
        // Small always-visible status. Tapping explains the full active mode
        // and the applied light/dark source, without opening a modal overlay.
        paletteLabel.setOnClickListener(v->
                android.widget.Toast.makeText(this,themeDetails(),
                        android.widget.Toast.LENGTH_LONG).show());
        paletteLabel.setPadding(dp(8), 0, dp(5), 0);
        paletteBar.addView(paletteLabel, new LinearLayout.LayoutParams(0, dp(28), 1f));
        paletteButton = new Button(this);
        // The palette icon belongs to the expanded SETTINGS panel, not
        // the prediction strip. The theme header shows mode and Light/Dark.
        paletteButton.setText("🎨");
        paletteButton.setTextSize(17);
        paletteButton.setAllCaps(false);
        paletteButton.setTextColor(keyTextColor);
        paletteButton.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams paletteParams = new LinearLayout.LayoutParams(dp(45), dp(34));
        paletteParams.setMargins(dp(3), 0, dp(4), dp(2));
        paletteControls.addView(paletteButton, paletteParams);
        paletteButton.setVisibility(View.GONE);
        paletteButton.setContentDescription("Change keyboard color style");
        paletteButton.setOnLongClickListener(v -> {
            android.widget.Toast.makeText(this,themeDetails(),
                    android.widget.Toast.LENGTH_LONG).show();
            return true;
        });
        paletteButton.setOnClickListener(v -> {
            int current = getSharedPreferences(AppPalette.PREFS, MODE_PRIVATE)
                    .getInt(AppPalette.KEY_MODE, AppPalette.DEFAULT);
            int next = AppPalette.nextMode(current);
            getSharedPreferences(AppPalette.PREFS, MODE_PRIVATE).edit()
                    .putInt(AppPalette.KEY_MODE, next).apply();
            setColors(getCurrentInputEditorInfo());
            refreshView();
        });
        sizeButton=new Button(this);
        sizeButton.setText("↕");
        sizeButton.setTextSize(21);
        sizeButton.setAllCaps(false);
        sizeButton.setTextColor(keyTextColor);
        sizeButton.setPadding(0,0,0,0);
        LinearLayout.LayoutParams sizeParams=new LinearLayout.LayoutParams(dp(44),dp(34));
        sizeParams.setMargins(dp(2),0,dp(3),dp(2));
        paletteControls.addView(sizeButton,sizeParams);
        sizeButton.setVisibility(View.GONE);
        sizeButton.setOnClickListener(v -> {
            int current=getSharedPreferences(KeyboardSizing.PREFS,MODE_PRIVATE)
                    .getInt(KeyboardSizing.KEY_MODE,KeyboardSizing.DEFAULT);
            int next=KeyboardSizing.next(current);
            getSharedPreferences(KeyboardSizing.PREFS,MODE_PRIVATE).edit()
                    .putInt(KeyboardSizing.KEY_MODE,next).apply();
            stopDeleteRepeat();
            refreshView();
        });
        // A deliberate, confirmed reset keeps learning reversible.
        sizeButton.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Clear Orivra learning?")
                .setMessage("Delete all locally learned corrections? This cannot be undone.")
                .setNegativeButton("Cancel", (dialog, which) -> {})
                .setPositiveButton("Clear", (dialog, which) -> {
                    PersonalLearning.clear(this);
                    android.widget.Toast.makeText(this,
                        "Local learning cleared and disabled", android.widget.Toast.LENGTH_SHORT).show();
                }).show();
            return true;
        });
        animationSpeedButton=new Button(this);
        animationSpeedButton.setAllCaps(false);
        animationSpeedButton.setTextSize(12);
        animationSpeedButton.setPadding(0,0,0,0);
        animationSpeedButton.setOnClickListener(v->{
            int current=riseMode();
            int next=current==400?600:current==600?800:current==800?1000:400;
            getSharedPreferences("keyboard_feedback",MODE_PRIVATE).edit()
                    .putInt(ANIMATION_PREF,next).apply();
            updateToolbar();
            android.widget.Toast.makeText(this,
                    "Prediction panel hold: "+next+" ms (no fading)",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        // Double-duty diagnostic access: tap-and-hold the suggestion row
        // control to inspect learning statistics without exporting text.
        animationSpeedButton.setContentDescription(
                "Prediction panel hold time; long press toggles local learning");
        // Long-press the rise control to opt into local learning.
        // Long-press again to disable; learned entries remain until cleared.
        animationSpeedButton.setOnLongClickListener(v -> {
            // An IME is not an Activity. An ordinary AlertDialog may lack a
            // valid application window token while the keyboard is displayed,
            // appearing to freeze or crashing the IME on some Samsung devices.
            // Keep opt-in immediate, reversible, and non-modal.
            boolean next = !PersonalLearning.enabled(this);
            PersonalLearning.setEnabled(this, next);
            personalPrefixCache340.clear();
            tappedSequence340.clear();
            clearTapSequence340();
            android.widget.Toast.makeText(this,
                    next ? "Orivra Learning V2 ON (local explicit corrections)"
                         : "Orivra learning OFF",
                    android.widget.Toast.LENGTH_LONG).show();
            return true;
        });
        paletteControls.addView(animationSpeedButton,
                new LinearLayout.LayoutParams(dp(66),dp(34)));
        splitModeButton=new Button(this);
        splitModeButton.setAllCaps(false);
        splitModeButton.setTextSize(12);
        splitModeButton.setPadding(0,0,0,0);
        splitModeButton.setOnClickListener(v->toggleSplitPredictionMode());
        // Intentional opt-in without adding another row to the keyboard.
        // Tap Split for existing behavior; hold it to toggle seed predictions.
        splitModeButton.setOnLongClickListener(v -> {
            toggleOfflineNextWordMode();
            return true;
        });
        paletteControls.addView(splitModeButton,new LinearLayout.LayoutParams(dp(80),dp(34)));
        // Dedicated one-tap control: Android IME toast messages may be hidden,
        // and long-press-only settings provide no obvious state indicator.
        nextWordButton=new Button(this);
        nextWordButton.setAllCaps(false);
        nextWordButton.setTextSize(11);
        nextWordButton.setPadding(0,0,0,0);
        nextWordButton.setOnClickListener(v -> {
            keyHaptic(v);
            toggleOfflineNextWordMode();
        });
        paletteControls.addView(nextWordButton,
                new LinearLayout.LayoutParams(dp(60),dp(34)));
        // Accessible through a long-press on the existing prediction toggle.
        // This displays only aggregate counts, never learned word pairs.

        // One arrow in the prediction strip controls all expandable UI.
        // Preserve haptic-style cycling on the theme header long-press,
        // rather than keeping a redundant second collapse arrow.
        paletteLabel.setOnLongClickListener(v -> {
            int current=hapticsEnabled()?hapticStyle():0;
            int next=KeyboardHapticStyle.next(current);
            getSharedPreferences("keyboard_feedback", MODE_PRIVATE).edit()
                    .putBoolean(HAPTIC_PREF,next!=0)
                    .putInt(HAPTIC_STYLE_PREF,next==0?2:next).apply();
            if(next!=0)keyHaptic(v);
            android.widget.Toast.makeText(this,
                    "Key haptics "+KeyboardHapticStyle.label(next),
                    android.widget.Toast.LENGTH_SHORT).show();
            return true;
        });
        themePanel.addView(paletteBar,new LinearLayout.LayoutParams(-1,dp(28)));
        themePanel.addView(paletteControls,new LinearLayout.LayoutParams(-1,dp(36)));
        ThemePanelLayout.apply(themePanel,false);
        predictionHeader=new LinearLayout(this);
        predictionHeader.setOrientation(LinearLayout.HORIZONTAL);
        predictionHeader.setGravity(Gravity.CENTER_VERTICAL);
        // Suggestions occupy the full available prediction-strip width.
        // A short tap on the existing right caret expands predictions; a
        // long-press on that SAME caret expands/collapses settings.
        LinearLayout.LayoutParams stripParams=new LinearLayout.LayoutParams(0,dp(48),1f);
        predictionHeader.addView(suggestionScroll,stripParams);
        // Reuse a dedicated, initially hidden slot in the existing strip.
        // Never add/remove child views during a Backspace touch gesture.
        swipeDeletePreview=new TextView(this);
        swipeDeletePreview.setSingleLine(true);
        swipeDeletePreview.setEllipsize(TextUtils.TruncateAt.START);
        swipeDeletePreview.setGravity(Gravity.CENTER_VERTICAL);
        swipeDeletePreview.setTextSize(14);
        swipeDeletePreview.setTextColor(suggestionTextColor);
        swipeDeletePreview.setPadding(dp(10),0,dp(8),0);
        swipeDeletePreview.setBackground(rounded(suggestionBackground));
        swipeDeletePreview.setVisibility(View.GONE);
        swipeDeletePreview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        predictionHeader.addView(swipeDeletePreview,
                new LinearLayout.LayoutParams(0,dp(48),1f));
        predictionsToggle=new Button(this);
        predictionsToggle.setText("⌃");
        predictionsToggle.setAllCaps(false);
        predictionsToggle.setTextSize(17);
        predictionsToggle.setPadding(0,0,0,0);
        predictionsToggle.setContentDescription("Tap to expand sentence predictions. Hold for theme and keyboard settings.");
        predictionHeader.addView(predictionsToggle,new LinearLayout.LayoutParams(dp(38),dp(44)));
        predictionsToggle.setOnClickListener(v->{
            if(toolbarExpanded)suggestionHandler.post(()->setThemePanelExpanded(false));
            else toggleRisePanel();
        });
        // Keep the existing right caret: short tap for sentence predictions,
        // 500ms hold for settings. No dedicated icon steals prediction space.
        // A normal Button long-click can be swallowed by fast IME touch handling.
        final boolean[] riseLongPressed={false};
        final Runnable riseHold=()->{
            riseLongPressed[0]=true;
            suggestionHandler.post(() -> setThemePanelExpanded(!toolbarExpanded));
        };
        predictionsToggle.setOnTouchListener((v,event)->{
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    riseLongPressed[0]=false;
                    v.setPressed(true);
                    suggestionHandler.postDelayed(riseHold,500L);
                    return true;
                case MotionEvent.ACTION_UP:
                    suggestionHandler.removeCallbacks(riseHold);
                    v.setPressed(false);
                    if(!riseLongPressed[0]) {
                        if(toolbarExpanded) {
                            // A single short tap closes settings when open.
                            suggestionHandler.post(()->setThemePanelExpanded(false));
                        } else toggleRisePanel();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    suggestionHandler.removeCallbacks(riseHold);
                    v.setPressed(false);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if(event.getX()<0 || event.getX()>v.getWidth()
                            || event.getY()<0 || event.getY()>v.getHeight()) {
                        suggestionHandler.removeCallbacks(riseHold);
                    }
                    return true;
                default:return true;
            }
        });
        expandedSuggestions=new LinearLayout(this);
        expandedSuggestions.setOrientation(LinearLayout.VERTICAL);
        base.addView(expandedSuggestions,new LinearLayout.LayoutParams(-1,-2));
        base.addView(predictionHeader,new LinearLayout.LayoutParams(-1,dp(48)));
        // Keep children mounted for the entire input-view lifetime; toggling
        // uses visibility instead of removeView/addView during input gestures.
        base.addView(themePanel,new LinearLayout.LayoutParams(-1,-2));
        updateToolbar();
        updatePredictionPanel();
        keys = new LinearLayout(this);
        keys.setOrientation(LinearLayout.VERTICAL);
        keys.setMotionEventSplittingEnabled(true);
        base.addView(keys);
        refreshView();
        return base;
    }
    @Override public void onConfigurationChanged(Configuration newConfig) {
        invalidateEditingTicket();
        super.onConfigurationChanged(newConfig);
        invalidateBackspaceGestureSession();
        stopSpacebarCursorGesture();
        setColors(getCurrentInputEditorInfo());
        refreshView();
    }
    @Override public void onStartInput(EditorInfo info, boolean restarting) {
        cancelStableWord329();
        stableWordCounted329="";
        stableWordSession329=-1L;
        invalidateEditingTicket();
        predictionDispatcher.cancel();
        personalPrefixCache340.clear();
        splitSnapshot340=null;
        tappedSequence340.clear();
        clearTapSequence340();
        invalidateBackspaceGestureSession();
        stopSpacebarCursorGesture();
        super.onStartInput(info, restarting);
        composing = "";
        editingInMiddle = false;
        lastSelectionStart = lastSelectionEnd = -1;
        EditorSuggestionPrivacyPolicy.Decision privacy=editorPrivacyDecision(info);
        sensitive=privacy.sensitive;
        editorAllowsPersonalLearning=privacy.allowPersonalLearning;
        editorAllowsSuggestions=privacy.allowSuggestions;
        reconcileOptionalDictionary();
        numericEditor=NumericKeypadPolicy323.isNumericField(
                info==null?0:info.inputType);
        numericAlphabeticOverride=false;
        symbols=false;
        extraSymbols=false;
        lastTypingTime=0L;
        averageTypingInterval=0;
        predictionsExpanded=false;
        manuallyCollapsedPredictions=false;
        shift=false;
        capsLock=false;
        setColors(info);
    }
    private void updateSentenceShift() {
        if (capsLock || sensitive || symbols) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || hasSelection()) return;
        // Re-evaluate at the current insertion point even inside existing
        // text. Previously a cursor with text after it retained stale Shift
        // from the end-of-document state, capitalizing mid-sentence fixes.
        shift = SentenceCapitalization.shouldCapitalize(ic.getTextBeforeCursor(160, 0));
    }
    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        invalidateEditingTicket();
        invalidateBackspaceGestureSession();
        stopSpacebarCursorGesture();
        super.onStartInputView(info, restarting);
        setColors(info);
        updateSentenceShift();
        refreshView();
    }
    @Override public void onFinishInput() {
        cancelStableWord329();
        stableWordCounted329="";
        stableWordSession329=-1L;
        tappedSequence340.clear();
        clearTapSequence340();
        invalidateEditingTicket();
        predictionDispatcher.cancel();
        invalidateBackspaceGestureSession();
        stopSpacebarCursorGesture();
        currentFinish();
        super.onFinishInput();
    }
    @Override public void onFinishInputView(boolean finishingInput) {
        // A hidden keyboard must not interpret a remaining draft as a
        // completed word. An app's own Send button is not proof of delivery.
        cancelStableWord329();
        stableWordCounted329="";
        stableWordSession329=-1L;
        invalidateEditingTicket();
        predictionDispatcher.cancel();
        invalidateBackspaceGestureSession();
        stopSpacebarCursorGesture();
        super.onFinishInputView(finishingInput);
    }
    @Override public void onDestroy() {
        cancelStableWord329();
        invalidateEditingTicket();
        predictionDispatcher.close();
        closeOptionalDictionary();
        invalidateBackspaceGestureSession();
        stopSpacebarCursorGesture();
        super.onDestroy();
    }
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart,oldSelEnd,newSelStart,newSelEnd,
                candidatesStart,candidatesEnd);
        if(lastSelectionStart!=newSelStart || lastSelectionEnd!=newSelEnd)
            invalidateEditingTicket();
        lastSelectionStart=newSelStart;
        lastSelectionEnd=newSelEnd;
        if(singleBackspaceMutation338 || composingTransaction340)return;
        // Suppress per-character prediction work while the finger is moving.
        if(cursorSwipeNavigating || cursorReleaseSuggestionsPending)return;
        if(composing.isEmpty()) {
            // Cursor moves outside composition must refresh word-under-cursor predictions.
            // The previous early return made the preview path unreachable.
            if(newSelStart!=oldSelStart || newSelEnd!=oldSelEnd) {
                predictionDispatcher.cancel();
                editingInMiddle=cursorHasTextAfter() || !wordAtCursor().isEmpty();
                showSuggestions();
            }
            return;
        }
        // A tap or drag that leaves the active composing span must never be
        // overwritten by the next setComposingText() call.
        boolean outside=candidatesStart<0 || candidatesEnd<0 ||
                newSelStart<candidatesStart || newSelEnd>candidatesEnd ||
                newSelStart!=newSelEnd ||
                newSelStart!=candidatesEnd;
        if(outside) {
            predictionDispatcher.cancel();
            InputConnection ic=getCurrentInputConnection();
            if(ic!=null)ic.finishComposingText();
            composing="";
            editingInMiddle=true;
            showSuggestions();
        }
    }
    private boolean cursorHasTextAfter() {
        InputConnection ic=getCurrentInputConnection();
        if(ic==null)return false;
        CharSequence after=ic.getTextAfterCursor(1,0);
        return after!=null && after.length()>0;
    }
    private boolean hasSelection() {
        return lastSelectionStart>=0 && lastSelectionEnd>=0 &&
                lastSelectionStart!=lastSelectionEnd;
    }
    private void prepareCursorEdit(InputConnection ic) {
        if(!composing.isEmpty() && (cursorHasTextAfter() || hasSelection())) {
            ic.finishComposingText();
            composing="";
            predictionDispatcher.cancel();
        }
        editingInMiddle=cursorHasTextAfter() || hasSelection();
    }
    // Exposed to package-scoped Android instrumentation, no editor text access.
    static EditorSuggestionPrivacyPolicy.Decision editorPrivacyDecision(EditorInfo info) {
        if(info==null)
            return EditorSuggestionPrivacyPolicy.decide(null,false,true,true,true);
        int input=info.inputType;
        int cls=input & InputType.TYPE_MASK_CLASS;
        int variation=input & InputType.TYPE_MASK_VARIATION;
        boolean identifierOrPassword=
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_URI;
        // Real name and address editors are normal text (gestures still work),
        // but their content must not update the local learned-word store.
        boolean personalDataVariation=
                variation == InputType.TYPE_TEXT_VARIATION_PERSON_NAME ||
                variation == InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_PHONETIC;
        boolean noSuggestions=(input & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)!=0;
        boolean noPersonalizedLearning=(info.imeOptions &
                EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0;
        int action=info.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean searchOrGo=action==EditorInfo.IME_ACTION_SEARCH ||
                action==EditorInfo.IME_ACTION_GO;
        return EditorSuggestionPrivacyPolicy.decide(info.packageName,
                cls==InputType.TYPE_CLASS_TEXT,identifierOrPassword,
                noSuggestions,noPersonalizedLearning,searchOrGo,
                personalDataVariation);
    }
    private void currentFinish() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.finishComposingText();
        composing = "";
    }
    private void setThemePanelExpanded(boolean expand) {
        if(base==null || themePanel==null || backspaceTouchActive ||
                enterTouchActive || spacebarTouchActive) return;
        toolbarExpanded=expand;
        updateToolbar();
        // The same arrow reports the current settings/prediction state.
        if(predictionsToggle!=null) {
            predictionsToggle.setText(toolbarExpanded?"⌄":predictionsExpanded?"⌄":"⌃");
            predictionsToggle.setContentDescription(toolbarExpanded?
                    "Close keyboard settings and return to predictions":
                    (predictionsExpanded?"Collapse predictions":"Expand predictions")+
                    ". Hold for keyboard settings");
        }
    }
    private void updateToolbar() {
        // Refresh collapsed status and expanded layout on every toggle,
        // including toggles performed without a keyboard-view rebuild.
        if(paletteLabel!=null) {
            updateThemeStatus();
            paletteLabel.setVisibility(View.VISIBLE);
        }
        if(splitModeButton!=null) {
            splitModeButton.setVisibility(toolbarExpanded?View.VISIBLE:View.GONE);
            splitModeButton.setText(splitPredictionsEnabled()?"Split ON":"Split OFF");
            splitModeButton.setTextColor(keyTextColor);
            splitModeButton.setBackground(feedbackBackground(specialBackground));
            splitModeButton.setContentDescription("Split predictions "+
                    (splitPredictionsEnabled()?"on":"off")+
                    ". Tap to toggle split mode. Hold to toggle next-word seeds.");
        }
        if(nextWordButton!=null) {
            boolean enabled=offlineNextWordEnabled();
            nextWordButton.setVisibility(toolbarExpanded?View.VISIBLE:View.GONE);
            nextWordButton.setText(NextWordControlLabels.buttonText(enabled));
            nextWordButton.setTextColor(enabled ? AppPaletteText.contrast(accent) : keyTextColor);
            nextWordButton.setBackground(feedbackBackground(enabled?accent:specialBackground));
            nextWordButton.setContentDescription(
                    NextWordControlLabels.contentDescription(enabled));
        }
        // The THEME PANEL is either fully present (two separate theme rows)
        // or fully GONE (zero reserved dp). The prediction strip never moves
        // into a combined theme/prediction row.
        ThemePanelLayout.apply(themePanel,toolbarExpanded);
        if(paletteBar!=null)paletteBar.setVisibility(View.VISIBLE);
        if(paletteControls!=null)paletteControls.setVisibility(View.VISIBLE);
        if (paletteButton != null) {
            paletteButton.setVisibility(toolbarExpanded ? View.VISIBLE : View.GONE);
            int selected=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                    .getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
            // Mode and applied appearance remain in the adjacent theme
            // header. This icon stays with settings, not with suggestions.
            paletteButton.setText("🎨");
            paletteButton.setContentDescription(themeDetails()+
                    ". Tap to select the next theme. Hold for theme information.");
        }
        if (sizeButton != null) sizeButton.setVisibility(toolbarExpanded ? View.VISIBLE : View.GONE);
        if(animationSpeedButton!=null) {
            animationSpeedButton.setVisibility(toolbarExpanded?View.VISIBLE:View.GONE);
            int mode=riseMode();
            animationSpeedButton.setText("Hold "+mode);
            animationSpeedButton.setTextColor(keyTextColor);
            animationSpeedButton.setBackground(feedbackBackground(specialBackground));
            animationSpeedButton.setContentDescription(
                    "Prediction panel hold "+mode+" milliseconds; no fade animation");
        }

    }
    private void toggleRisePanel() {
        suggestionHandler.removeCallbacks(delayedRiseDismiss);
        predictionsExpanded=!predictionsExpanded;
        manuallyCollapsedPredictions=!predictionsExpanded;
        manualCollapseSnapshot=latestSnapshot;
        updatePredictionPanel();
    }
    private void toggleOfflineNextWordMode() {
        boolean next=!offlineNextWordEnabled();
        // Never show NW ON if the optional bundled model cannot be opened.
        boolean canEnable=!next || offlineNextWordSeed()!=null;
        if(canEnable) {
            getSharedPreferences("keyboard_feedback",MODE_PRIVATE).edit()
                    .putBoolean(OFFLINE_NEXT_WORD_PREF,next).apply();
        }
        String message=!canEnable ? "Offline model unavailable; NW remains OFF" :
                next ? "Offline next-word predictions ON" :
                        "Offline next-word predictions OFF";
        android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_SHORT).show();
        // Visible state remains updated even if the OS suppresses IME toasts.
        updateToolbar();
        if(paletteLabel!=null) {
            paletteLabel.setContentDescription(
                    NextWordControlLabels.contentDescription(offlineNextWordEnabled()));
        }
        showSuggestions();
    }
    private void toggleSplitPredictionMode() {
        boolean enabled=!splitPredictionsEnabled();
        getSharedPreferences("keyboard_feedback",MODE_PRIVATE).edit()
                .putBoolean(SPLIT_PREDICTIONS_PREF,enabled).apply();
        android.widget.Toast.makeText(this,enabled ?
                "Split word + phrase predictions ON" :
                "Split word + phrase predictions OFF",android.widget.Toast.LENGTH_SHORT).show();
        manuallyCollapsedPredictions=false;
        predictionsExpanded=false;
        updateToolbar();
        showSuggestions();
    }
    private void updatePredictionPanel() {
        if(predictionsToggle==null || expandedSuggestions==null)return;
        // Preserve the expanded panel throughout a held backspace gesture.
        // Deletion still updates predictions; only panel motion is deferred.
        if(freezeRiseDuringDelete || riseChipPressed)return;
        predictionsToggle.setText(toolbarExpanded?"⌄":predictionsExpanded?"⌄":"⌃");
        predictionsToggle.setContentDescription(toolbarExpanded?
                "Close keyboard settings and return to predictions":
                (predictionsExpanded?"Collapse predictions":"Expand predictions")+
                ". Hold for keyboard settings");
        predictionsToggle.setTextColor(keyTextColor);
        // Avoid replacing the ripple drawable on every keystroke.
        if(!(predictionsToggle.getTag() instanceof Integer) ||
                ((Integer)predictionsToggle.getTag())!=specialBackground) {
            predictionsToggle.setBackground(feedbackBackground(specialBackground));
            predictionsToggle.setTag(specialBackground);
        }
        // Do not restart a panel animation for every incoming prediction.
        if(predictionsExpanded==panelWasVisible && !predictionsExpanded)return;
        // Keep the current touch targets in place while their contents are unchanged.
        expandedSuggestions.animate().cancel();
        boolean wasVisible=panelWasVisible;
        panelWasVisible=predictionsExpanded;
        if(!predictionsExpanded) {
            riseRenderedSignature="";
            expandedSuggestions.animate().cancel();
            expandedSuggestions.setAlpha(1f);
            expandedSuggestions.setTranslationY(0f);
            expandedSuggestions.setVisibility(View.GONE);
            expandedSuggestions.removeAllViews();
            return;
        }
        expandedSuggestions.setVisibility(View.VISIBLE);
        // A single slim row above the standard suggestion strip.
        // Show alternatives rather than repeating the first three strip items.
        // Split Mode should prefer phrases, but must not suppress the Rise
        // panel when no phrase is available. Restore additional predictions
        // above the strip without repeating the primary three choices.
        boolean splitActive=splitPredictionsEnabled();
        List<String> riseOptions;
        int start;
        if(splitActive && !splitPhrases.isEmpty()) {
            riseOptions=splitPhrases;
            start=0;
        } else {
            riseOptions=latestOptions;
            start=latestOptions.size()>3?3:0;
        }
        java.util.ArrayList<String> distinct=new java.util.ArrayList<>();
        for(int i=start;i<riseOptions.size() && distinct.size()<2;i++) {
            String value=riseOptions.get(i);
            if(value==null || value.trim().isEmpty())continue;
            if(!SuggestionDisplayPolicy.usefulRiseAlternative(latestSnapshot,value))continue;
            // Never repeat the primary suggestion in the expanded alternatives.
            if(!latestOptions.isEmpty() && value.equalsIgnoreCase(latestOptions.get(0)))continue;
            boolean repeated=false;
            for(String prior:distinct)if(prior.trim().equalsIgnoreCase(value.trim())) {
                repeated=true;break;
            }
            if(!repeated)distinct.add(value);
        }
        riseOptions=distinct;
        start=0;
        int count=riseOptions.size();
        if(count<=start) {
            predictionsExpanded=false;
            panelWasVisible=false;
            expandedSuggestions.setVisibility(View.GONE);
            expandedSuggestions.removeAllViews();
            predictionsToggle.setText("⌃");
            return;
        }
        String signature=latestSnapshot+"\u0001"+android.text.TextUtils.join("\u0001",riseOptions);
        if(wasVisible && signature.equals(riseRenderedSignature))return;
        riseRenderedSignature=signature;
        // Update chips in place while their count stays unchanged.
        // Removing/readding the whole row each keystroke caused visible flashing.
        boolean reuseRiseRow=wasVisible && expandedSuggestions.getChildCount()==1 &&
                expandedSuggestions.getChildAt(0) instanceof LinearLayout &&
                ((LinearLayout)expandedSuggestions.getChildAt(0)).getChildCount()==count*2-1;
        LinearLayout row;
        if(reuseRiseRow) {
            row=(LinearLayout)expandedSuggestions.getChildAt(0);
        } else {
            expandedSuggestions.removeAllViews();
            row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            expandedSuggestions.addView(row,new LinearLayout.LayoutParams(-1,dp(64)));
        }
        for(int i=start;i<count;i++) {
            final String candidate=riseOptions.get(i);
            final String expected=latestSnapshot;
            TextView item=reuseRiseRow ? (TextView)row.getChildAt((i-start)*2) : new TextView(this);
            item.setText(suggestionCase(candidate));
            item.setTextColor(suggestionTextColor);
            item.setTextSize(candidate.length()>28?14:16);
            item.setTypeface(android.graphics.Typeface.create(
                    "sans-serif",android.graphics.Typeface.NORMAL));
            item.setLetterSpacing(.004f);
            item.setIncludeFontPadding(false);
            item.setGravity(Gravity.CENTER);
            boolean phrase=candidate.indexOf(' ')>=0;
            item.setSingleLine(!phrase);
            item.setMaxLines(phrase?2:1);
            item.setEllipsize(TextUtils.TruncateAt.END);
            item.setPadding(dp(6),0,dp(6),0);
            item.setBackground(feedbackBackground(Color.TRANSPARENT));
            // No card backgrounds: match the regular suggestion strip.
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(60),1f);
            params.setMargins(dp(2),dp(1),dp(2),dp(1));
            if(!reuseRiseRow)row.addView(item,params);
            item.setOnTouchListener((v,event)->{
                int action=event.getActionMasked();
                if(action==MotionEvent.ACTION_DOWN)riseChipPressed=true;
                else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
                    riseChipPressed=false;
                    suggestionHandler.post(this::updatePredictionPanel);
                }
                return false;
            });
            item.setOnClickListener(v->{
                acceptSuggestion(v,expected,candidate);
                predictionsExpanded=false;
                updatePredictionPanel();
            });
            if(!reuseRiseRow && i<count-1) {
                View divider=new View(this);
                divider.setBackgroundColor(borderColor);
                row.addView(divider,new LinearLayout.LayoutParams(dp(1),dp(22)));
            }
        }
        // Keep expanded candidate labels stable: no alpha/translation effect
        // on every rapid new result or held-backspace transition.
        expandedSuggestions.setAlpha(1f);
        expandedSuggestions.setTranslationY(0f);
    }
    private void refreshView() {
        if (base == null || keys == null || spacebarTouchActive ||
                enterTouchActive ||
                !BackspaceRepeatGuard.mayRebuildKeys(backspaceTouchActive,deleteHeld))
            return;
        base.setBackgroundColor(background);
        if (paletteBar != null) paletteBar.setBackgroundColor(background);
        if (paletteControls != null) paletteControls.setBackgroundColor(background);
        if (themePanel != null) themePanel.setBackgroundColor(background);
        updateToolbar();
        if (paletteLabel != null) updateThemeStatus();
        if(paletteButton!=null) {
            paletteButton.setTextColor(keyTextColor);
            paletteButton.setBackground(feedbackBackground(specialBackground));
            paletteButton.setContentDescription(themeDetails()+
                    ". Tap to select the next theme. Hold for theme information.");
        }
        final int heightMode=KeyboardSizing.normalize(
                getSharedPreferences(KeyboardSizing.PREFS,MODE_PRIVATE)
                        .getInt(KeyboardSizing.KEY_MODE,KeyboardSizing.DEFAULT));
        if(sizeButton!=null) {
            sizeButton.setBackground(feedbackBackground(specialBackground));
            sizeButton.setTextColor(keyTextColor);
            sizeButton.setContentDescription("Keyboard size: "+KeyboardSizing.name(heightMode)
                    +". Tap to change size");
        }
        final boolean landscape=getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        keys.removeAllViews();
        final boolean numericPad=numericEditor&&!numericAlphabeticOverride;
        if(numericPad) {
            renderNumericKeypad323(heightMode,landscape);
            showSuggestions();
            return;
        }
        final int layoutMode=!symbols?KeyboardLayoutSpec.ABC:
                extraSymbols?KeyboardLayoutSpec.EXTRA:KeyboardLayoutSpec.SYMBOLS;
        final String[] rows=KeyboardLayoutSpec.rows(layoutMode);
        for (int index = 0; index < rows.length; index++) {
            currentRowHeightDp=KeyboardSizing.keyHeightDp(heightMode,index,landscape);
            LinearLayout row = new LinearLayout(this);
            row.setMotionEventSplittingEnabled(true);
            row.setGravity(Gravity.CENTER);
            if(index==2) {
                if(!symbols) {
                    Button shiftKey=addKey(row,capsLock?"⇪":"⇧",1.4f,()->{});
                    attachShiftKey(shiftKey);
                } else {
                    String pageKey=KeyboardLayoutSpec.thirdRowPrefix(layoutMode);
                    addKey(row,pageKey,1.4f,()->{
                        extraSymbols=!extraSymbols;
                        refreshView();
                    });
                }
            }
            for(int col=0;col<rows[index].length();col++) {
                final String value=String.valueOf(rows[index].charAt(col));
                final String visible=shift&&!symbols?value.toUpperCase():value;
                if(!symbols) {
                    String hint=KeyboardLayoutSpec.longPressHint(index,col);
                    addHintLetterKey(row,visible,value,hint);
                } else addKey(row,visible,1f,()->type(value));
            }
            if (index == 2) {
                Button deleteKey = addKey(row, "⌫", 1.4f, this::backspace);
                attachRepeatingBackspace(deleteKey);
            }
            keys.addView(row);
        }
        currentRowHeightDp=KeyboardSizing.keyHeightDp(heightMode,3,landscape);
        LinearLayout bottom = new LinearLayout(this);
        bottom.setMotionEventSplittingEnabled(true);
        addKey(bottom,KeyboardLayoutSpec.bottomModeLabel(layoutMode),
                1.3f,()->{
                    if(numericEditor) {
                        numericAlphabeticOverride=false;
                        symbols=false;
                    } else symbols=!symbols;
                    extraSymbols=false;
                    refreshView();
                });
        addKey(bottom,",",0.95f,()->type(","));
        {
            if(symbols) {
                final Button[] numberKey=new Button[1];
                numberKey[0]=addKey(bottom,"1234",0.95f,()->showNumberMenu(numberKey[0]));
                numberKey[0].setContentDescription("Number shortcuts");
            } else {
                final Button[] emojiKey=new Button[1];
                emojiKey[0]=addKey(bottom,"☺",0.95f,()->showEmojiMenu(emojiKey[0]));
                emojiKey[0].setContentDescription("Emoji and keyboard switcher");
            }
            Button spaceKey=addKey(bottom,"EN",4.6f,this::space);
            spaceKey.setContentDescription("Space; slide left or right to move cursor");
            attachSpacebarCursorControl(spaceKey);
        }
        addKey(bottom,".",0.95f,()->type("."));
        Button enterKey=addKey(bottom,"↵",1.3f,this::handleEnterTap);
        attachSafeEnterKey(enterKey);
        keys.addView(bottom);
        showSuggestions();
    }
    /**
     * Numeric/financial input: actual digits, decimal and deletion, with a
     * visible ABC escape hatch. No predictions or private learning occur.
     * Uses the same full-cell touch and haptic behavior as ordinary keys.
     */
    private void renderNumericKeypad323(int sizeMode,boolean landscape) {
        final EditorInfo editor=getCurrentInputEditorInfo();
        final int inputType=editor==null?0:editor.inputType;
        final String[] lines=NumericKeypadPolicy323.NUMBER_ROWS;
        for(int index=0;index<3;index++) {
            currentRowHeightDp=KeyboardSizing.keyHeightDp(sizeMode,index,landscape);
            LinearLayout row=new LinearLayout(this);
            row.setMotionEventSplittingEnabled(true);
            for(int col=0;col<3;col++) {
                String digit=String.valueOf(lines[index].charAt(col));
                addKey(row,digit,1f,()->type(digit));
            }
            keys.addView(row);
        }
        currentRowHeightDp=KeyboardSizing.keyHeightDp(sizeMode,3,landscape);
        LinearLayout last=new LinearLayout(this);
        last.setMotionEventSplittingEnabled(true);
        String separator=NumericKeypadPolicy323.secondarySymbol(inputType);
        if(separator.isEmpty()) {
            // Keep zero physically centered; blank slot has no touch action.
            View unused=new View(this);
            last.addView(unused,new LinearLayout.LayoutParams(
                    0,dp(currentRowHeightDp),1f));
        } else addKey(last,separator,1f,()->type(separator));
        addKey(last,"0",1f,()->type("0"));
        Button delete=addKey(last,"⌫",1f,this::backspace);
        attachRepeatingBackspace(delete);
        keys.addView(last);

        currentRowHeightDp=KeyboardSizing.keyHeightDp(sizeMode,3,landscape);
        LinearLayout actions=new LinearLayout(this);
        actions.setMotionEventSplittingEnabled(true);
        addKey(actions,"ABC",1.4f,()->{
            numericAlphabeticOverride=true;
            symbols=false;
            extraSymbols=false;
            refreshView();
        }).setContentDescription("Switch to letters temporarily");
        if(NumericKeypadPolicy323.isSigned(inputType))
            addKey(actions,"−",0.9f,()->type("-"))
                    .setContentDescription("Minus");
        Button enter=addKey(actions,"↵",2.1f,this::handleEnterTap);
        attachSafeEnterKey(enter);
        keys.addView(actions);
    }

    /**
     * Alphabet hints mirror the screenshot. A quick release types the letter;
     * a deliberate long press types its secondary symbol instead, never both.
     * This intentionally keeps the hit target constant and lets multi-touch
     * cancellations fail closed. Hint TextViews are display-only.
     */
    private void addHintLetterKey(LinearLayout row,String visible,String letter,String hint) {
        Button key=addKey(row,visible,1f,()->type(letter));
        LinearLayout.LayoutParams cellParams=(LinearLayout.LayoutParams)key.getLayoutParams();
        row.removeView(key);
        FrameLayout cell=new FrameLayout(this);
        row.addView(cell,cellParams);
        cell.addView(key,new FrameLayout.LayoutParams(-1,-1));
        TextView hintLabel=new TextView(this);
        hintLabel.setText(hint);
        hintLabel.setTextColor(secondaryTextColor);
        hintLabel.setTextSize(10);
        hintLabel.setGravity(Gravity.END|Gravity.TOP);
        // Anchor the hint INSIDE the inset keycap, not above the key row.
        hintLabel.setPadding(0,0,dp(7),0);
        hintLabel.setIncludeFontPadding(false);
        hintLabel.setClickable(false);
        hintLabel.setFocusable(false);
        hintLabel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams hintParams=
                new FrameLayout.LayoutParams(-1,dp(22),Gravity.TOP);
        hintParams.topMargin=dp(11);
        cell.addView(hintLabel,hintParams);
        key.setContentDescription(visible+", hold for "+hint);
        final boolean[] triggered={false},cancelled={false};
        final Runnable hold=()->{
            if(!cancelled[0]&&key.isPressed()&&getCurrentInputConnection()!=null) {
                triggered[0]=true;
                keyHaptic(key);
                symbolHoldInProgress=true;
                try { type(hint); }
                finally { symbolHoldInProgress=false; }
            }
        };
        key.setOnTouchListener((view,event)->{
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    cancelled[0]=false;
                    triggered[0]=false;
                    key.setPressed(true);
                    animateKeyPress(key,true);
                    keyHaptic(key);
                    suggestionHandler.postDelayed(hold,ViewConfiguration.getLongPressTimeout());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if(event.getPointerCount()!=1 ||
                            event.getX() < -dp(10) ||
                            event.getX()>key.getWidth()+dp(10) ||
                            event.getY() < -dp(10) ||
                            event.getY()>key.getHeight()+dp(10)) {
                        cancelled[0]=true;
                        suggestionHandler.removeCallbacks(hold);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    suggestionHandler.removeCallbacks(hold);
                    key.setPressed(false);
                    // Text commitment has priority over release cosmetics.
                    // The symbol-hold gesture still owns the original key:
                    // never type a second letter after a long press/cancel.
                    if(!cancelled[0]&&!triggered[0])type(letter);
                    else if(triggered[0])suggestionHandler.post(this::refreshView);
                    animateKeyPress(key,false);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_POINTER_UP:
                    cancelled[0]=true;
                    suggestionHandler.removeCallbacks(hold);
                    key.setPressed(false);
                    animateKeyPress(key,false);
                    return true;
                default:return true;
            }
        });
    }

    private void showEmojiMenu(Button anchor) {
        if(anchor==null)return;
        android.widget.PopupMenu menu=new android.widget.PopupMenu(this,anchor);
        String[] quick={"😀","😂","😍","👍","❤️","😊","😭","🙏","🎉","💯"};
        for(String value:quick)
            menu.getMenu().add(value).setOnMenuItemClickListener(item->{
                type(value);
                return true;
            });
        menu.getMenu().add("Switch keyboard").setOnMenuItemClickListener(item->{
            InputMethodManager manager=(InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if(manager!=null)manager.showInputMethodPicker();
            return true;
        });
        menu.show();
    }
    private void showNumberMenu(Button anchor) {
        if(anchor==null)return;
        android.widget.PopupMenu menu=new android.widget.PopupMenu(this,anchor);
        // The supplied Gboard image shows a dedicated "1234" switcher;
        // this safe Candidate initially offers explicit number shortcuts.
        for(String value:new String[]{"0","1","2","3","4","5","6","7","8","9"}) {
            menu.getMenu().add(value).setOnMenuItemClickListener(item->{
                type(value);
                return true;
            });
        }
        menu.show();
    }
    private void attachShiftKey(Button key) {
        final boolean[] held={false};
        final Runnable hold=()->{
            held[0]=true;
            capsLock=true;
            shift=true;
            key.setText("⇪");
            key.setSelected(true);
            keyHaptic(key);
        };
        key.setOnClickListener(v -> {
            if(capsLock) { capsLock=false; shift=false; }
            else shift=!shift;
            refreshView();
        });
        key.setOnTouchListener((v,event)->{
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    held[0]=false;
                    key.setPressed(true);
                    animateKeyPress(key,true);
                    keyHaptic(key);
                    suggestionHandler.postDelayed(hold,450L);
                    return true;
                case MotionEvent.ACTION_UP:
                    suggestionHandler.removeCallbacks(hold);
                    key.setPressed(false);
                    animateKeyPress(key,false);
                    if(!held[0]) {
                        if(capsLock) { capsLock=false; shift=false; }
                        else shift=!shift;
                    }
                    // Rebuild after touch release, not during ACTION_DOWN.
                    refreshView();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    suggestionHandler.removeCallbacks(hold);
                    key.setPressed(false);
                    animateKeyPress(key,false);
                    return true;
                default:return true;
            }
        });
    }
    private Button addKey(LinearLayout row, String label, float weight, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        int sizeMode=KeyboardSizing.normalize(
                getSharedPreferences(KeyboardSizing.PREFS,MODE_PRIVATE)
                        .getInt(KeyboardSizing.KEY_MODE,KeyboardSizing.DEFAULT));
        b.setTextSize(label.equals("space")?15:
                sizeMode==KeyboardSizing.EXTRA_TALL?21:
                sizeMode==KeyboardSizing.TALL?20:19);
        boolean enterKey=label.equals("↵");
        boolean utility=label.equals("⇧")||label.equals("⌫")||
                label.equals("?123")||label.equals("ABC")||label.equals("🌐");
        int paint=enterKey?specialBackground:(utility?specialBackground:keyBackground);
        b.setTextColor(enterKey?AppPaletteText.contrast(paint):
                (utility?AppPaletteText.contrast(paint):keyTextColor));
        // Full-size Button owns the complete touch cell. Only its drawable
        // is inset, so visible gaps never become dead touch zones.
        b.setBackground(new InsetDrawable(feedbackBackground(paint),
                dp(3), dp(6), dp(3), dp(6)));
        b.setStateListAnimator(null);
        // Flat, softly rounded keycaps rather than elevated buttons.
        b.setElevation(0f);
        b.setContentDescription(label.equals("⌫")?"Delete":
                label.equals("⇧")?"Shift":label.equals("🌐")?"Choose keyboard":
                label.equals("↵")?"Enter":label.equals("space")?"Space":label);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(currentRowHeightDp), weight);
        // Do not use LayoutParams margins: they remove hit area.
        p.setMargins(0, 0, 0, 0);
        row.addView(b, p);
        // Execute on ACTION_DOWN, so a fast thumb press cannot be lost while
        // waiting for ACTION_UP. The touch listener consumes the event sequence;
        // onClick remains as the accessibility/programmatic fallback.
        b.setOnClickListener(v -> action.run());
        b.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.setPressed(true);
                    animateKeyPress(view,true);
                    keyHaptic(view);
                    action.run();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.setPressed(false);
                    animateKeyPress(view,false);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    return true;
                default:
                    return true;
            }
        });
        return b;
    }
    private void type(String value) {
        cancelStableWord329();
        // A new character changes the observed word; preserve neither
        // full draft text nor a stale completed prefix.
        stableWordCounted329="";
        stableWordSession329=-1L;
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        final long typeSession340=inputSessionGeneration;
        prepareCursorEdit(ic);
        if (PunctuationSpacing.insertSpaceBeforeLetter(ic.getTextBeforeCursor(1, 0),
                value, sensitive, editingInMiddle, hasSelection(), cursorHasTextAfter())) {
            if (!composing.isEmpty()) currentFinish();
            ic.commitText(" ", 1);
            // The same keystroke should use sentence-case after ! or ?.
            // Commas remain lowercase; the pure-Java boundary detector decides.
            updateSentenceShift();
        }
        if(value.matches("[A-Za-z]"))recordTypingRhythm();
        boolean shiftAtStart=shift;
        boolean usedShift=shift && value.matches("[A-Za-z]");
        if (usedShift) { value = value.toUpperCase(); if(!capsLock)shift=false; }
        final boolean apostropheInsideWord=!editingInMiddle && !sensitive &&
                !hasSelection() && !cursorHasTextAfter() &&
                EnglishApostrophePolicy.canAppendApostrophe(composing,value);
        if (PunctuationSpacing.isClosingPunctuation(value)) {
            // Suggestion taps and Space can leave a trailing separator.
            // Attach a closing mark to the preceding text without deleting
            // newlines or intentionally spaced opening symbols.
            if (!composing.isEmpty()) currentFinish();
            ic.beginBatchEdit();
            try {
                int trailing=PunctuationSpacing.spacesBeforeClosingPunctuation(
                        ic.getTextBeforeCursor(64,0),value,sensitive,hasSelection());
                if(trailing>0)ic.deleteSurroundingText(trailing,0);
                boolean addSpace=PunctuationSpacing.appendSpaceAfterPunctuation(value,sensitive,hasSelection(),cursorHasTextAfter());
                ic.commitText(addSpace ? value + " " : value,1);
            } finally {
                ic.endBatchEdit();
            }
        } else if (apostropheInsideWord) {
            // Verify the host's live result before ever retrying insertion.
            appendComposing340(ic,value);
        } else if (editingInMiddle || sensitive || !editorAllowsSuggestions ||
                symbols || !value.matches("[A-Za-z]")) {
            if (!composing.isEmpty()) currentFinish();
            ic.commitText(value, 1);
        } else {
            appendComposing340(ic,value);
        }
        if (!value.matches("[A-Za-z]")) updateSentenceShift();
        // Only an actual key entered through this IME can teach the transient
        // word sequence. Do not infer learning from pasted/editor-only text.
        if(value.matches("[A-Za-z]") &&
                editorAllowsPersonalLearning && PersonalLearning.enabled(this) &&
                !sensitive && !editingInMiddle && !hasSelection() &&
                !cursorHasTextAfter() && !numericEditor && !symbols) {
            String visible=composing.isEmpty()?wordAtCursor():composing;
            String normalized=EnglishApostrophePolicy.canonical(visible);
            CharSequence before=ic.getTextBeforeCursor(96,0);
            if(normalized!=null &&
                    StableWordUsagePolicy329.sameVisibleWord(normalized,
                            visible,before,ic==getCurrentInputConnection(),
                            typeSession340==inputSessionGeneration) &&
                    before!=null && before.toString().endsWith(value)) {
                lastTappedWord340=normalized;
                lastTappedSession340=inputSessionGeneration;
            } else {
                lastTappedWord340="";
                lastTappedSession340=-1L;
            }
        } else {
            lastTappedWord340="";
            lastTappedSession340=-1L;
            if(".".equals(value) || "!".equals(value) ||
                    "?".equals(value) || ";".equals(value))
                tappedSequence340.resetContext();
        }
        showSuggestions();
        scheduleStableWord329();
        // Only Shift changes require re-rendering key labels. In particular,
        // never detach the touched key while a symbol long-press is active.
        // ?123 and symbol-page switches refresh explicitly on user activation.
        if(!numericEditor && !capsLock && shiftAtStart!=shift &&
                !symbolHoldInProgress)
            suggestionHandler.post(this::refreshView);
    }
    private void deleteSwipeWords(int words,InputConnection ic,
            BackspaceSwipeSnapshotGuard.Ticket ticket,long downSession) {
        if(ic==null || ticket==null || sensitive)return;
        if(!BackspaceSwipePolicy.sameEditorSession(
                downSession,inputSessionGeneration,ic,getCurrentInputConnection()))return;
        // An Android editor can change selection or composing state during
        // finishComposingText. Recheck the exact preview before AND after
        // finalization, not only when the finger first touches Backspace.
        final String composingAtStart=composing;
        BackspaceSwipeEditorTransaction.Result outcome=
                BackspaceSwipeEditorTransaction.apply(
                new BackspaceSwipeEditorTransaction.Editor() {
                    @Override public boolean sameSession() {
                        return !sensitive && BackspaceSwipePolicy.sameEditorSession(
                                downSession,inputSessionGeneration,ic,
                                getCurrentInputConnection());
                    }
                    @Override public boolean selectionExpected() {
                        return hasSelection();
                    }
                    @Override public CharSequence before(int count) {
                        return ic.getTextBeforeCursor(count,0);
                    }
                    @Override public CharSequence selected() {
                        return ic.getSelectedText(0);
                    }
                    @Override public CharSequence after(int count) {
                        return ic.getTextAfterCursor(count,0);
                    }
                    @Override public void beginBatchEdit() {
                        ic.beginBatchEdit();
                    }
                    @Override public boolean finishComposition() {
                        if(!ic.finishComposingText())return false;
                        composing="";
                        return true;
                    }
                    @Override public boolean deleteBeforeCursor(int utf16Units) {
                        return ic.deleteSurroundingText(utf16Units,0);
                    }
                    @Override public boolean deleteSelection() {
                        return ic.commitText("",1);
                    }
                    @Override public void endBatchEdit() {
                        ic.endBatchEdit();
                    }
                },ticket,words,composingAtStart);
        if(outcome!=BackspaceSwipeEditorTransaction.Result.APPLIED) {
            // Fail closed: never dispatch KEYCODE_DEL or retry a different
            // range when the host editor rejected or changed the edit.
            showSuggestions();
            return;
        }
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        editingInMiddle=cursorHasTextAfter() || hasSelection();
        updateSentenceShift();
        showSuggestions();
    }
    // Suppress re-entrant Android selection callbacks during an actual
    // Backspace edit. Otherwise a transient -1 composing range may clear
    // the final composing letter and briefly show an empty suggestion hint.
    private boolean singleBackspaceMutation338;
    private void backspace() {
        clearTapSequence340();
        cancelStableWord329();
        stableWordCounted329="";
        stableWordSession329=-1L;
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        singleBackspaceMutation338=true;
        try {
            prepareCursorEdit(ic);
            if (!composing.isEmpty()) {
                if(composing.codePointCount(0,composing.length())==1) {
                    final long session=inputSessionGeneration;
                    final String letter=composing;
                    FinalComposingDelete338.Result result=
                            FinalComposingDelete338.apply(
                                new FinalComposingDelete338.Editor() {
                                    @Override public boolean sameSession() {
                                        return ic==getCurrentInputConnection() &&
                                                session==inputSessionGeneration;
                                    }
                                    @Override public CharSequence before() {
                                        return ic.getTextBeforeCursor(96,0);
                                    }
                                    @Override public boolean replaceComposingWithEmpty() {
                                        return ic.setComposingText("",1);
                                    }
                                    @Override public boolean finishComposing() {
                                        return ic.finishComposingText();
                                    }
                                    @Override public boolean deleteOneCodePoint() {
                                        return ic.deleteSurroundingTextInCodePoints(1,0)
                                                ||ic.deleteSurroundingText(1,0);
                                    }
                                },letter);
                    // Never retain a stale composing span after the host
                    // rejected or unexpectedly changed the final-letter edit.
                    composing="";
                    if(result==FinalComposingDelete338.Result.STALE)return;
                } else {
                    int previous=composing.offsetByCodePoints(
                            composing.length(),-1);
                    String shortened=composing.substring(0,previous);
                    if(ic.setComposingText(shortened,1))
                        composing=shortened;
                    else {
                        ic.finishComposingText();
                        composing=""; // next tap may use committed-text deletion
                    }
                }
            } else {
                try {
                    if(!ic.deleteSurroundingTextInCodePoints(1,0)
                            && !ic.deleteSurroundingText(1,0)) {
                        // Some custom search editors implement only the standard
                        // delete key event. Send it ONLY if both direct deletion
                        // APIs rejected the operation.
                        if(ic.sendKeyEvent(new KeyEvent(
                                KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DEL)))
                            ic.sendKeyEvent(new KeyEvent(
                                    KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DEL));
                    }
                } catch(RuntimeException editorDetached) {
                    return;
                }
            }
        } finally {
            singleBackspaceMutation338=false;
        }
        boolean oldShift=shift;
        updateSentenceShift();
        showSuggestions();
        // The previous per-repeat refresh recreated every key ~15 times/sec.
        // Preserve the live backspace touch target until release.
        if(!deleteHeld && oldShift!=shift)refreshView();
    }
    private void space() {
        cancelStableWord329();
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        // Detect a real word just typed through this IME, not a pasted or
        // existing editor string. Capture before finishComposingText(), and
        // keep only a short approved word pair after a successful Space.
        CommittedWordUsagePolicy321.Observation used=null;
        // Separate per-word frequency from adjacent-context learning. A
        // repeated standalone "continue" must be remembered even without
        // a whitelisted previous word.
        String completedWord328=null;
        long session=inputSessionGeneration;
        String tappedWord340=null;
        if(lastTappedSession340==session && !lastTappedWord340.isEmpty() &&
                editorAllowsPersonalLearning && PersonalLearning.enabled(this) &&
                !sensitive && !editingInMiddle && !hasSelection() &&
                !cursorHasTextAfter() && !numericEditor && !symbols) {
            String visible=composing.isEmpty()?wordAtCursor():composing;
            try {
                if(StableWordUsagePolicy329.sameVisibleWord(lastTappedWord340,
                        visible,ic.getTextBeforeCursor(96,0),
                        ic==getCurrentInputConnection(),true))
                    tappedWord340=lastTappedWord340;
            } catch(RuntimeException detached) { tappedWord340=null; }
        }
        if(editorAllowsPersonalLearning && PersonalLearning.enabled(this) &&
                !sensitive && !editingInMiddle && !hasSelection() &&
                !cursorHasTextAfter() && !symbols && !numericEditor)
            completedWord328=EnglishApostrophePolicy.canonical(composing);
        if(editorAllowsPersonalLearning && PersonalLearning.enabled(this) &&
                !sensitive && !composing.isEmpty()) {
            try {
                used=CommittedWordUsagePolicy321.capture(
                        ic.getTextBeforeCursor(96,0),composing,true,
                        editorAllowsPersonalLearning,sensitive,editingInMiddle,
                        hasSelection(),cursorHasTextAfter(),symbols);
            } catch(RuntimeException detached) { used=null; }
        }
        boolean alreadyCounted329=completedWord328!=null &&
                completedWord328.equals(stableWordCounted329) &&
                session==stableWordSession329;
        currentFinish();
        if(!ic.commitText(" ", 1)) {
            clearTapSequence340();
            return;
        }
        // New words can be learned as in-memory bigrams/trigrams after
        // repeated successful word boundaries, including ordinary key taps.
        // No raw message or unsaved new word is written to persistent storage.
        if(ic==getCurrentInputConnection() && session==inputSessionGeneration &&
                editorAllowsPersonalLearning && PersonalLearning.enabled(this) &&
                !sensitive && tappedWord340!=null)
            tappedSequence340.observeCommittedWord(tappedWord340,true);
        else tappedSequence340.resetContext();
        lastTappedWord340="";
        lastTappedSession340=-1L;
        if(ic==getCurrentInputConnection() && session==inputSessionGeneration &&
                !sensitive && editorAllowsPersonalLearning &&
                PersonalLearning.enabled(this)) {
            if(used!=null)
                PersonalContextLearning319.recordUsed(this,used.prior,used.word);
            if(completedWord328!=null && !alreadyCounted329) {
                PersonalWordFrequency328.recordCommitted(this,completedWord328);
                personalPrefixCache340.clear();
            }
        }
        stableWordCounted329="";
        stableWordSession329=-1L;
        // A normal space typed after repositioning must not switch back to
        // end-of-document composition when content remains after the cursor.
        editingInMiddle=cursorHasTextAfter() || hasSelection();
        // Previously every space destroyed and rebuilt the full key layout.
        // A normal word boundary does not change any key labels. Only a
        // sentence-boundary Shift change requires a deferred rebuild.
        boolean previousShift=shift;
        updateSentenceShift();
        showSuggestions();
        if(previousShift!=shift && !capsLock && !symbols)
            suggestionHandler.post(this::refreshView);
    }
    private void insertNewlineSafely() {
        InputConnection ic = getCurrentInputConnection();
        EditorInfo info = getCurrentInputEditorInfo();
        if (ic == null || info == null ||
                !EnterKeyPolicy.supportsNewline(info.inputType)) return;
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        currentFinish();
        ic.commitText("\n", 1);
        editingInMiddle=cursorHasTextAfter() || hasSelection();
        updateSentenceShift();
        showSuggestions();
        refreshView();
    }
    /**
     * A normal SEARCH/GO button performs the host-declared editor action.
     * Ordinary SEND is deliberately excluded: it retains the hold gesture.
     * Never fabricate a search based on the app package or the typed text.
     */
    private void handleEnterTap() {
        EditorInfo info=getCurrentInputEditorInfo();
        int action=info==null?0:EnterKeyPolicy.tapEditorAction(info.imeOptions);
        if(action==0) {
            insertNewlineSafely();
            return;
        }
        InputConnection editor=getCurrentInputConnection();
        if(editor==null)return;
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        try {
            // Make the completed query visible to the host before SEARCH/GO.
            currentFinish();
            boolean acted=editor.performEditorAction(action);
            if(!acted && !EnterKeyPolicy.supportsNewline(info.inputType)) {
                // Only the user explicitly tapped a Search/Go action. Some
                // custom web search editors handle physical ENTER instead
                // of performEditorAction. Do not retry on a success and do
                // not send KEYCODE_ENTER in SEND or arbitrary text fields.
                boolean down=editor.sendKeyEvent(new KeyEvent(
                        KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
                if(down)editor.sendKeyEvent(new KeyEvent(
                        KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));
            }
        } catch(RuntimeException editorDetached) {
            // Focus can switch during search submission. Never retry.
        }
        if(editor==getCurrentInputConnection()) {
            editingInMiddle=cursorHasTextAfter()||hasSelection();
            updateSentenceShift();
            showSuggestions();
            refreshView();
        }
    }

    private String enterKeyLabel() {
        EditorInfo info=getCurrentInputEditorInfo();
        return EnterKeyPolicy.keyLabel(info==null?0:info.imeOptions);
    }

    private String enterKeyDescription() {
        EditorInfo info=getCurrentInputEditorInfo();
        int action=info==null?0:EnterKeyPolicy.tapEditorAction(info.imeOptions);
        return action==EnterKeyPolicy.IME_ACTION_SEARCH?"Search":
                action==EnterKeyPolicy.IME_ACTION_GO?"Go":
                "Enter newline. Hold then release to send if supported";
    }

    private void sendIfExplicitlySupported() {
        InputConnection ic = getCurrentInputConnection();
        EditorInfo info = getCurrentInputEditorInfo();
        if (ic == null || info == null ||
                !EnterKeyPolicy.supportsLongPressSend(info.imeOptions)) return;
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        // No KEYCODE_ENTER fallback: it could send in an unexpected editor.
        // Leave the draft intact if the host rejects the SEND editor action.
        if (ic.performEditorAction(EditorInfo.IME_ACTION_SEND)) {
            currentFinish();
            editingInMiddle=cursorHasTextAfter() || hasSelection();
        }
        showSuggestions();
        refreshView();
    }
    private void attachSafeEnterKey(Button key) {
        key.setText(enterKeyLabel());
        key.setContentDescription(enterKeyDescription());
        // addKey's normal ACTION_DOWN trigger is replaced for this key.
        // Accessibility/programmatic taps mirror the user's short tap:
        // SEARCH/GO when declared, newline only for ordinary multiline text.
        key.setOnClickListener(v -> handleEnterTap());
        final float[] startX={0f},startY={0f};
        final boolean[] armed={false},cancelled={false};
        final InputConnection[] touchedEditor={null};
        final long[] touchedSession={0L};
        key.setOnTouchListener((view,event)->{
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    stopEnterHold();
                    armed[0]=false;
                    cancelled[0]=false;
                    startX[0]=event.getRawX();
                    startY[0]=event.getRawY();
                    touchedEditor[0]=getCurrentInputConnection();
                    touchedSession[0]=inputSessionGeneration;
                    enterTouchActive=true;
                    view.setPressed(true);
                    animateKeyPress(view,true);
                    keyHaptic(view);
                    EditorInfo info=getCurrentInputEditorInfo();
                    if(info!=null && EnterKeyPolicy.supportsLongPressSend(info.imeOptions)) {
                        Runnable hold=()->{
                            enterHoldPending=null;
                            if(cancelled[0] || !view.isPressed() ||
                                    !view.isAttachedToWindow() ||
                                    !sameEnterSession(touchedSession[0],touchedEditor[0]))
                                return;
                            armed[0]=true;
                            key.setText("Send");
                            key.setContentDescription("Release to send message");
                            keyHaptic(view); // separate confirmation of arming
                        };
                        enterHoldPending=hold;
                        suggestionHandler.postDelayed(hold,ENTER_SEND_HOLD_MS);
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float dx=event.getRawX()-startX[0];
                    float dy=event.getRawY()-startY[0];
                    if(!EnterKeyPolicy.gestureWithinSlop(dx,dy,dp(22))) {
                        cancelled[0]=true;
                        armed[0]=false;
                        stopEnterHold();
                        key.setText(enterKeyLabel());
                    }
                    return true;
                }
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_POINTER_UP:
                    cancelled[0]=true;
                    armed[0]=false;
                    stopEnterHold();
                    key.setText(enterKeyLabel());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    final InputConnection editor=touchedEditor[0];
                    final long session=touchedSession[0];
                    final boolean sameSession=sameEnterSession(session,editor);
                    EditorInfo info=getCurrentInputEditorInfo();
                    final boolean support=info!=null &&
                            EnterKeyPolicy.supportsLongPressSend(info.imeOptions);
                    final boolean release=event.getActionMasked()==MotionEvent.ACTION_UP;
                    // Android may coalesce the last MOVE into ACTION_UP. Check
                    // final coordinates before deciding on SEND or newline.
                    // Fail closed on NaN/out-of-bounds and large drags.
                    if(!EnterKeyPolicy.gestureWithinSlop(
                            event.getRawX()-startX[0],
                            event.getRawY()-startY[0],dp(22))) {
                        cancelled[0]=true;
                        armed[0]=false;
                    }
                    final boolean send=EnterKeyPolicy.shouldSendOnRelease(
                            release,armed[0],cancelled[0],sameSession,support);
                    final boolean newline=EnterKeyPolicy.shouldInsertNewline(
                            release,armed[0],cancelled[0],sameSession);
                    stopEnterHold();
                    enterTouchActive=false;
                    key.setText(enterKeyLabel());
                    key.setContentDescription(enterKeyDescription());
                    view.setPressed(false);
                    animateKeyPress(view,false);
                    // Build 287 showed that rebuilding a LinearLayout during touch
                    // dispatch can crash Android 17. Commit AFTER dispatch.
                    suggestionHandler.post(()->{
                        if(!view.isAttachedToWindow() ||
                                !sameEnterSession(session,editor))
                            return;
                        if(send) {
                            sendIfExplicitlySupported();
                        } else if(newline) {
                            handleEnterTap();
                        }
                    });
                    return true;
                }
                default: return true;
            }
        });
    }

    private String suggestionCase(String value) {
        // Correction chips reflect the *existing word's* casing, not the
        // keyboard Shift flag. This includes the visual preview and any
        // expanded suggestion chip. New-word entry keeps normal Shift.
        if(composing.isEmpty() && !latestSnapshot.isEmpty())
            return CursorWordCorrectionPolicy.matchCase(latestSnapshot,value);
        if(capsLock) return value.toUpperCase(java.util.Locale.US);
        if(shift && !value.isEmpty()) return Character.toUpperCase(value.charAt(0))+value.substring(1);
        return value;
    }
    // Confirmation feedback happens only after the editor accepts the text.
    // Respect the existing keyboard haptic toggle and system vibration policy.
    private void confirmSuggestionTap(View source) {
        if (source == null) return;
        // One tick only; do not double-vibrate on accepted candidates.
        if(!hapticsEnabled())return;
        if(Build.VERSION.SDK_INT>=30)
            source.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        else keyHaptic(source);
    }
    // Apply a cursor-word correction only after re-reading the live editor text.
    // Cursor movement and the centered original suggestion never change content.
    private boolean replaceCursorWord(View source, String expected, String replacement) {
        // A tap is the only operation that can accept a correction.
        // Re-read the editor, validate the entire live word, and never
        // append a space or punctuation the user didn't request.
        if(sensitive || hasSelection() || !composing.isEmpty())return false;
        InputConnection ic=getCurrentInputConnection();
        if(ic==null)return false;
        CursorWordCorrectionPolicy.Plan plan;
        try {
            plan=CursorWordCorrectionPolicy.plan(
                    ic.getTextBeforeCursor(64,0),ic.getTextAfterCursor(64,0),
                    expected,replacement,hasSelection(),sensitive);
        } catch(RuntimeException detachedEditor) {
            return false;
        }
        if(plan==null)return false;
        // Derive only a safe, common previous word *before* the live edit.
        // Never persist an editor snapshot, full message or surrounding text.
        String priorWord=null;
        if(editorAllowsPersonalLearning && !cursorHasTextAfter()) {
            try {
                priorWord=AcceptedContextPolicy319.precedingForReplacement(
                        ic.getTextBeforeCursor(96,0),expected);
            } catch(RuntimeException detachedEditor) {priorWord=null;}
        }
        invalidateEditingTicket();
        boolean done=false;
        try {
            ic.beginBatchEdit();
            try {
                if(ic.deleteSurroundingText(plan.lettersBefore,plan.lettersAfter))
                    done=ic.commitText(plan.replacement,1);
            } finally {
                ic.endBatchEdit();
            }
        } catch(RuntimeException detachedEditor) {
            // Do not retry or apply a second deletion after editor detach.
            return false;
        }
        if(done) {
            editingInMiddle=cursorHasTextAfter();
            confirmSuggestionTap(source);
            if(editorAllowsPersonalLearning) {
                PersonalLearning.recordAcceptance(this,expected,replacement);
                PersonalWordFrequency328.recordTapped(this,replacement);
                personalPrefixCache340.clear();
                PersonalContextLearning319.record(this,priorWord,replacement);
            }
            updateSentenceShift();
            showSuggestions();
            refreshView();
        }
        return done;
    }
    private boolean acceptSplitNextWord(View source,String expected,String next) {
        if(sensitive || expected.isEmpty() || !expected.equals(composing)
                || !splitPredictionsEnabled() || !splitNextWords.contains(next))return false;
        InputConnection ic=getCurrentInputConnection();
        if(ic==null)return false;
        // A split next-word chip is ALSO an explicit acceptance.
        // Candidate 319 missed this path, so users' most natural taps
        // were not contributing to contextual learning. Capture only
        // a closed-list safe word, not any surrounding sentence text.
        final String acceptedPrior=SplitNextWordContextPolicy320.safePrevious(
                expected,editorAllowsPersonalLearning &&
                        PersonalLearning.enabled(this),sensitive,
                editingInMiddle,hasSelection(),cursorHasTextAfter());
        final long sourceSession=inputSessionGeneration;
        // Capture safety BEFORE commit. Editor callbacks can update cursor
        // flags synchronously during commitText(), even when this tap was
        // genuinely safe. Never infer the pre-commit state afterward.
        final boolean eligibleAtTap=editorAllowsPersonalLearning &&
                PersonalLearning.enabled(this) && !sensitive &&
                !editingInMiddle && !hasSelection() && !cursorHasTextAfter() &&
                !numericEditor && !symbols;
        boolean committed=false;
        ic.beginBatchEdit();
        try {
            ic.finishComposingText();
            committed=ic.commitText(" "+next+" ",1);
        } finally {ic.endBatchEdit();}
        if(committed) {
            composing="";
            // Editor can switch while a split chip completes; never learn
            // from a stale input session or a rejected host commit.
            if(eligibleAtTap && sourceSession==inputSessionGeneration &&
                    ic==getCurrentInputConnection() &&
                    editorAllowsPersonalLearning && !sensitive &&
                    PersonalLearning.enabled(this)) {
                if(acceptedPrior!=null)
                    PersonalContextLearning319.record(this,acceptedPrior,next);
                // Split next-word chips are explicitly chosen words too.
                // Count only after a successful commit to the same editor.
                PersonalWordFrequency328.recordTapped(this,next);
                personalPrefixCache340.clear();
            }
            confirmSuggestionTap(source);
            updateSentenceShift();
            showSuggestions();
            refreshView();
        }
        return committed;
    }
    private void acceptSuggestion(View source, String expected, String suggestion) {
        cancelStableWord329();
        stableWordCounted329="";
        stableWordSession329=-1L;
        cancelCursorReleaseSuggestions();
        if(splitNextWords.contains(suggestion) && !suggestion.equalsIgnoreCase(expected)
                && acceptSplitNextWord(source,expected,suggestion))return;
        if (sensitive) return;
        if(composing.isEmpty() && !expected.isEmpty()) {
            // A reused word at another cursor location is not the same chip.
            // Verify the ticket before the existing live whole-word edit plan.
            if(!isEditingTicketCurrent(visibleEditingTicket)) {
                predictionDispatcher.cancel();
                showSuggestions();
                return;
            }
            // Use the tap's captured expected word, then verify it again
            // against live editor text. Do not inherit stale auto-Shift
            // or trust the display strip's changing snapshot.
            replaceCursorWord(source,expected,
                    CursorWordCorrectionPolicy.matchCase(expected,suggestion));
            return;
        }
        suggestion=suggestionCase(suggestion);
        if (!expected.equals(composing)) return;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        // Empty-bar choices are NEXT-WORD insertions, not corrections to a
        // composing span. Never call setComposingText with an empty expected span.
        if (expected.isEmpty()) {
            String prior=editorAllowsPersonalLearning &&
                    !editingInMiddle && !hasSelection() && !cursorHasTextAfter()
                    ?AcceptedContextPolicy319.precedingForNextWord(
                            ic.getTextBeforeCursor(96,0)):null;
            if (!ic.commitText(suggestion + " ", 1)) return;
            if(editorAllowsPersonalLearning) {
                PersonalContextLearning319.record(this,prior,suggestion);
                PersonalWordFrequency328.recordTapped(this,suggestion);
                personalPrefixCache340.clear();
            }
            confirmSuggestionTap(source);
            if(shift && !capsLock) shift=false;
            // A next-word tap commits a trailing boundary at the cursor.
            // Selection callbacks from the previous composing span may have
            // marked this as mid-word editing. Re-evaluate after the commit.
            editingInMiddle=cursorHasTextAfter() || !wordAtCursor().isEmpty();
            updateSentenceShift();
            showSuggestions();
            // Some editors publish their final selection asynchronously.
            // Refresh only if still at a safe word boundary.
            suggestionHandler.post(() -> {
                if(!sensitive && composing.isEmpty() && !hasSelection()
                        && !cursorHasTextAfter() && wordAtCursor().isEmpty()) {
                    editingInMiddle=false;
                    showSuggestions();
                }
            });
            refreshView();
            return;
        }
        if (hasSelection()) return;
        final String priorWord=editorAllowsPersonalLearning &&
                !editingInMiddle && !cursorHasTextAfter()
                ?AcceptedContextPolicy319.precedingForReplacement(
                        ic.getTextBeforeCursor(96,0),expected):null;
        // Commit the entire chosen sentence in one InputConnection operation.
        // A separate setComposingText + space commit risked partial insertion.
        SentenceSuggestionCommit.Result outcome=SentenceSuggestionCommit.insert(
                new SentenceSuggestionCommit.Editor() {
                    @Override public CharSequence before(int count) {
                        return ic.getTextBeforeCursor(count,0);
                    }
                    @Override public CharSequence after(int count) {
                        return ic.getTextAfterCursor(count,0);
                    }
                    @Override public void beginBatch() { ic.beginBatchEdit(); }
                    @Override public boolean commit(String text) {
                        return ic.commitText(text,1);
                    }
                    @Override public void endBatch() { ic.endBatchEdit(); }
                },expected,suggestion);
        if(outcome==SentenceSuggestionCommit.Result.STALE) {
            android.widget.Toast.makeText(this, "Suggestion skipped: text no longer matches",
                    android.widget.Toast.LENGTH_SHORT).show();
            currentFinish();
            showSuggestions();
            return;
        }
        if(outcome==SentenceSuggestionCommit.Result.APPLIED) {
            composing="";
            if(shift && !capsLock)shift=false;
            confirmSuggestionTap(source);
            if(editorAllowsPersonalLearning) {
                PersonalLearning.recordAcceptance(this,expected,suggestion);
                // A chosen sentence contains useful completed words too.
                // V3 stores only bounded per-word counters, never the phrase.
                PersonalWordFrequency328.recordAcceptedPhrase(this,suggestion);
                personalPrefixCache340.clear();
                // Explicit selection can teach adjacent public-word pairs.
                // Only closed-list left context plus accepted words persist.
                PersonalContextLearning319.record(this,priorWord,suggestion);
                PersonalContextLearning319.recordChosenPhrase(this,suggestion);
            }
            predictionsExpanded=false;
            updateSentenceShift();
            showSuggestions();
            refreshView();
        } else {
            // Do not delete/retry after a rejected commit: remote editor state
            // may already have changed, and the user's original must be safe.
            android.widget.Toast.makeText(this, "Could not apply suggestion",
                    android.widget.Toast.LENGTH_SHORT).show();
            showSuggestions();
        }
    }
    private String precedingContext() {
        if(sensitive || editingInMiddle || cursorHasTextAfter() || hasSelection())return "";
        InputConnection ic=getCurrentInputConnection();
        if(ic==null)return "";
        CharSequence before=ic.getTextBeforeCursor(160,0);
        return before==null?"":before.toString();
    }
    private String precedingWord() {
        InputConnection ic=getCurrentInputConnection();
        if(ic==null)return "";
        CharSequence visible=ic.getTextBeforeCursor(72,0);
        if(visible==null)return "";
        // Shared pure-Java policy is independently tested for repeated spaces.
        return NextWordCandidatePolicy.precedingWord(visible);
    }
    // Read the whole word straddling the insertion point without moving the cursor.
    private String wordAtCursor() {
        if(hasSelection())return "";
        InputConnection ic=getCurrentInputConnection();
        if(ic==null)return "";
        CharSequence before=ic.getTextBeforeCursor(64,0);
        CharSequence after=ic.getTextAfterCursor(64,0);
        if(before==null||after==null)return "";
        int left=before.length(),right=0;
        while(left>0 && EnglishApostrophePolicy.isTokenChar(before.charAt(left-1)))left--;
        while(right<after.length() &&
                EnglishApostrophePolicy.isTokenChar(after.charAt(right)))right++;
        if(left==before.length() && right==0)return "";
        return before.subSequence(left,before.length()).toString()+after.subSequence(0,right).toString();
    }
    private void showSuggestions() {
        if(candidates==null)return;
        final String snapshot=composing;
        if(sensitive || !editorAllowsSuggestions) {
            predictionDispatcher.cancel();
            renderSuggestions(snapshot,java.util.Collections.emptyList());
            return;
        }
        if(snapshot.isEmpty()) {
            predictionDispatcher.cancel();
            // Editors that commit every character (Search/Keep and others)
            // have no composing span. Offer ordinary English spelling
            // corrections as well as contractions, always as explicit taps.
            // The same immutable caret ticket protects the live word; do not
            // replace text using an old suggestion after typing or navigating.
            if(!editingInMiddle && !hasSelection() && !cursorHasTextAfter()) {
                final String cursorWord=wordAtCursor();
                if(CommittedEnglishCursorPolicy.eligible(
                        cursorWord,false,editingInMiddle,false,false,
                        sensitive,editorAllowsSuggestions)) {
                    final CursorEditSnapshotGuard.Ticket ticket=
                            captureEditingTicket(cursorWord);
                    if(ticket!=null) {
                        java.util.List<String> initial=
                                CommittedEnglishCursorPolicy.immediate(cursorWord);
                        if(editorAllowsPersonalLearning &&
                                PersonalLearning.enabled(this))
                            initial=AdaptivePrefixPolicy328.merge(cursorWord,
                                    initial,personalizedPrefix340(cursorWord),
                                    PredictionLite.publicWordForContext(
                                            cursorWord.toLowerCase(java.util.Locale.ROOT)));
                        renderSuggestions(cursorWord,initial);
                        visibleEditingTicket=ticket;
                        final long startedAt=CandidateLatencyMetrics.start(getPackageName());
                        predictionDispatcher.scheduleEditing(cursorWord,(id,input,options)->{
                            final long computedAt=startedAt==0L?0L:System.nanoTime();
                            suggestionHandler.post(()->{
                                if(!predictionDispatcher.isCurrent(id) ||
                                        candidates==null || sensitive ||
                                        !editorAllowsSuggestions || !composing.isEmpty() ||
                                        !input.equals(wordAtCursor()) ||
                                        !isEditingTicketCurrent(ticket))return;
                                java.util.List<String> merged=
                                        CommittedEnglishCursorPolicy.merge(input,options);
                                if(editorAllowsPersonalLearning &&
                                        PersonalLearning.enabled(this))
                                    merged=AdaptivePrefixPolicy328.merge(
                                            input,merged,
                                            personalizedPrefix340(input),
                                            PredictionLite.publicWordForContext(
                                                    input.toLowerCase(java.util.Locale.ROOT)));
                                renderSuggestions(input,merged);
                                visibleEditingTicket=ticket;
                                if(CandidateLatencyMetrics.recordReady(
                                        startedAt,computedAt,System.nanoTime()))
                                    recordNextCandidateFrame(id,startedAt);
                            });
                        });
                        return;
                    }
                }
            }
            if(editingInMiddle || cursorHasTextAfter() || hasSelection()) {
                final String word=wordAtCursor();
                if(word.isEmpty()) {
                    renderSuggestions("",java.util.Collections.emptyList());
                    return;
                }
                // Cursor-word options use a live range-checked replacement on tap.
                // Show a safe exact-word/contraction alternative immediately,
                // even if the debounced fuzzy predictor has not returned yet.
                final CursorEditSnapshotGuard.Ticket editTicket=captureEditingTicket(word);
                java.util.List<String> immediately=
                        CursorWordCorrectionPolicy.safeOptions(word,
                                EnglishApostrophePolicy.suggestions(word));
                renderSuggestions(word,immediately);
                if(editTicket==null)return; // No trustworthy editor snapshot.
                visibleEditingTicket=editTicket;
                final long startedAt=CandidateLatencyMetrics.start(getPackageName());
                predictionDispatcher.scheduleEditing(word,(id,input,options)->{
                    final long computedAt=startedAt==0L?0L:System.nanoTime();
                    suggestionHandler.post(()->{
                        if(!predictionDispatcher.isCurrent(id)||sensitive||!composing.isEmpty()
                                ||!input.equals(wordAtCursor())
                                ||!isEditingTicketCurrent(editTicket))return;
                        java.util.List<String> wordOptions=
                                CursorWordCorrectionPolicy.safeOptions(input,
                                        EnglishApostrophePolicy.merge(input,options));
                        // Read only bounded, live current-editor context for
                        // ranking already proposed words. Strong evidence on
                        // BOTH sides is required; otherwise keep the same row.
                        // The typed context is never stored or logged.
                        try {
                            InputConnection connection=getCurrentInputConnection();
                            if(connection!=null)wordOptions=
                                    CursorContextCorrectionRanker.rerank(
                                            input,wordOptions,
                                            connection.getTextBeforeCursor(128,0),
                                            connection.getTextAfterCursor(128,0),
                                            PredictionLite::editingTransitionSupported);
                        } catch(RuntimeException editorDetached) {
                            // Keep original spelling suggestions if an editor
                            // declines the optional contextual snapshot.
                        }
                        // The view and the click use the same captured editor.
                        renderSuggestions(input,wordOptions);
                        visibleEditingTicket=editTicket;
                        if(CandidateLatencyMetrics.recordReady(
                                startedAt,computedAt,System.nanoTime()))
                            recordNextCandidateFrame(id,startedAt);
                    });
                });
                return;
            }
            String context=precedingContext();
            List<String> legacy=PredictionLite.contextualNextWords(context);
            // Learned context must remain visible even when the optional
            // offline seed/phrase models fill all three candidate slots.
            // Only two or more explicitly accepted, safe local associations
            // are promoted; never auto-insert a next word.
            List<String> learned=editorAllowsPersonalLearning &&
                    PersonalLearning.enabled(this) &&
                    !editingInMiddle && !hasSelection() && !cursorHasTextAfter()
                    ?PersonalContextLearning319.suggest(this,precedingWord(),3)
                    :java.util.Collections.emptyList();
            List<String> transient340=editorAllowsPersonalLearning &&
                    PersonalLearning.enabled(this) && !numericEditor &&
                    !editingInMiddle && !hasSelection() && !cursorHasTextAfter()
                    ?tappedSequence340.suggest(context,3)
                    :java.util.Collections.emptyList();
            List<String> contextual340=NextWordCandidatePolicy.merge(
                    transient340,learned,3);
            if(NextWordCandidatePolicy.eligible(offlineNextWordEnabled(),sensitive,
                    editingInMiddle,hasSelection(),cursorHasTextAfter(),precedingWord())) {
                NextWordSeedModel model=offlineNextWordSeed();
                List<String> seed=model==null ? java.util.Collections.emptyList() :
                        model.suggest(context,5);
                List<String> phrase=NextWordPhraseModel.suggest(context,3);
                List<String> established=NextWordCandidatePolicy.mergeWithPhrase(
                        phrase,seed,legacy,5);
                renderSuggestions("",NextWordCandidatePolicy.merge(
                        contextual340,established,3));
            } else renderSuggestions("",NextWordCandidatePolicy.merge(
                    contextual340,legacy,3));
            return;
        }
        // Instant, tap-only prefix predictions from completed-word habits,
        // before the asynchronous full-sentence decoder finishes. Do not
        // replace real short words such as "can", "my" or "in".
        final boolean prefixLearning328=editorAllowsPersonalLearning &&
                PersonalLearning.enabled(this) && !editingInMiddle &&
                !hasSelection() && !cursorHasTextAfter() && !symbols &&
                !numericEditor;
        final java.util.List<String> personalized328=prefixLearning328 ?
                personalizedPrefix340(snapshot):
                java.util.Collections.emptyList();
        if(!personalized328.isEmpty())
            renderSuggestions(snapshot,AdaptivePrefixPolicy328.merge(
                    snapshot,java.util.Collections.emptyList(),
                    personalized328,PredictionLite.publicWordForContext(
                            snapshot.toLowerCase(java.util.Locale.ROOT))));
        // Narrow fast lane: show the typed word and exact, tap-only
        // apostrophe choices at once (including were/we're and hows/how's).
        // Full SentenceFlow still runs off-thread and replaces these options
        // only if its result belongs to the same composing snapshot.
        if(EnglishApostrophePolicy.hasDistinctSuggestion(snapshot)) {
            java.util.ArrayList<String> immediate=new java.util.ArrayList<>();
            immediate.add(snapshot);
            immediate.addAll(EnglishApostrophePolicy.suggestions(snapshot));
            renderSuggestions(snapshot,AdaptivePrefixPolicy328.merge(
                    snapshot,immediate,personalized328,true));
        }
        // The background worker does all expensive sentence reconstruction.
        // Do not dim visible predictions while an asynchronous update runs.
        // The old alpha toggle visibly flashed during fast typing/backspace.
        final long startedAt=CandidateLatencyMetrics.start(getPackageName());
        predictionDispatcher.schedule(snapshot,(id,input,options)->{
            final long computedAt=startedAt==0L?0L:System.nanoTime();
            suggestionHandler.post(()->{
                if(!predictionDispatcher.isCurrent(id)||candidates==null||
                        sensitive||!input.equals(composing))return;
                // A manually saved personal word is considered only for the
                // matching prefix, never from ambient typing or editor history.
                // This runs on the IME UI callback, reading at most 250 entries.
                // A contraction with straight/curly apostrophe stays intact;
                // missing-mark options are tap-only, never silent autocorrect.
                java.util.List<String> withContractions=
                        EnglishApostrophePolicy.merge(input,options);
                java.util.List<String> withPersonal=editorAllowsPersonalLearning
                        ?PersonalDictionaryPolicy.merge(input,withContractions,
                                PersonalDictionary.matching(this,input)):withContractions;
                // Existing opt-in Learning v1 remains independent: only a
                // twice-accepted explicit single-word correction may outrank.
                String preferred=editorAllowsPersonalLearning ?
                        PersonalLearning.preferredCorrection(this,input):null;
                java.util.List<String> ranked=withPersonal;
                if(preferred!=null) {
                    java.util.ArrayList<String> chosen=new java.util.ArrayList<>(withPersonal);
                    chosen.remove(preferred);
                    chosen.add(0,preferred);
                    if(chosen.size()>8)chosen.remove(chosen.size()-1);
                    ranked=chosen;
                }
                // Stable, evidence-ranked personal completions retain their
                // place even after the slower prediction batch arrives.
                if(prefixLearning328)
                    ranked=AdaptivePrefixPolicy328.merge(input,ranked,
                            personalizedPrefix340(input),
                            PredictionLite.publicWordForContext(
                                    input.toLowerCase(java.util.Locale.ROOT)));
                renderSuggestions(input,ranked);
                if(CandidateLatencyMetrics.recordReady(
                        startedAt,computedAt,System.nanoTime()))
                    recordNextCandidateFrame(id,startedAt);
            });
        });
    }
    /**
     * The callback runs at the beginning of the next animation time step,
     * not after pixels are presented. Cancelled generations are ignored.
     */
    private void recordNextCandidateFrame(long id,long startedAt) {
        if(candidates==null || !candidates.isAttachedToWindow())return;
        final LinearLayout row=candidates;
        row.postOnAnimation(()->{
            if(row==candidates && row.isAttachedToWindow()
                    &&predictionDispatcher.isCurrent(id) && !sensitive)
                CandidateLatencyMetrics.recordFrame(startedAt,System.nanoTime());
        });
    }
    private void renderSuggestions(String input,List<String> options) {
        visibleEditingTicket=null;
        if(candidates==null)return;
        if(manuallyCollapsedPredictions && !input.equals(manualCollapseSnapshot))
            manuallyCollapsedPredictions=false;
        latestSnapshot=input;
        boolean splitActive=splitPredictionsEnabled() && !sensitive &&
                editorAllowsSuggestions && !input.isEmpty()
                && !editingInMiddle && !cursorHasTextAfter();
        if(splitActive) {
            if(!input.equals(splitSnapshot340)) {
                splitSnapshot340=input;
                splitPhrasesCache340=PredictionLite.splitPhraseSuggestions(input);
                splitNextCache340=PredictionLite.splitNextWords(input);
            }
            splitPhrases=splitPhrasesCache340;
            splitNextWords=splitNextCache340;
        } else {
            splitSnapshot340=null;
            splitPhrases=java.util.Collections.emptyList();
            splitNextWords=java.util.Collections.emptyList();
        }
        if(splitActive && (!splitNextWords.isEmpty() || !splitPhrases.isEmpty())) {
            java.util.ArrayList<String> singles=new java.util.ArrayList<>();
            for(String option:options) if(option.indexOf(' ')<0)singles.add(option);
            // For a correctly spelled word, offer useful next words rather
            // than filling the strip with near-miss spelling corrections.
            if(!splitNextWords.isEmpty()) {
                singles.clear();
                // Split mode used to replace the entire suggestion strip
                // with generic right-side words, hiding learned "con ->
                // continue" completions. Keep personalized prefix choices
                // visible without discarding split next-word controls.
                if(editorAllowsPersonalLearning &&
                        PersonalLearning.enabled(this) && !numericEditor &&
                        !editingInMiddle && !hasSelection() &&
                        !cursorHasTextAfter()) {
                    String raw=input.toLowerCase(java.util.Locale.ROOT);
                    if(PredictionLite.publicWordForContext(raw))
                        singles.add(input);
                    singles.addAll(personalizedPrefix340(input));
                }
                for(String next:splitNextWords)
                    if(!singles.contains(next))singles.add(next);
            }
            // If a typo is being reconstructed into multiple words, keep
            // sentence candidates rather than hiding all useful corrections.
            if(!singles.isEmpty())options=singles;
        }
        // Display-only condensation retains the highest-ranked prediction.
        // Do not change the underlying predictor scores or accepted text.
        options=SuggestionDisplayPolicy.compact(input,options);
        // Build 304: a few idle next-word chips should sit in the CENTER of
        // the available strip, not cling to its left edge. Restore START
        // while composing/editing and for private-field status messages.
        // The HorizontalScrollView already has fillViewport=true; its child
        // can therefore center a short row without moving the right caret.
        final boolean centerIdle=SuggestionStripLayoutPolicy.centerIdleSuggestions(
                input,options.size(),sensitive);
        final int stripGravity=Gravity.CENTER_VERTICAL |
                (centerIdle?Gravity.CENTER_HORIZONTAL:Gravity.START);
        if(candidates.getGravity()!=stripGravity)
            candidates.setGravity(stripGravity);
        latestOptions=new java.util.ArrayList<>(options);
        suggestionHandler.removeCallbacks(delayedRiseDismiss);
        // Auto-expand only when there are several real choices; avoid a jump
        // on every single character or in secure fields.
        if(input.isEmpty()) {
            if(!freezeRiseDuringDelete) {
                if(predictionsExpanded)suggestionHandler.postDelayed(delayedRiseDismiss,riseDurationMs());
                else predictionsExpanded=false;
            }
            manuallyCollapsedPredictions=false;
        } else if(!freezeRiseDuringDelete && !sensitive && !manuallyCollapsedPredictions
                && input.length()>=2 && (!splitPhrases.isEmpty() || options.size()>3)) {
            predictionsExpanded=true;
        } else if(!freezeRiseDuringDelete && (options.isEmpty() || (splitPhrases.isEmpty() && options.size()<=3))
                && !manuallyCollapsedPredictions) {
            if(predictionsExpanded)suggestionHandler.postDelayed(delayedRiseDismiss,riseDurationMs());
            else predictionsExpanded=false;
        }
        updatePredictionPanel();
        candidates.setAlpha(1f);
        if(suggestionScroll!=null && suggestionScroll.getScrollX()!=0)
            suggestionScroll.scrollTo(0,0);
        if(options.isEmpty() && !input.isEmpty() && !sensitive &&
                editorAllowsSuggestions) {
            options=java.util.Collections.singletonList(input);
        }
        if(options.isEmpty()) {
            candidates.removeAllViews();
            candidates.setTag("message");
            TextView message=new TextView(this);
            message.setText(sensitive?"Private field • predictions off":
                    !editorAllowsSuggestions?"Suggestions off for this field":
                    NextWordControlLabels.emptyBarText(offlineNextWordEnabled()));
            message.setTextColor(secondaryTextColor);
            message.setTextSize(12);
            message.setPadding(dp(12),0,0,0);
            candidates.addView(message);
            return;
        }
        // For long sentences the best correction is the FIRST, almost
        // screen-wide chip. Short-word predictions keep their familiar
        // centered layout. In every case the original input remains tappable.
        // Nothing is accepted or changes the editor without an explicit tap.
        if(!input.isEmpty() && !sensitive)
            options=SuggestionDisplayPolicy.arrange(
                    input,options,splitActive && !splitNextWords.isEmpty());
        // Smart Rise 3.0: the primary candidate owns a fixed-width strip cell.
        // Other long alternatives belong in the two-card expanded row.
        // Long sentence corrections need a readable, two-line primary chip.
        // Short words retain the existing compact three-column presentation.
        int screenWidth=getResources().getDisplayMetrics().widthPixels;
        boolean longPhrase=false;
        for(String candidate:options)if(SuggestionDisplayPolicy.longSentence(input,candidate)) {
            longPhrase=true;break;
        }
        int maxCandidateWidth=Math.max(dp(160),screenWidth-dp(70));
        // Use prediction evidence, not raw character count: a long single word
        // must never trigger the full-width sentence layout.
        boolean stableSentenceStrip=!input.isEmpty() && !options.isEmpty()
                && SuggestionDisplayPolicy.confidentMultiword(input,options.get(0));
        final boolean primarySentence=!options.isEmpty()
                && SuggestionDisplayPolicy.longSentence(input,options.get(0));
        final int emphasizedIndex=primarySentence?0:1;
        final int shownCount=SuggestionStripLayoutPolicy.shownCount(
                options.size(),stableSentenceStrip);
        final boolean emptyInput=input.isEmpty();
        final String cellSignature="chips:"+shownCount+":"+emptyInput;
        final int expectedChildren=SuggestionStripLayoutPolicy.childCount(
                shownCount,emptyInput);
        boolean reuseCells=cellSignature.equals(candidates.getTag()) &&
                candidates.getChildCount()==expectedChildren;
        if(!reuseCells) {
            candidates.removeAllViews();
            candidates.setTag(cellSignature);
        }
        for (int i=0;i<shownCount;i++) {
            String suggestion=options.get(i);
            int childIndex=SuggestionStripLayoutPolicy.chipChildIndex(i,emptyInput);
            TextView option=reuseCells?(TextView)candidates.getChildAt(childIndex):new TextView(this);
            String shownSuggestion=suggestionCase(suggestion);
            if(!TextUtils.equals(option.getText(),shownSuggestion))
                option.setText(shownSuggestion);
            boolean fullSentenceChip=longPhrase && suggestion.indexOf(' ')>=0;
            option.setTextSize(SuggestionVisualStyle.textSizeSp(fullSentenceChip,
                    suggestion.codePointCount(0,suggestion.length())));
            option.setLetterSpacing(SuggestionVisualStyle.letterSpacing(fullSentenceChip));
            option.setIncludeFontPadding(false);
            option.setSingleLine(!fullSentenceChip);
            option.setMaxLines(fullSentenceChip?2:1);
            option.setEllipsize(TextUtils.TruncateAt.END);
            option.setMaxWidth(maxCandidateWidth);
            option.setGravity(Gravity.CENTER);
            option.setPadding(dp(13),0,dp(13),0);
            option.setContentDescription("Insert suggestion "+suggestionCase(suggestion));
            // Long press previews the complete candidate without accepting
            // or changing the editor. Toast uses only the current local text.
            if(fullSentenceChip)option.setOnLongClickListener(v->{
                android.widget.Toast.makeText(this,suggestionCase(suggestion),
                        android.widget.Toast.LENGTH_LONG).show();
                return true;
            });
            else option.setOnLongClickListener(null);
            // Emphasize the best displayed correction, without indicating
            // verified accuracy or changing the prediction rank/accepted text.
            // Never mark unchanged typed text as a confident correction.
            final boolean emphasize=SuggestionVisualStyle.highlight(
                    input,suggestion,i,shownCount==1?0:emphasizedIndex);
            int tileColor=SuggestionVisualStyle.chipColor(
                    suggestionBackground,keyBackground,themeDark,emphasize);
            option.setTextColor(SuggestionVisualStyle.readableText(
                    suggestionTextColor,tileColor));
            if(!(option.getTag() instanceof Integer) ||
                    ((Integer)option.getTag())!=tileColor) {
                option.setBackground(feedbackBackground(tileColor));
                option.setTag(tileColor);
            }
            option.setTypeface(android.graphics.Typeface.create(
                    emphasize?"sans-serif-medium":"sans-serif",
                    android.graphics.Typeface.NORMAL));
            // Give long phrases almost the whole scrollable strip. A 1/3-
            // width chip hid most of the correction on the Galaxy S26 Ultra.
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(
                    stableSentenceStrip?maxCandidateWidth:
                    fullSentenceChip?maxCandidateWidth:
                    (!input.isEmpty() && i<3 ? Math.max(dp(70),(screenWidth-dp(30))/3) : -2),
                    fullSentenceChip?dp(48):dp(44));
            params.setMargins(dp(2),dp(2),dp(2),dp(2));
            if(reuseCells) {
                LinearLayout.LayoutParams previous=
                        (LinearLayout.LayoutParams)option.getLayoutParams();
                if(previous.width!=params.width || previous.height!=params.height)
                    option.setLayoutParams(params);
            } else candidates.addView(option,params);
            final String expected=input;
            option.setOnClickListener(v->acceptSuggestion(v,expected,suggestion));
            if(!reuseCells && SuggestionStripLayoutPolicy.dividerAfter(
                    i,shownCount,emptyInput)) {
                View separator=new View(this);
                separator.setBackgroundColor(borderColor);
                LinearLayout.LayoutParams divider=new LinearLayout.LayoutParams(dp(1),dp(26));
                divider.setMargins(dp(3),0,dp(3),0);
                candidates.addView(separator,divider);
            }
        }
    }
}
