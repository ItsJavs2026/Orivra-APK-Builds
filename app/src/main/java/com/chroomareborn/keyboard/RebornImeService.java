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
    private LinearLayout base, keys, candidates, paletteBar;
    private HorizontalScrollView suggestionScroll;
    private TextView paletteLabel;
    private Button paletteButton, sizeButton, toolbarToggle, predictionsToggle, animationSpeedButton, splitModeButton, nextWordButton;
    private LinearLayout predictionHeader, expandedSuggestions;
    private boolean predictionsExpanded=false;
    private boolean freezeRiseDuringDelete = false;
    // Smart Rise 2.0: retain the panel briefly through transient candidate gaps.
    private final Runnable delayedRiseDismiss=()->{
        if(!predictionsExpanded || freezeRiseDuringDelete)return;
        predictionsExpanded=false;
        updatePredictionPanel();
    };
    private static final long RISE_DISMISS_GRACE_MS=420L;
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
    private String composing = "";
    private boolean sensitive, shift, symbols, capsLock;
    private boolean extraSymbols=false;

    private boolean numericEditor=false;
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
    private final Runnable deleteRepeat = new Runnable() {
        @Override public void run() {
            if (!deleteHeld || getCurrentInputConnection() == null) {
                stopDeleteRepeat();
                return;
            }
            backspace();
            deleteHandler.postDelayed(this, DELETE_REPEAT_MS);
        }
    };
    private void stopDeleteRepeat() {
        deleteHeld = false;
        deleteHandler.removeCallbacks(deleteRepeat);
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
        if(sensitive) {
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
        if(sensitive) {
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

    // Visual feedback only: the Button's layout bounds and hit target never move.
    // Cancel old animations so rapid alternating thumb taps do not queue effects.
    private void animateKeyPress(View view, boolean pressed) {
        view.animate().cancel();
        // Gboard-inspired restrained feedback: no overshoot or bouncing.
        // Layout bounds remain unchanged even during rapid multi-touch.
        if (pressed) {
            view.animate().scaleX(.97f).scaleY(.97f)
                    .translationZ(0f).setDuration(45L).start();
        } else {
            view.animate().scaleX(1f).scaleY(1f)
                    .translationZ(0f).setDuration(65L).start();
        }
    }
    private void attachRepeatingBackspace(Button key) {
        // Accessibility actions still support a normal one-character click.
        key.setOnClickListener(v -> backspace());
        key.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    stopDeleteRepeat();
                    deleteHeld = true;
                    key.setPressed(true);
                    animateKeyPress(key,true);
                    keyHaptic(key);
                    freezeRiseDuringDelete = true;
                    backspace();
                    deleteHandler.postDelayed(deleteRepeat, DELETE_HOLD_DELAY_MS);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    // Moving the finger away cancels repeat; it must not delete
                    // from a different field after an interrupted gesture.
                    if (event.getX() < -key.getWidth() / 2f
                            || event.getX() > key.getWidth() * 1.5f
                            || event.getY() < -key.getHeight() / 2f
                            || event.getY() > key.getHeight() * 1.5f) {
                        stopDeleteRepeat();
                        freezeRiseDuringDelete = false;
                        showSuggestions();
                        key.setPressed(false);
                        animateKeyPress(key,false);
                        refreshView();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopDeleteRepeat();
                    freezeRiseDuringDelete = false;
                    showSuggestions();
                    key.setPressed(false);
                    animateKeyPress(key,false);
                    // One rebuild only after lift; never detach the pressed
                    // backspace key during a fast held-delete sequence.
                    refreshView();
                    return true;
                default:
                    return true;
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
    private void setColors(EditorInfo editor) {
        AppPalette.Result result=AppPalette.resolve(this,editor==null?null:editor.packageName);
        background=result.background;
        keyBackground=result.keyBackground;
        accent=result.accent;
        themeSource=result.label;
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
        // Suggestions live immediately below the compact toolbar.
        // The toolbar itself is inserted before this scroll view below.
        paletteBar = new LinearLayout(this);
        paletteBar.setGravity(Gravity.CENTER_VERTICAL);
        paletteBar.setMotionEventSplittingEnabled(true);
        paletteLabel = new TextView(this);
        paletteLabel.setTextColor(secondaryTextColor);
        paletteLabel.setSingleLine(true);
        paletteLabel.setTextSize(10);
        paletteLabel.setPadding(dp(8), 0, dp(5), 0);
        paletteBar.addView(paletteLabel, new LinearLayout.LayoutParams(0, dp(32), 1f));
        paletteButton = new Button(this);
        paletteButton.setText("🎨");
        paletteButton.setTextSize(16);
        paletteButton.setAllCaps(false);
        paletteButton.setTextColor(keyTextColor);
        paletteButton.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams paletteParams = new LinearLayout.LayoutParams(dp(52), dp(34));
        paletteParams.setMargins(dp(3), 0, dp(4), dp(2));
        paletteBar.addView(paletteButton, paletteParams);
        paletteButton.setVisibility(View.GONE);
        paletteButton.setContentDescription("Change keyboard color style");
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
        paletteBar.addView(sizeButton,sizeParams);
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
                    next==400?"Prediction rise: Quick (400 ms)":
                    next==600?"Prediction rise: Smooth (600 ms)":
                    next==800?"Prediction rise: Relaxed (800 ms)":
                            "Prediction rise: Ultra Smooth (1000 ms)",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        // Double-duty diagnostic access: tap-and-hold the suggestion row
        // control to inspect learning statistics without exporting text.
        animationSpeedButton.setContentDescription(
                "Prediction animation speed; long press toggles local learning");
        // Long-press the rise control to opt into local learning.
        // Long-press again to disable; learned entries remain until cleared.
        animationSpeedButton.setOnLongClickListener(v -> {
            // An IME is not an Activity. An ordinary AlertDialog may lack a
            // valid application window token while the keyboard is displayed,
            // appearing to freeze or crashing the IME on some Samsung devices.
            // Keep opt-in immediate, reversible, and non-modal.
            boolean next = !PersonalLearning.enabled(this);
            PersonalLearning.setEnabled(this, next);
            android.widget.Toast.makeText(this,
                    next ? "Orivra learning ON (local single-word corrections)"
                         : "Orivra learning OFF",
                    android.widget.Toast.LENGTH_LONG).show();
            return true;
        });
        paletteBar.addView(animationSpeedButton,
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
        paletteBar.addView(splitModeButton,new LinearLayout.LayoutParams(dp(80),dp(34)));
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
        paletteBar.addView(nextWordButton,
                new LinearLayout.LayoutParams(dp(60),dp(34)));
        // Accessible through a long-press on the existing prediction toggle.
        // This displays only aggregate counts, never learned word pairs.

        toolbarToggle = new Button(this);
        toolbarToggle.setAllCaps(false);
        toolbarToggle.setText("⌄");
        toolbarToggle.setTextSize(18);
        toolbarToggle.setPadding(0,0,0,0);
        toolbarToggle.setContentDescription("Expand keyboard controls");
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(dp(42),dp(30));
        toggleParams.setMargins(dp(2),0,dp(3),0);
        paletteBar.addView(toolbarToggle,toggleParams);
        // Long press cycles Off -> Light -> Standard -> Crisp -> Off.
        // No additional permission and no intrusive new toolbar button.
        toolbarToggle.setOnLongClickListener(v -> {
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
        toolbarToggle.setOnClickListener(v -> {
            toolbarExpanded = !toolbarExpanded;
            updateToolbar();
        });
        // Hidden controls no longer consume an entire permanent toolbar row.
        base.addView(paletteBar, new LinearLayout.LayoutParams(-1, dp(34)));
        predictionHeader=new LinearLayout(this);
        predictionHeader.setOrientation(LinearLayout.HORIZONTAL);
        predictionHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams stripParams=new LinearLayout.LayoutParams(0,dp(48),1f);
        predictionHeader.addView(suggestionScroll,stripParams);
        predictionsToggle=new Button(this);
        predictionsToggle.setText("⌃");
        predictionsToggle.setAllCaps(false);
        predictionsToggle.setTextSize(17);
        predictionsToggle.setPadding(0,0,0,0);
        predictionsToggle.setContentDescription("Expand sentence predictions");
        predictionHeader.addView(predictionsToggle,new LinearLayout.LayoutParams(dp(38),dp(44)));
        predictionsToggle.setOnClickListener(v->toggleRisePanel());
        // Handle long presses explicitly. A normal Button long-click can be
        // swallowed by the IME's fast-touch handling on some devices.
        final boolean[] riseLongPressed={false};
        final Runnable riseHold=()->{
            riseLongPressed[0]=true;
            toolbarExpanded=!toolbarExpanded;
            updateToolbar();
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
                    if(!riseLongPressed[0])toggleRisePanel();
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
        stopDeleteRepeat();
        stopSpacebarCursorGesture();
        setColors(getCurrentInputEditorInfo());
        refreshView();
    }
    @Override public void onStartInput(EditorInfo info, boolean restarting) {
        invalidateEditingTicket();
        predictionDispatcher.cancel();
        stopDeleteRepeat();
        stopSpacebarCursorGesture();
        super.onStartInput(info, restarting);
        composing = "";
        editingInMiddle = false;
        lastSelectionStart = lastSelectionEnd = -1;
        sensitive = isSensitive(info);
        numericEditor=info!=null && ((info.inputType & InputType.TYPE_MASK_CLASS)==InputType.TYPE_CLASS_NUMBER
                || (info.inputType & InputType.TYPE_MASK_CLASS)==InputType.TYPE_CLASS_PHONE);
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
        stopSpacebarCursorGesture();
        super.onStartInputView(info, restarting);
        setColors(info);
        updateSentenceShift();
        refreshView();
    }
    @Override public void onFinishInput() {
        invalidateEditingTicket();
        predictionDispatcher.cancel();
        stopDeleteRepeat();
        stopSpacebarCursorGesture();
        currentFinish();
        super.onFinishInput();
    }
    @Override public void onFinishInputView(boolean finishingInput) {
        invalidateEditingTicket();
        predictionDispatcher.cancel();
        stopDeleteRepeat();
        stopSpacebarCursorGesture();
        super.onFinishInputView(finishingInput);
    }
    @Override public void onDestroy() {
        invalidateEditingTicket();
        predictionDispatcher.close();
        stopDeleteRepeat();
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
    private boolean isSensitive(EditorInfo info) {
        if (info == null) return true;
        int input = info.inputType;
        int cls = input & InputType.TYPE_MASK_CLASS;
        int variation = input & InputType.TYPE_MASK_VARIATION;
        return cls != InputType.TYPE_CLASS_TEXT ||
            (input & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0 ||
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_URI;
    }
    private void currentFinish() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.finishComposingText();
        composing = "";
    }
    private void updateToolbar() {
        // Refresh collapsed status and expanded layout on every toggle,
        // including toggles performed without a keyboard-view rebuild.
        if(paletteLabel!=null) {
            int mode=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                    .getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
            paletteLabel.setText(AppPalette.modeName(mode)+" • "+
                    NextWordControlLabels.buttonText(offlineNextWordEnabled()));
            paletteLabel.setVisibility(toolbarExpanded?View.GONE:View.VISIBLE);
            paletteLabel.setContentDescription(
                    NextWordControlLabels.contentDescription(offlineNextWordEnabled()));
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
        if(paletteBar!=null)paletteBar.setVisibility(toolbarExpanded?View.VISIBLE:View.GONE);
        if (paletteButton != null) paletteButton.setVisibility(toolbarExpanded ? View.VISIBLE : View.GONE);
        if (sizeButton != null) sizeButton.setVisibility(toolbarExpanded ? View.VISIBLE : View.GONE);
        if(animationSpeedButton!=null) {
            animationSpeedButton.setVisibility(toolbarExpanded?View.VISIBLE:View.GONE);
            int mode=riseMode();
            animationSpeedButton.setText("Rise "+mode);
            animationSpeedButton.setTextColor(keyTextColor);
            animationSpeedButton.setBackground(feedbackBackground(specialBackground));
            animationSpeedButton.setContentDescription("Prediction rise animation speed");
        }
        if (toolbarToggle != null) {
            toolbarToggle.setText(toolbarExpanded ? "⌃" : "⌄");
            toolbarToggle.setTextColor(keyTextColor);
            toolbarToggle.setBackground(feedbackBackground(specialBackground));
            toolbarToggle.setContentDescription(toolbarExpanded ? "Collapse keyboard controls" : "Expand keyboard controls");
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
        predictionsToggle.setText(predictionsExpanded?"⌄":"⌃");
        predictionsToggle.setContentDescription((predictionsExpanded?"Collapse predictions":"Expand predictions")+". Hold to open keyboard controls");
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
        if (base == null || keys == null || spacebarTouchActive) return;
        base.setBackgroundColor(background);
        if (paletteBar != null) paletteBar.setBackgroundColor(background);
        updateToolbar();
        if (paletteLabel != null) {
            int mode=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                    .getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
            paletteLabel.setText(AppPalette.modeName(mode)+" • "+
                    NextWordControlLabels.buttonText(offlineNextWordEnabled()));
            paletteLabel.setVisibility(toolbarExpanded?View.GONE:View.VISIBLE);
            paletteLabel.setContentDescription(
                    NextWordControlLabels.contentDescription(offlineNextWordEnabled()));
            paletteLabel.setTextColor(secondaryTextColor);
        }
        if(paletteButton!=null) {
            paletteButton.setTextColor(keyTextColor);
            paletteButton.setBackground(feedbackBackground(specialBackground));
            paletteButton.setContentDescription("Color mode: "+themeSource);
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
        final int layoutMode=!symbols?KeyboardLayoutSpec.ABC:
                extraSymbols?KeyboardLayoutSpec.EXTRA:KeyboardLayoutSpec.SYMBOLS;
        final String[] rows=KeyboardLayoutSpec.rows(layoutMode);
        for (int index = 0; index < rows.length; index++) {
            currentRowHeightDp=KeyboardSizing.keyHeightDp(heightMode,index,landscape);
            LinearLayout row = new LinearLayout(this);
            row.setMotionEventSplittingEnabled(true);
            row.setGravity(Gravity.CENTER);
            if(index==2 && !numericEditor) {
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
                if(!symbols && !numericEditor) {
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
        if(!numericEditor) addKey(bottom,KeyboardLayoutSpec.bottomModeLabel(layoutMode),
                1.3f,()->{
                    symbols=!symbols;
                    extraSymbols=false;
                    refreshView();
                });
        if(!numericEditor)addKey(bottom,",",0.95f,()->type(","));
        if(!numericEditor) {
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
        if(!numericEditor)addKey(bottom,".",0.95f,()->type("."));
        addKey(bottom,"↵",1.3f,this::enter);
        keys.addView(bottom);
        showSuggestions();
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
                type(hint);
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
                    animateKeyPress(key,false);
                    if(!cancelled[0]&&!triggered[0])type(letter);
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
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
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
        boolean usedShift=shift && value.matches("[A-Za-z]");
        if (usedShift) { value = value.toUpperCase(); if(!capsLock)shift=false; }
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
        } else if (editingInMiddle || sensitive || symbols || !value.matches("[A-Za-z]")) {
            if (!composing.isEmpty()) currentFinish();
            ic.commitText(value, 1);
        } else {
            composing += value;
            if (!ic.setComposingText(composing, 1)) { ic.commitText(value, 1); composing = ""; }
        }
        if (!value.matches("[A-Za-z]")) updateSentenceShift();
        showSuggestions();
        // Symbol taps should not discard and rebuild the key view while a
        // finger is still down. Mode/page toggles already refresh explicitly.
        if (!numericEditor && !symbols &&
                (usedShift || !value.matches("[A-Za-z]")) && !capsLock)
            suggestionHandler.post(this::refreshView);
    }
    private void backspace() {
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        prepareCursorEdit(ic);
        if (!composing.isEmpty()) {
            composing = composing.substring(0, composing.length() - 1);
            if (composing.isEmpty()) ic.finishComposingText();
            else ic.setComposingText(composing, 1);
        } else if (!ic.deleteSurroundingTextInCodePoints(1, 0)) {
            // Fallback for editors that do not support code-point deletion.
            ic.deleteSurroundingText(1, 0);
        }
        boolean oldShift=shift;
        updateSentenceShift();
        showSuggestions();
        // The previous per-repeat refresh recreated every key ~15 times/sec.
        // Preserve the live backspace touch target until release.
        if(!deleteHeld && oldShift!=shift)refreshView();
    }
    private void space() {
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        currentFinish();
        if(!ic.commitText(" ", 1))return;
        // A normal space typed after repositioning must not switch back to
        // end-of-document composition when content remains after the cursor.
        editingInMiddle=cursorHasTextAfter() || hasSelection();
        updateSentenceShift();
        showSuggestions();
        refreshView();
    }
    private void enter() {
        invalidateEditingTicket();
        cancelCursorReleaseSuggestions();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        currentFinish();
        int action = getCurrentInputEditorInfo() == null ? EditorInfo.IME_ACTION_NONE :
            getCurrentInputEditorInfo().imeOptions & EditorInfo.IME_MASK_ACTION;
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED)
            ic.performEditorAction(action);
        else {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
        updateSentenceShift();
        showSuggestions();
        refreshView();
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
            PersonalLearning.recordAcceptance(this,expected,replacement);
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
        boolean committed=false;
        ic.beginBatchEdit();
        try {
            ic.finishComposingText();
            committed=ic.commitText(" "+next+" ",1);
        } finally {ic.endBatchEdit();}
        if(committed) {
            composing="";
            confirmSuggestionTap(source);
            updateSentenceShift();
            showSuggestions();
            refreshView();
        }
        return committed;
    }
    private void acceptSuggestion(View source, String expected, String suggestion) {
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
            if (!ic.commitText(suggestion + " ", 1)) return;
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
            PersonalLearning.recordAcceptance(this,expected,suggestion);
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
        while(left>0 && Character.isLetter(before.charAt(left-1)))left--;
        while(right<after.length() && Character.isLetter(after.charAt(right)))right++;
        if(left==before.length() && right==0)return "";
        return before.subSequence(left,before.length()).toString()+after.subSequence(0,right).toString();
    }
    private void showSuggestions() {
        if(candidates==null)return;
        final String snapshot=composing;
        if(sensitive) {
            predictionDispatcher.cancel();
            renderSuggestions(snapshot,java.util.Collections.emptyList());
            return;
        }
        if(snapshot.isEmpty()) {
            predictionDispatcher.cancel();
            if(editingInMiddle || cursorHasTextAfter() || hasSelection()) {
                final String word=wordAtCursor();
                if(word.isEmpty()) {
                    renderSuggestions("",java.util.Collections.emptyList());
                    return;
                }
                // Cursor-word options use a live range-checked replacement on tap.
                renderSuggestions(word,java.util.Collections.emptyList());
                final CursorEditSnapshotGuard.Ticket editTicket=captureEditingTicket(word);
                if(editTicket==null)return; // No trustworthy editor snapshot.
                final long startedAt=CandidateLatencyMetrics.start(getPackageName());
                predictionDispatcher.scheduleEditing(word,(id,input,options)->{
                    final long computedAt=startedAt==0L?0L:System.nanoTime();
                    suggestionHandler.post(()->{
                        if(!predictionDispatcher.isCurrent(id)||sensitive||!composing.isEmpty()
                                ||!input.equals(wordAtCursor())
                                ||!isEditingTicketCurrent(editTicket))return;
                        java.util.List<String> wordOptions=
                                CursorWordCorrectionPolicy.safeOptions(input,options);
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
            if(NextWordCandidatePolicy.eligible(offlineNextWordEnabled(),sensitive,
                    editingInMiddle,hasSelection(),cursorHasTextAfter(),precedingWord())) {
                NextWordSeedModel model=offlineNextWordSeed();
                List<String> seed=model==null ? java.util.Collections.emptyList() :
                        model.suggest(context,5);
                // Use precise multiword context where known; fall back to the
                // established offline single-word model and legacy ranking.
                List<String> phrase=NextWordPhraseModel.suggest(context,3);
                renderSuggestions("",NextWordCandidatePolicy.mergeWithPhrase(
                        phrase,seed,legacy,3));
            } else renderSuggestions("",legacy);
            return;
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
                java.util.List<String> withPersonal=
                        PersonalDictionaryPolicy.merge(input,options,
                                PersonalDictionary.matching(this,input));
                // Existing opt-in Learning v1 remains independent: only a
                // twice-accepted explicit single-word correction may outrank.
                String preferred=PersonalLearning.preferredCorrection(this,input);
                if(preferred!=null) {
                    java.util.ArrayList<String> ranked=new java.util.ArrayList<>(withPersonal);
                    ranked.remove(preferred);
                    ranked.add(0,preferred);
                    if(ranked.size()>8)ranked.remove(ranked.size()-1);
                    renderSuggestions(input,ranked);
                } else renderSuggestions(input,withPersonal);
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
        boolean splitActive=splitPredictionsEnabled() && !sensitive && !input.isEmpty()
                && !editingInMiddle && !cursorHasTextAfter();
        splitPhrases=splitActive?PredictionLite.splitPhraseSuggestions(input):
                java.util.Collections.emptyList();
        splitNextWords=splitActive?PredictionLite.splitNextWords(input):
                java.util.Collections.emptyList();
        if(splitActive && (!splitNextWords.isEmpty() || !splitPhrases.isEmpty())) {
            java.util.ArrayList<String> singles=new java.util.ArrayList<>();
            for(String option:options) if(option.indexOf(' ')<0)singles.add(option);
            // For a correctly spelled word, offer useful next words rather
            // than filling the strip with near-miss spelling corrections.
            if(!splitNextWords.isEmpty()) {
                singles.clear();
                singles.addAll(splitNextWords);
            }
            // If a typo is being reconstructed into multiple words, keep
            // sentence candidates rather than hiding all useful corrections.
            if(!singles.isEmpty())options=singles;
        }
        // Display-only condensation retains the highest-ranked prediction.
        // Do not change the underlying predictor scores or accepted text.
        options=SuggestionDisplayPolicy.compact(input,options);
        latestOptions=new java.util.ArrayList<>(options);
        suggestionHandler.removeCallbacks(delayedRiseDismiss);
        // Auto-expand only when there are several real choices; avoid a jump
        // on every single character or in secure fields.
        if(input.isEmpty()) {
            if(!freezeRiseDuringDelete) {
                if(predictionsExpanded)suggestionHandler.postDelayed(delayedRiseDismiss,RISE_DISMISS_GRACE_MS);
                else predictionsExpanded=false;
            }
            manuallyCollapsedPredictions=false;
        } else if(!freezeRiseDuringDelete && !sensitive && !manuallyCollapsedPredictions
                && input.length()>=2 && (!splitPhrases.isEmpty() || options.size()>3)) {
            predictionsExpanded=true;
        } else if(!freezeRiseDuringDelete && (options.isEmpty() || (splitPhrases.isEmpty() && options.size()<=3))
                && !manuallyCollapsedPredictions) {
            if(predictionsExpanded)suggestionHandler.postDelayed(delayedRiseDismiss,RISE_DISMISS_GRACE_MS);
            else predictionsExpanded=false;
        }
        updatePredictionPanel();
        candidates.setAlpha(1f);
        if(suggestionScroll!=null && suggestionScroll.getScrollX()!=0)
            suggestionScroll.scrollTo(0,0);
        if(options.isEmpty() && !input.isEmpty() && !sensitive) {
            options=java.util.Collections.singletonList(input);
        }
        if(options.isEmpty()) {
            candidates.removeAllViews();
            candidates.setTag("message");
            TextView message=new TextView(this);
            message.setText(sensitive?"Private field • predictions off":
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
            option.setTextColor(suggestionTextColor);
            boolean fullSentenceChip=longPhrase && suggestion.indexOf(' ')>=0;
            option.setTextSize(fullSentenceChip?14:(suggestion.length()>25?14:18));
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
            // Subtle, theme-derived tint for suggestion chips. The first
            // candidate is visually distinct, but is NOT auto-committed:
            // rank alone does not establish sufficient correction confidence.
            float tint=(!input.isEmpty() && i==emphasizedIndex) ? .34f : .20f;
            int tileColor=Color.argb(
                    Math.round(Color.alpha(suggestionBackground)*(1f-tint)+Color.alpha(keyBackground)*tint),
                    Math.round(Color.red(suggestionBackground)*(1f-tint)+Color.red(keyBackground)*tint),
                    Math.round(Color.green(suggestionBackground)*(1f-tint)+Color.green(keyBackground)*tint),
                    Math.round(Color.blue(suggestionBackground)*(1f-tint)+Color.blue(keyBackground)*tint));
            if(!(option.getTag() instanceof Integer) ||
                    ((Integer)option.getTag())!=tileColor) {
                option.setBackground(feedbackBackground(tileColor));
                option.setTag(tileColor);
            }
            option.setTypeface(null,(!input.isEmpty() && i==emphasizedIndex)?
                    android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
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
