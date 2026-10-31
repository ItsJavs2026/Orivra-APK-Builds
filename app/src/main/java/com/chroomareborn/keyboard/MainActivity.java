package com.chroomareborn.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;

public final class MainActivity extends Activity {
    // Standalone preview worker: separate from the IME's own prediction queue.
    // Does not persist or transmit entered strings or prediction results.
    private final PredictionDispatcher previewDispatcher=new PredictionDispatcher();
    // Explicit user-selected documents only. No IME access or auto-export.
    private final ExecutorService learningTransferIo=
            Executors.newSingleThreadExecutor();
    private static final int REQUEST_EXPORT_LEARNING=3071;
    private static final int REQUEST_IMPORT_LEARNING=3072;
    private static final int REQUEST_EXPORT_CONTEXT=3073;
    private static final int REQUEST_EXPORT_FREQUENCY=3074;
    private static final int REQUEST_EXPORT_UNIFIED_V4=3471;
    private static final int REQUEST_IMPORT_UNIFIED_V4=3472;
    private TextView learningStatsView;
    private TextView dictionaryInfoView;
    private int dp(float n) { return Math.round(getResources().getDisplayMetrics().density * n); }

    /**
     * Non-production visual QA surface: used by GitHub emulator screenshots.
     * A fixed, non-sensitive text field keeps the REAL installed IME visible.
     * Normal launcher use never enters this path.
     */
    private boolean openDebugImeCapture() {
        boolean debuggable=(getApplicationInfo().flags &
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0;
        if(!debuggable || getIntent()==null ||
                !getIntent().getBooleanExtra("orivra_ui_capture",false))return false;
        LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12),dp(6),dp(12),dp(6));
        TextView heading=new TextView(this);
        heading.setText("Orivra emulator visual QA — test text only");
        heading.setTextColor(Color.BLACK);
        panel.addView(heading);
        EditText field=new EditText(this);
        field.setId(android.view.View.generateViewId());
        field.setContentDescription("Orivra emulator keyboard screenshot test editor");
        field.setSingleLine(false);
        field.setMinLines(5);
        field.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        field.setText("likethis heythere");
        field.setSelection(field.length());
        panel.addView(field,new LinearLayout.LayoutParams(-1,dp(160)));
        setContentView(panel);
        field.requestFocus();
        field.postDelayed(()->{
            android.view.inputmethod.InputMethodManager manager=
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
            if(manager!=null)manager.showSoftInput(
                    field,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        },300L);
        return true;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(openDebugImeCapture())return;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(16));
        root.setBackgroundColor(0xFFF6F7FA);

        TextView title = new TextView(this);
        title.setText("Orivra — Candidate 399 Test");
        title.setTextColor(Color.BLACK);
        title.setTextSize(22);
        root.addView(title);

        TextView notes = new TextView(this);
        notes.setText("\nCandidate 399 SHIFT + PHRASE EFFICIENCY TRIAL: Reuses a fixed public-phrase index for existing suggestions and updates ABC Shift labels in place instead of rebuilding the full keyboard. Keeps Candidate 396 learning, chat Send reset recovery and DOWN gesture safeguards unchanged. This is an INDEPENDENT empty-memory installation; export/import V4 manually and keep Candidate 396 and backups. Re-enable letter-row gestures in this app. Android emulator tests do NOT prove Samsung Android 17 responsiveness or gesture correctness. PREVIOUS Candidate 396 RESPONSIVE TYPING + RELIABLE DOWN: Faster learned frequency/context lookups without cached personal text, safe chat-composer refresh after Send, and deliberate ASDF/QWERTY DOWN at 32dp. DOWN on a single centered idle suggestion now selects that word plus its one safe Space boundary. Keep Candidate 391 and your V4 backup; this is an independent app requiring a manual import. NEW gestures are initially OFF: enable ASDF middle row in Settings. Emulator testing is NOT Samsung handset validation. PREVIOUS Candidate 391 SAFE V4 MERGE + NEXT-WORD ROW SWIPE TRIAL: New independent app with larger protected learning capacities (384 accepted corrections, 320 context keys, 400 word frequencies) and actual QWERTY/ASDF swipes for next-word predictions such as I | The | Hey. Preserve original 378 and 386 V4 files; import 386 backup first, then 378 into THIS NEW empty candidate, confirm each, and export a NEW V4 to verify the 304/242/326 union. Keep both old apps installed. ASDF mode starts OFF in this separate app and must be re-enabled after install. Old Candidate 386 LETTER-ROW SWIPE TRIAL: NEW OFF-by-default gestures start on actual ASDF or QWERTY letter keys, NOT the prediction strip. Select ASDF middle row, QWERTY top row, both, or OFF in Settings. Swipe LEFT for first visible candidate, RIGHT for rightmost visible candidate, DOWN to accept CENTER prediction with one safe trailing Space. One-, two- and three-letter prefixes preserve normal Space completion boundary rules; do not type an extra starting letter or automatically Send. Previous Candidate 378: NEW — Space accepts a safe final highlighted one-, two-, or three-letter prefix completion, retaining one trailing separator. Long drafts: late Android selection callbacks at the document end no longer force a sticky middle-edit state. The top letter-row swipe experiment is NOT included yet. PREVIOUS Candidate 373: Swipe across prediction chips left/right; swipe upward to select a RISE alternative when expanded. Optional gesture on/off. Space accepts the live highlighted correction plus one proper word separator, with independent on/off control. Rejects stale or private editor changes. Previous Candidate 371: Shaded neutral light/dark surfaces with confidence-scored app color accents, background-only icon parsing, instant visual feedback (no key shrink) and fast 320 ms special-character holds. Both feedback options can be switched OFF independently to compare with the original. Previous Candidate 365: Tap a completed word's correction in existing text and start typing a new word before the next word; the first letter receives one left separator while preserving the existing right separator. Example: I am hungry -> tap am -> type not -> I am not hungry. One-shot editor/session guarded; normal typing path untouched. Prior Candidate 363: Combined short joined-word and doubled QWERTY neighbor correction (e.g. Iwikk → I will) is now available as a tap-only suggestion, with its single safe separator. Retains the existing smooth typing and long-draft prediction. Earlier Candidate 361: Cursor re-entry prediction: continue suggesting the complete word when an additional letter is typed after revisiting an existing committed word. Preserve Candidate 359 long-draft fallback and Candidate 354 touch smoothness. Earlier Candidate 359: Long-draft prediction continuity (word-level preview chips and next-word fallback after Space; safe end-of-document only; preserves Candidate 354 smoothness, private-field gates, Candidate 345 stable prediction strip and V4 learning). PREVIOUS Candidate 354: Prediction continuity (one trailing separator on explicit suggestion taps, current cursor word in the center of three steady chips, safe next-word recovery when long messages leave stale editing state). Alphabet presses preserve overlapping two-thumb down-order and do not cancel the primary key when another pointer appears. Keeps Candidate 350's prediction timing, Candidate 345's no-flicker strip and V4 learning/export. Candidate 352: Highlighted-word selection safety. A stale composing/caret precondition now verifies the live editor before a one-time replacement, never finalizing an active highlight before commit. No extra IPC on normal typed keys. Candidate 350 prediction timing, automatic Rise, split suggestions and V4 backup all remain unchanged. Candidate 350: NORMAL BAR ARRIVAL SPEED: offline predictor uses 12 ms for short words, 20 ms for medium, 28 ms for longer reconstruction (instead of fixed 40 ms); separate background worker priority is increased but stays below the UI thread, and a public lexicon is warmed away from key taps. Candidate 345 strip, automatic Rise and all Candidate 347 learning/V4 controls remain unchanged. Candidate 347: Opt-in typed-word learning across sessions and unified V4 backup. The verified Space-boundary worker runs off the typing thread; only ordinary allowed fields are eligible. The main prediction bar retains the Candidate 345 Samsung-tested visual continuity. Remember typed words defaults OFF and requires Learning ON. Reopen a text field after changing the setting. V4 includes pending learned words and their counts. New trial installs separately, so import a previous V3 backup here if needed. Candidate 345: SCREEN-RECORDING NORMAL BAR FLICKER FIX: The normal three-slot bar now retains its side-chip visuals during the immediate-word-to-background-prediction handoff, but stale options cannot be tapped. Fast Backspace leaves the existing blank chip row mounted. Automatic Rise and split predictions are unchanged. Candidate 344: STABLE NORMAL PREDICTION BAR: Fast typing retains three persistent prediction chip views, showing updated text in place without frequent view rebuilding, and avoids repeated unchanged style layout work. Automatic Rise, split-mode, word learning, Backspace, V3 backup and security gates remain. Candidate 343: FAST TYPING / NO KEEP TYPING FLICKER: Empty prediction rows remain quiet, the literal typed word appears immediately, automatic Rise and split next-word choices are preserved, while the unwanted empty placeholder and redundant cursor echoes are addressed. BACKSPACE AND COMPOSING FIXES RETAINED. Candidate 345: COMPOSING SAFETY, TAPPED-WORD CONTEXT & LATENCY: This trial guards against double composing writes, learns repeated typed-word sequences and multiword phrase completions when Learning is ON, and records opt-in memory-only typed-key latency. Novel sequence learning is session-only and NOT included in V3 backup yet. BACKSPACE LAST-LETTER FIX: A single Backspace really deletes the final composing character instead of only ending composition. No extra press or misleading empty-suggestions flash while a letter remains. Retains Candidate 333 frequent/short-word learning, committed phrase learning, safer pause timing, curated can't/ain't/that's contractions, private numeric banking keypad and V1/V2/V3 all-in-one backup. GOOGLE SEARCH FIX: normal Search/Go fields no longer inherit private-field restrictions from NO_SUGGESTIONS. A Search/Go Enter tap now performs the host action, not a newline. Backspace and spacebar cursor gestures remain available regardless of suggestion UI hints, and actual password/email/URI fields remain private. Tap-only we're and how's choices now appear promptly when you type Were or hows, even after punctuation. No silent changes to correctly typed words. Export and Import manually saved words and learned corrections via the Android document picker, with preview, approval, and safe merge. Learning V2 supports explicit accepted contractions like can't, ain't and we're; preserves V1 learning format within the same application. Idle next-word suggestions remain centered; active corrections and the right caret stay unchanged. Includes the optional, default-OFF offline English dictionary trial. Google Keep compatibility from Candidate 300 remains intact. Ordinary Google Keep notes can show offline predictions when Android requests no suggestions, while password, email, URL, numeric and private fields remain protected. Personal correction learning is disabled where the editor requests no suggestions or no personalized learning. This trial keeps the Candidate 298 SentenceFlow prediction engine, cleaned one-arrow UI, Safe Enter, Backspace and theme behavior. Test in a disposable note. Spanish remains research-only; letter-to-letter swipe typing remains incomplete. This is a separate trial keyboard; keep the stable Orivra installed. Avoid entering passwords or other sensitive text during testing.\n");
        notes.setTextColor(0xFF303846);
        notes.setTextSize(15);
        root.addView(notes);

        // Build 303 research, default OFF. Explicit opt-in and app-local only.
        // Never reads notes or keyboard input; changing this preference takes
        // effect when a text field is reopened and onStartInput runs again.
        TextView dictionaryResearchInfo=new TextView(this);
        dictionaryResearchInfo.setText("\nExperimental offline vocabulary: 78,942 extra exact English words. "
                +"It helps recognize intact words, but does not add new fuzzy typo candidates. "
                +"Uses about 280 KB APK storage plus runtime memory. "
                +"Default OFF. Do not enable in production-critical typing.");
        dictionaryResearchInfo.setTextColor(0xFF303846);
        dictionaryResearchInfo.setTextSize(14);
        root.addView(dictionaryResearchInfo);
        Button offlineExactDictionary=new Button(this);
        offlineExactDictionary.setAllCaps(false);
        boolean exactEnabled=getSharedPreferences(
                OfflineDictionaryActivationPolicy.PREFS,MODE_PRIVATE)
                .getBoolean(OfflineDictionaryActivationPolicy.KEY,false);
        offlineExactDictionary.setText("Experimental offline English dictionary: "+
                (exactEnabled?"ON":"OFF")+" (tap to change)");
        offlineExactDictionary.setOnClickListener(v->{
            android.content.SharedPreferences prefs=getSharedPreferences(
                    OfflineDictionaryActivationPolicy.PREFS,MODE_PRIVATE);
            boolean enabled=!prefs.getBoolean(OfflineDictionaryActivationPolicy.KEY,false);
            prefs.edit().putBoolean(OfflineDictionaryActivationPolicy.KEY,enabled).apply();
            offlineExactDictionary.setText("Experimental offline English dictionary: "+
                    (enabled?"ON":"OFF")+" (tap to change)");
            android.widget.Toast.makeText(this,
                    "Reopen a text field to apply. Password/privacy protection is unchanged.",
                    android.widget.Toast.LENGTH_LONG).show();
        });
        root.addView(offlineExactDictionary);

        Button settings = new Button(this);
        settings.setText("1. Enable keyboard in Android settings");
        settings.setOnClickListener(v -> startActivity(new android.content.Intent("android.settings.INPUT_METHOD_SETTINGS")));
        root.addView(settings);

        Button picker = new Button(this);
        picker.setText("2. Choose Orivra Candidate 399 keyboard");
        picker.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });
        root.addView(picker);

        Button theme = new Button(this);
        int initialMode=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE).getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
        theme.setText("3. Keyboard theme: "+AppPalette.modeName(initialMode));
        theme.setOnClickListener(v -> {
            int mode=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                    .getInt(AppPalette.KEY_MODE,AppPalette.DEFAULT);
            int next=AppPalette.nextMode(mode);
            getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                    .edit().putInt(AppPalette.KEY_MODE,next).apply();
            theme.setText("3. Keyboard theme: "+AppPalette.modeName(next));
            android.widget.Toast.makeText(this,"Tap the palette icon on the keyboard to see changes immediately",android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(theme);
        Button cleanLook=new Button(this);
        cleanLook.setAllCaps(false);
        cleanLook.setText("Apply Clean Neutral keyboard look");
        cleanLook.setOnClickListener(v -> {
            getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE).edit()
                    .putInt(AppPalette.KEY_MODE,AppPalette.CLEAN_NEUTRAL).apply();
            theme.setText("3. Keyboard theme: "+AppPalette.modeName(AppPalette.CLEAN_NEUTRAL));
            android.widget.Toast.makeText(this,
                    "Clean Neutral selected; reopen keyboard to apply",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(cleanLook);

        Button height=new Button(this);
        int saved=getSharedPreferences(KeyboardSizing.PREFS,MODE_PRIVATE)
                .getInt(KeyboardSizing.KEY_MODE,KeyboardSizing.DEFAULT);
        height.setText("6. Keyboard height: "+KeyboardSizing.name(saved));
        height.setOnClickListener(v -> {
            int mode=getSharedPreferences(KeyboardSizing.PREFS,MODE_PRIVATE)
                    .getInt(KeyboardSizing.KEY_MODE,KeyboardSizing.DEFAULT);
            int next=KeyboardSizing.next(mode);
            getSharedPreferences(KeyboardSizing.PREFS,MODE_PRIVATE)
                    .edit().putInt(KeyboardSizing.KEY_MODE,next).apply();
            height.setText("6. Keyboard height: "+KeyboardSizing.name(next));
            android.widget.Toast.makeText(this,
                    "Size saved. Reopen the keyboard or tap ↕ beside the palette.",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(height);

        // Build 287: discoverable feedback settings in addition to the
        // toolbar long-press shortcut. Android remains responsible for actual
        // motor strength and the device's master haptic setting.
        TextView hapticHelp=new TextView(this);
        hapticHelp.setText("\nKeyboard vibrations: choose the feedback feel. "
                +"The system can suppress feedback when vibration is disabled.");
        hapticHelp.setTextSize(15);
        hapticHelp.setTextColor(0xFF303846);
        root.addView(hapticHelp);
        Button hapticButton=new Button(this);
        hapticButton.setAllCaps(false);
        final android.content.SharedPreferences feedback=getSharedPreferences(
                "keyboard_feedback",MODE_PRIVATE);
        int currentHaptic=feedback.getBoolean("orivra_haptic_enabled",true)?
                KeyboardHapticStyle.normalize(feedback.getInt(
                        "orivra_haptic_style",KeyboardHapticStyle.STANDARD)):
                KeyboardHapticStyle.OFF;
        hapticButton.setText("Key vibrations: "+KeyboardHapticStyle.label(currentHaptic)
                +" (tap to change)");
        hapticButton.setOnClickListener(v -> {
            int active=feedback.getBoolean("orivra_haptic_enabled",true)?
                    KeyboardHapticStyle.normalize(feedback.getInt(
                            "orivra_haptic_style",KeyboardHapticStyle.STANDARD)):
                    KeyboardHapticStyle.OFF;
            int next=KeyboardHapticStyle.next(active);
            feedback.edit().putBoolean("orivra_haptic_enabled",
                    KeyboardHapticStyle.enabled(next))
                    .putInt("orivra_haptic_style",next==KeyboardHapticStyle.OFF?
                            KeyboardHapticStyle.STANDARD:next).apply();
            hapticButton.setText("Key vibrations: "+KeyboardHapticStyle.label(next)
                    +" (tap to change)");
            if(KeyboardHapticStyle.enabled(next))
                v.performHapticFeedback(next==KeyboardHapticStyle.LIGHT?
                        android.view.HapticFeedbackConstants.CLOCK_TICK:
                        next==KeyboardHapticStyle.CRISP &&
                        android.os.Build.VERSION.SDK_INT>=30?
                        android.view.HapticFeedbackConstants.CONFIRM:
                        android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        });
        root.addView(hapticButton);

        // Research 370 isolated visual feedback A/B controls. Each change
        // takes effect at the next editor focus, never mid-keypress.
        TextView experimentTitle370=new TextView(this);
        experimentTitle370.setText("\nShaded Surface trial • keyboard responsiveness");
        experimentTitle370.setTextSize(16);
        experimentTitle370.setTextColor(0xFF303846);
        root.addView(experimentTitle370);

        Button visualFeedback370=new Button(this);
        visualFeedback370.setAllCaps(false);
        visualFeedback370.setText("Instant key visual feedback: "+
                (feedback.getBoolean(
                        KeyPressVisualPolicy369.KEY_INSTANT_FEEDBACK,true)?
                        "ON (no key shrink)":"OFF (original animation)"));
        visualFeedback370.setOnClickListener(v->{
            boolean nextEnabled=!feedback.getBoolean(
                    KeyPressVisualPolicy369.KEY_INSTANT_FEEDBACK,true);
            feedback.edit().putBoolean(
                    KeyPressVisualPolicy369.KEY_INSTANT_FEEDBACK,nextEnabled).apply();
            visualFeedback370.setText("Instant key visual feedback: "+
                    (nextEnabled?"ON (no key shrink)":
                            "OFF (original animation)"));
            android.widget.Toast.makeText(this,
                    "Reopen the keyboard to compare key feel",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(visualFeedback370);

        Button symbolHold370=new Button(this);
        symbolHold370.setAllCaps(false);
        symbolHold370.setText("Special character hold: "+
                (feedback.getBoolean(
                        SymbolHoldTiming367.KEY_FAST_SYMBOL_HOLD,true)?
                        "Fast (320 ms)":"Android system timing"));
        symbolHold370.setOnClickListener(v->{
            boolean nextEnabled=!feedback.getBoolean(
                    SymbolHoldTiming367.KEY_FAST_SYMBOL_HOLD,true);
            feedback.edit().putBoolean(
                    SymbolHoldTiming367.KEY_FAST_SYMBOL_HOLD,nextEnabled).apply();
            symbolHold370.setText("Special character hold: "+
                    (nextEnabled?"Fast (320 ms)":"Android system timing"));
            android.widget.Toast.makeText(this,
                    "Reopen the keyboard to compare special-character holds",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(symbolHold370);

        TextView swipeInfo372=new TextView(this);
        swipeInfo372.setText("\nPrediction gestures (experimental)\n"+
                "On the suggestion strip, slide toward the next chip to "+
                "select it. Swipe up to choose a RISE alternative when "+
                "the expanded panel is visible. Normal taps still work.");
        swipeInfo372.setTextSize(15);
        swipeInfo372.setTextColor(0xFF303846);
        root.addView(swipeInfo372);
        Button swipePredictions372=new Button(this);
        swipePredictions372.setAllCaps(false);
        swipePredictions372.setText("Swipe prediction selection: "+
                (feedback.getBoolean(PredictionSwipe372.PREF,true)?"ON":"OFF"));
        swipePredictions372.setOnClickListener(v->{
            boolean next=!feedback.getBoolean(PredictionSwipe372.PREF,true);
            feedback.edit().putBoolean(PredictionSwipe372.PREF,next).apply();
            swipePredictions372.setText("Swipe prediction selection: "+
                    (next?"ON":"OFF"));
            android.widget.Toast.makeText(this,
                    "Reopen keyboard to apply swipe option",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(swipePredictions372);

        TextView actualLetterSwipe386=new TextView(this);
        actualLetterSwipe386.setText("\nPrediction swipes on LETTER KEYS (experimental)\n"+
                "Swipe left/right directly on QWERTY or ASDF keys for a side "+
                "prediction; swipe DOWN to insert the CENTER prediction "+
                "using the normal single-Space completion rules, including "+
                "one-letter and short-word prefixes. No Send action or "+
                "extra Space. Choose ASDF, QWERTY or both, independently "+
                "of prediction-strip swipes. Ordinary taps still type.");
        actualLetterSwipe386.setTextSize(15);
        actualLetterSwipe386.setTextColor(0xFF303846);
        root.addView(actualLetterSwipe386);
        Button rowSwipeMode386=new Button(this);
        rowSwipeMode386.setAllCaps(false);
        rowSwipeMode386.setText("Swipe predictions from letter rows: "+
                LetterRowPredictionSwipe386.label(feedback.getInt(
                        LetterRowPredictionSwipe386.PREF,0)));
        rowSwipeMode386.setOnClickListener(v->{
            int mode=LetterRowPredictionSwipe386.next(
                    feedback.getInt(LetterRowPredictionSwipe386.PREF,0));
            feedback.edit().putInt(LetterRowPredictionSwipe386.PREF,mode).apply();
            rowSwipeMode386.setText("Swipe predictions from letter rows: "+
                    LetterRowPredictionSwipe386.label(mode));
            android.widget.Toast.makeText(this,
                    "Reopen a text field to change the letter-row swipe mode",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(rowSwipeMode386);

        Button spaceCorrection372=new Button(this);
        spaceCorrection372.setAllCaps(false);
        spaceCorrection372.setText("Space accepts highlighted correction: "+
                (feedback.getBoolean(HighlightedSpace372.PREF,true)?
                        "ON":"OFF"));
        spaceCorrection372.setOnClickListener(v->{
            boolean next=!feedback.getBoolean(HighlightedSpace372.PREF,true);
            feedback.edit().putBoolean(HighlightedSpace372.PREF,next).apply();
            spaceCorrection372.setText("Space accepts highlighted correction: "+
                    (next?"ON":"OFF"));
            android.widget.Toast.makeText(this,
                    "Reopen keyboard to apply Space correction option",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(spaceCorrection372);

        // Candidate 378: side-by-side, independently reversible broad-thumb
        // experiments. Settings are sampled on next editor focus only.
        TextView thumbTitle378=new TextView(this);
        thumbTitle378.setText("\nBroad-thumb typing (experimental)\n"+
                "Drift control keeps a touch on its original letter; it never "+
                "moves characters across keys. Wider outer letters preserve "+
                "the rest of the QWERTY layout. Reopen keyboard after changing.");
        thumbTitle378.setTextSize(15);
        thumbTitle378.setTextColor(0xFF303846);
        root.addView(thumbTitle378);
        Button thumbDrift378=new Button(this);
        thumbDrift378.setAllCaps(false);
        thumbDrift378.setText("Thumb drift tolerance: "+
                (feedback.getBoolean(ThumbTapGuard377.PREF,true)?"ON":"OFF"));
        thumbDrift378.setOnClickListener(v->{
            boolean next=!feedback.getBoolean(ThumbTapGuard377.PREF,true);
            feedback.edit().putBoolean(ThumbTapGuard377.PREF,next).apply();
            thumbDrift378.setText("Thumb drift tolerance: "+(next?"ON":"OFF"));
            android.widget.Toast.makeText(this,
                    "Reopen keyboard to apply thumb drift option",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(thumbDrift378);
        Button edgeKeys378=new Button(this);
        edgeKeys378.setAllCaps(false);
        edgeKeys378.setText("Wider edge letters: "+
                (feedback.getBoolean(EdgeKeyWeights378.PREF,true)?"ON":"OFF"));
        edgeKeys378.setOnClickListener(v->{
            boolean next=!feedback.getBoolean(EdgeKeyWeights378.PREF,true);
            feedback.edit().putBoolean(EdgeKeyWeights378.PREF,next).apply();
            edgeKeys378.setText("Wider edge letters: "+(next?"ON":"OFF"));
            android.widget.Toast.makeText(this,
                    "Reopen keyboard to apply edge letter widths",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        root.addView(edgeKeys378);

        boolean follow=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                .getBoolean(AppPalette.KEY_FOLLOW_SYSTEM,true);
        Button system=new Button(this);
        system.setAllCaps(false);
        system.setText("4. Follow Samsung Light/Dark: "+(follow?"ON":"OFF"));
        root.addView(system);
        system.setOnClickListener(v -> {
            boolean old=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                    .getBoolean(AppPalette.KEY_FOLLOW_SYSTEM,true);
            boolean next=!old;
            getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE).edit()
                    .putBoolean(AppPalette.KEY_FOLLOW_SYSTEM,next).apply();
            system.setText("4. Follow Samsung Light/Dark: "+(next?"ON":"OFF"));
        });
        Button appearance=new Button(this);
        appearance.setAllCaps(false);
        boolean manualDark=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                .getBoolean(AppPalette.KEY_MANUAL_DARK,true);
        appearance.setText("5. Manual Light/Dark (if follow OFF): "+(manualDark?"Dark":"Light"));
        root.addView(appearance);
        appearance.setOnClickListener(v -> {
            boolean old=getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE)
                    .getBoolean(AppPalette.KEY_MANUAL_DARK,true);
            boolean next=!old;
            getSharedPreferences(AppPalette.PREFS,MODE_PRIVATE).edit()
                    .putBoolean(AppPalette.KEY_MANUAL_DARK,next).apply();
            appearance.setText("5. Manual Light/Dark (if follow OFF): "+(next?"Dark":"Light"));
        });
        // Build 285 — explicit local vocabulary, separate from automatic
        // correction learning. This screen never reads editor content.
        TextView dictionaryTitle=new TextView(this);
        dictionaryTitle.setText("\nPersonal dictionary (English words, including can't and ain't)");
        dictionaryTitle.setTextSize(17);
        dictionaryTitle.setTextColor(0xFF303846);
        root.addView(dictionaryTitle);

        TextView dictionaryInfo=new TextView(this);
        dictionaryInfo.setText(PersonalDictionary.summary(this)
                +"\nSave names or words you use often. Do not save passwords.");
        dictionaryInfo.setTextColor(0xFF455263);
        root.addView(dictionaryInfo);
        dictionaryInfoView=dictionaryInfo;

        EditText dictionaryWord=new EditText(this);
        dictionaryWord.setSingleLine(true);
        dictionaryWord.setInputType(InputType.TYPE_CLASS_TEXT|
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        dictionaryWord.setHint("Word to add or remove (2–24 letters)");
        root.addView(dictionaryWord,new LinearLayout.LayoutParams(-1,dp(56)));

        Button addDictionaryWord=new Button(this);
        addDictionaryWord.setAllCaps(false);
        addDictionaryWord.setText("Add word to my dictionary");
        addDictionaryWord.setOnClickListener(v->{
            PersonalDictionary.Add status=PersonalDictionary.add(this,
                    dictionaryWord.getText().toString());
            String message;
            switch(status) {
                case ADDED: message="Saved locally.";break;
                case EXISTS: message="This word is already saved.";break;
                case FULL: message="Personal dictionary is full (250 words).";break;
                default: message="Use only 2–24 English letters, without spaces.";
            }
            android.widget.Toast.makeText(this,message,
                    android.widget.Toast.LENGTH_SHORT).show();
            dictionaryInfo.setText(PersonalDictionary.summary(this));
        });
        root.addView(addDictionaryWord);

        Button removeDictionaryWord=new Button(this);
        removeDictionaryWord.setAllCaps(false);
        removeDictionaryWord.setText("Remove entered word");
        removeDictionaryWord.setOnClickListener(v->{
            boolean removed=PersonalDictionary.remove(this,
                    dictionaryWord.getText().toString());
            android.widget.Toast.makeText(this,
                    removed?"Word removed.":"No matching saved word.",
                    android.widget.Toast.LENGTH_SHORT).show();
            dictionaryInfo.setText(PersonalDictionary.summary(this));
        });
        root.addView(removeDictionaryWord);

        Button inspectDictionary=new Button(this);
        inspectDictionary.setAllCaps(false);
        inspectDictionary.setText("View saved words");
        inspectDictionary.setOnClickListener(v->{
            java.util.List<String> words=PersonalDictionary.savedWords(this);
            new android.app.AlertDialog.Builder(this)
                .setTitle("My saved words (on device)")
                .setMessage(words.isEmpty()?"No saved words yet.":
                        android.text.TextUtils.join("\n",words))
                .setPositiveButton("Close",null)
                .show();
        });
        root.addView(inspectDictionary);

        // Learning controls belong to the Activity, not a modal IME window.
        TextView learningTitle = new TextView(this);
        learningTitle.setText("Orivra Learning V2 — on-device, explicit word corrections");
        learningTitle.setTextColor(0xFF303846);
        learningTitle.setTextSize(16);
        root.addView(learningTitle);

        Button learning = new Button(this);
        learning.setAllCaps(false);
        learning.setText("Learning: " + (PersonalLearning.enabled(this) ? "ON" : "OFF"));
        learning.setOnClickListener(v -> {
            boolean next = !PersonalLearning.enabled(this);
            PersonalLearning.setEnabled(this, next);
            learning.setText("Learning: " + (next ? "ON" : "OFF"));
        });
        root.addView(learning);

        TextView learningStats = new TextView(this);
        learningStats.setText(PersonalLearning.diagnosticSummary(this));
        learningStats.setTextColor(0xFF455263);
        root.addView(learningStats);
        learningStatsView=learningStats;

        Button refreshLearning = new Button(this);
        refreshLearning.setAllCaps(false);
        refreshLearning.setText("Refresh local learning statistics");
        refreshLearning.setOnClickListener(v ->
                learningStats.setText(PersonalLearning.diagnosticSummary(this)));
        root.addView(refreshLearning);

        Button reviewLearning = new Button(this);
        reviewLearning.setAllCaps(false);
        reviewLearning.setText("Review learned corrections (on this device)");
        reviewLearning.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                .setTitle("Locally learned corrections")
                .setMessage(PersonalLearning.learnedPairs(this))
                .setPositiveButton("Close", null)
                .show());
        root.addView(reviewLearning);

        Button resetLearning = new Button(this);
        resetLearning.setAllCaps(false);
        resetLearning.setText("Clear learned corrections (confirm)");
        resetLearning.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                .setTitle("Clear Orivra learning?")
                .setMessage("Delete all local learned corrections and disable learning?")
                .setNegativeButton("Cancel", (dialog, which) -> {})
                .setPositiveButton("Clear", (dialog, which) -> {
                    PersonalLearning.clear(this);
                    learning.setText("Learning: OFF");
                    learningStats.setText(PersonalLearning.diagnosticSummary(this));
                }).show());
        root.addView(resetLearning);

        // Candidate 347: independent consent for remembering verified typed
        // words across sessions. Unlike V1 correction learning, this may
        // include names; explain clearly and default OFF. No raw text display.
        TextView typedInfo347=new TextView(this);
        typedInfo347.setText("\nTyped-word memory (Candidate 347, optional)\n"
                +"After repeated successful Space commits, the keyboard can "
                +"learn manually typed words—including short words and word "
                +"pairs—across sessions. Words may include names. Stored "
                +"privately on this device; never sent to a server. This "
                +"setting is separate from Learning and starts OFF. "
                +"Reopen a text field to apply changes.");
        typedInfo347.setTextColor(0xFF303846);
        typedInfo347.setTextSize(14);
        root.addView(typedInfo347);

        Button remember347=new Button(this);
        remember347.setAllCaps(false);
        remember347.setText("Remember words I type: "+
                (TypedWordsConsent341.enabled(this)?"ON":"OFF"));
        remember347.setOnClickListener(v->{
            if(TypedWordsConsent341.enabled(this)){
                if(TypedWordsConsent341.setEnabled(this,false))
                    remember347.setText("Remember words I type: OFF");
                return;
            }
            new android.app.AlertDialog.Builder(this)
                .setTitle("Remember verified typed words?")
                .setMessage("This option can remember names or personal "
                        +"words, even if they are not passwords. Only words "
                        +"typed with Orivra in allowed ordinary text fields "
                        +"and committed by a successful Space are eligible. "
                        +"It stays on your device, uses no network, and can "
                        +"be erased separately. Your existing Learning toggle "
                        +"must also be ON. You can export the data manually "
                        +"as part of a V4 plaintext backup. Enable?")
                .setNegativeButton("Not now",null)
                .setPositiveButton("Enable typed-word memory",(dialog,which)->{
                    if(TypedWordsConsent341.setEnabled(this,true)) {
                        remember347.setText("Remember words I type: ON");
                        android.widget.Toast.makeText(this,
                                "Reopen a text field to activate.",
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                }).show();
        });
        root.addView(remember347);

        TextView typedCounts347=new TextView(this);
        typedCounts347.setText("Stored typed-word counts: select Refresh. "
                +"Individual words are never displayed in these statistics.");
        typedCounts347.setTextColor(0xFF455263);
        root.addView(typedCounts347);

        Button typedRefresh347=new Button(this);
        typedRefresh347.setAllCaps(false);
        typedRefresh347.setText("Refresh saved typed-word counts");
        typedRefresh347.setOnClickListener(v->learningTransferIo.execute(()->{
            String summary;
            try {
                TypedWordEvidenceStore341.Snapshot e=
                        TypedWordEvidenceStore341.read(this);
                summary="On device: "+e.vocabulary()+" typed words ("
                        +e.confirmedWords()+" learned 3+ times), "
                        +e.associations()+" word pairs ("
                        +e.confirmedPairs()+" learned 3+ times).";
            } catch(RuntimeException invalid) {
                summary="Typed-word storage is invalid; clear it before reuse.";
            }
            final String ready=summary;
            runOnUiThread(()->{
                if(!isFinishing()&&!isDestroyed())typedCounts347.setText(ready);
            });
        }));
        root.addView(typedRefresh347);

        Button typedClear347=new Button(this);
        typedClear347.setAllCaps(false);
        typedClear347.setText("Turn OFF and erase typed-word memory");
        typedClear347.setOnClickListener(v->
                new android.app.AlertDialog.Builder(this)
                .setTitle("Erase verified typed-word memory?")
                .setMessage("Disable new typed-word learning and delete "
                        +"only its separate on-device counts. Existing "
                        +"V1/V2/V3 learning, saved dictionary and exported "
                        +"backup files are not deleted. You can cancel.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Disable and erase",(dialog,which)->
                    learningTransferIo.execute(()->{
                        boolean ok=TypedWordsConsent341.setEnabled(this,false)
                                &&TypedWordEvidenceStore341.clearAll(this);
                        runOnUiThread(()->{
                            if(isFinishing()||isDestroyed())return;
                            remember347.setText("Remember words I type: OFF");
                            typedCounts347.setText(ok?
                                    "Typed-word counts erased. Reopen keyboard.":
                                    "Could not erase the typed-word data.");
                        });
                    })).show());
        root.addView(typedClear347);

        // One unified V4 is the RECOMMENDED current backup; older V1/V2/V3
        // controls remain for export compatibility and rollback testing.
        TextView unifiedInfo347=new TextView(this);
        unifiedInfo347.setText("\nRecommended: one all-in-one V4 backup\n"
                +"Includes the older V1 dictionary/corrections, V2 context, "
                +"V3 word frequencies and V4 verified typed words/pairs. "
                +"It also saves counts for words typed just 1–2 times. "
                +"Plaintext may contain names and personal words: choose a "
                +"trusted location. Older Orivra releases cannot import V4; "
                +"keep your previous V3 export for rollback.");
        unifiedInfo347.setTextColor(0xFF303846);
        unifiedInfo347.setTextSize(15);
        root.addView(unifiedInfo347);

        Button unifiedExport347=new Button(this);
        unifiedExport347.setAllCaps(false);
        unifiedExport347.setText("Export ONE complete learning backup (V4)");
        unifiedExport347.setOnClickListener(v->
                new android.app.AlertDialog.Builder(this)
                .setTitle("Export one complete learning backup?")
                .setMessage("The V4 text file includes saved words, "
                        +"corrections, contexts, frequencies and verified "
                        +"typed-word/pair evidence—even counts of 1 or 2. "
                        +"It could reveal names or typing habits. "
                        +"You choose the file and location. Export?")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Choose trusted location",(dialog,which)->
                        startActivityForResult(
                                LearningTransferDocumentIO.createUnifiedV4Intent(),
                                REQUEST_EXPORT_UNIFIED_V4))
                .show());
        root.addView(unifiedExport347);

        Button unifiedImport347=new Button(this);
        unifiedImport347.setAllCaps(false);
        unifiedImport347.setText("Import V4 (or earlier V1/V2/V3) backup");
        unifiedImport347.setOnClickListener(v->startActivityForResult(
                LearningTransferDocumentIO.openIntent(),
                REQUEST_IMPORT_UNIFIED_V4));
        root.addView(unifiedImport347);

        TextView migration391=new TextView(this);
        migration391.setText("\nSAFE CANDIDATE 396 → 399 LEARNING TRANSFER\n"+
                "Candidate 399 is a NEW separate keyboard and cannot read "+
                "Candidate 396 private learning or settings automatically. "+
                "Keep Candidates 391 and 396 installed and preserve all "+
                "original V4 backups. Export a NEW V4 from Candidate 396 "+
                "after your current testing; import that V4 into Candidate 399 "+
                "using the explicit Import V4 button and confirmation preview. "+
                "Export a NEW Candidate 399 V4 and verify the checksum and "+
                "learning record counts before relying on 399. Do NOT delete "+
                "old installations or clear learning. Re-enable letter-row "+
                "gestures manually. Capacities remain 384 corrections / "+
                "320 context / 400 frequencies.");
        migration391.setTextSize(15);
        migration391.setTextColor(0xFF303846);
        root.addView(migration391);

        // Build 307 research: explicit user-selected backup, never automatic.
        TextView transferNotice=new TextView(this);
        transferNotice.setText("\nPortable learning backup (experimental)\n"
                +"Export only manually saved words and intentional accepted corrections. "
                +"The file is plaintext: it may include names or personalized words. "
                +"Choose a location you trust. No full notes or typing history. "+
                "The familiar V1 export works with older builds; V2 can also "+
                "include compact learned word-use relationships.");
        transferNotice.setTextColor(0xFF455263);
        transferNotice.setTextSize(14);
        root.addView(transferNotice);

        Button exportLearning=new Button(this);
        exportLearning.setAllCaps(false);
        exportLearning.setText("Export my saved words + learned corrections");
        exportLearning.setOnClickListener(v->
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Export local learning?")
                    .setMessage("You choose where the plain-text backup is saved. "
                            +"It may reveal words and names you saved and deliberate "
                            +"correction pairs. No full notes or raw typing "
                            +"history are included. Save only to a trusted place.")
                    .setNegativeButton("Cancel",null)
                    .setPositiveButton("Choose location",(dialog,which)->
                            startActivityForResult(
                                    LearningTransferDocumentIO.createIntent(),
                                    REQUEST_EXPORT_LEARNING))
                    .show());
        root.addView(exportLearning);

        Button exportContext=new Button(this);
        exportContext.setAllCaps(false);
        exportContext.setText("Export my context learning (V2 backup)");
        exportContext.setOnClickListener(v->
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Export contextual word learning?")
                    .setMessage("This optional plaintext V2 backup includes "+
                            "two-word habits and counts, saved words, and "+
                            "accepted corrections. It may reveal how you type. "+
                            "Older Orivra versions cannot import V2; keep a "+
                            "V1 backup too for rollback. Save only to a trusted place.")
                    .setNegativeButton("Cancel",null)
                    .setPositiveButton("Choose location",(dialog,which)->
                            startActivityForResult(
                                    LearningTransferDocumentIO.createContextIntent(),
                                    REQUEST_EXPORT_CONTEXT))
                    .show());
        root.addView(exportContext);

        Button exportFrequentWords=new Button(this);
        exportFrequentWords.setAllCaps(false);
        exportFrequentWords.setText("Export all learning + frequent words (V3)");
        exportFrequentWords.setOnClickListener(v->
                new android.app.AlertDialog.Builder(this)
                .setTitle("Export frequent-word learning?")
                .setMessage("This user-selected plaintext V3 file saves "+
                        "the V1 dictionary, V2 context patterns, and individual "+
                        "word use counts. It may reveal your habits. Older "+
                        "Orivra versions cannot read V3. Keep a V1 backup "+
                        "separately for rollback.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Choose location",(dialog,which)->
                        startActivityForResult(
                                LearningTransferDocumentIO.createFrequencyIntent(),
                                REQUEST_EXPORT_FREQUENCY))
                .show());
        root.addView(exportFrequentWords);

        Button importLearning=new Button(this);
        importLearning.setAllCaps(false);
        importLearning.setText("Import previous Orivra learning backup");
        importLearning.setOnClickListener(v->startActivityForResult(
                LearningTransferDocumentIO.openIntent(),REQUEST_IMPORT_LEARNING));
        root.addView(importLearning);

        // This entire panel is invisible to stable-preview and other packages.
        // Opt-in lives only in process memory; no SharedPreferences or export.
        if(CandidateLatencyMetrics.isCandidate(getPackageName())) {
            TextView heading=new TextView(this);
            heading.setText("\nCandidate-only performance lab (optional)");
            heading.setTextSize(17);
            heading.setTextColor(0xFF303846);
            root.addView(heading);

            TextView explanation=new TextView(this);
            explanation.setText("Turn on, type in the test box below, then tap Refresh. "
                    +"Only elapsed times are collected in memory. No typed text, "
                    +"predictions, or screen content is recorded. Disabling clears results.");
            explanation.setTextColor(0xFF455263);
            root.addView(explanation);

            Button latencyToggle=new Button(this);
            latencyToggle.setAllCaps(false);
            latencyToggle.setText("Latency recording: "
                    +(CandidateLatencyMetrics.enabled(getPackageName())?"ON":"OFF"));
            root.addView(latencyToggle);

            TextView latencySummary=new TextView(this);
            latencySummary.setTextColor(0xFF303846);
            latencySummary.setTextIsSelectable(true);
            latencySummary.setText(CandidateLatencyMetrics.summary(getPackageName()));
            root.addView(latencySummary);

            latencyToggle.setOnClickListener(v->{
                boolean next=!CandidateLatencyMetrics.enabled(getPackageName());
                CandidateLatencyMetrics.setEnabled(getPackageName(),next);
                latencyToggle.setText("Latency recording: "+(next?"ON":"OFF"));
                latencySummary.setText(CandidateLatencyMetrics.summary(getPackageName()));
            });
            Button refreshLatency=new Button(this);
            refreshLatency.setAllCaps(false);
            refreshLatency.setText("Refresh numeric latency report");
            refreshLatency.setOnClickListener(v->
                    latencySummary.setText(CandidateLatencyMetrics.summary(getPackageName())));
            root.addView(refreshLatency);

            Button clearLatency=new Button(this);
            clearLatency.setAllCaps(false);
            clearLatency.setText("Clear timing samples");
            clearLatency.setOnClickListener(v->{
                CandidateLatencyMetrics.clear(getPackageName());
                latencySummary.setText(CandidateLatencyMetrics.summary(getPackageName()));
            });
            root.addView(clearLatency);
        }

        TextView cursorHelp=new TextView(this);
        cursorHelp.setText("Cursor control: slide left or right across the spacebar to move through text. Tap the spacebar to insert a normal space. You can also hold, then slide. Try editing an earlier word in the box below.");
        cursorHelp.setTextSize(15);
        cursorHelp.setTextColor(0xFF303846);
        cursorHelp.setPadding(dp(4),dp(12),dp(4),dp(12));
        root.addView(cursorHelp);

        EditText test = new EditText(this);
        test.setSingleLine(false);
        test.setMinLines(4);
        test.setGravity(Gravity.TOP);
        test.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        test.setHint("7. Test here: thisiscookbutitslaking, thsi, hel, or howareyoudoingtoday");
        root.addView(test, new LinearLayout.LayoutParams(-1, dp(150)));
        if(CandidateLatencyMetrics.isCandidate(getPackageName())) {
            TextView previewTitle=new TextView(this);
            previewTitle.setText("\nFull-sentence inspection (optional)");
            previewTitle.setTextSize(17);
            previewTitle.setTextColor(0xFF303846);
            root.addView(previewTitle);

            TextView previewHelp=new TextView(this);
            previewHelp.setText("Type a non-sensitive, no-space phrase above, "
                    +"optionally enter your intended sentence, then tap Inspect. Full predictions and exact-match ranks appear below "
                    +"in model order. Inspect does not accept or insert text.");
            previewHelp.setTextColor(0xFF455263);
            root.addView(previewHelp);

            EditText expected=new EditText(this);
            expected.setSingleLine(false);
            expected.setMinLines(2);
            expected.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            expected.setHint("Optional intended sentence (spaces allowed; no sensitive text)");
            root.addView(expected,new LinearLayout.LayoutParams(-1,dp(90)));

            TextView previewOutput=new TextView(this);
            previewOutput.setTextColor(0xFF303846);
            previewOutput.setTextSize(15);
            previewOutput.setPadding(dp(10),dp(12),dp(10),dp(12));
            previewOutput.setTextIsSelectable(true);
            previewOutput.setText("No prediction inspected yet.");
            Button inspect=new Button(this);
            inspect.setAllCaps(false);
            inspect.setText("Inspect complete prediction candidates");
            inspect.setOnClickListener(v->{
                final String typed=test.getText().toString().trim();
                final String intended=expected.getText().toString().trim();
                if(!CandidatePreviewReport.supported(typed)
                        || !CandidatePreviewReport.supportedExpected(intended)) {
                    previewDispatcher.cancel();
                    previewOutput.setText(CandidatePreviewReport.supported(typed)
                            ? "Expected phrase must use 1–100 letters, spaces, or apostrophes."
                            : CandidatePreviewReport.instructions());
                    return;
                }
                previewOutput.setText("Analyzing locally…");
                previewDispatcher.schedule(typed,(id,input,suggestions)->
                        runOnUiThread(()->{
                            if(!isFinishing() && !isDestroyed() &&
                                    previewDispatcher.isCurrent(id) &&
                                    input.equals(test.getText().toString().trim())
                                    && intended.equals(expected.getText().toString().trim()))
                                previewOutput.setText(
                                        CandidatePreviewReport.format(input,suggestions,intended));
                        }));
            });
            root.addView(inspect);
            root.addView(previewOutput,new LinearLayout.LayoutParams(-1,-2));

            TextView oneTapHeading=new TextView(this);
            oneTapHeading.setText("\nOne-tap insertion check (Candidate only)");
            oneTapHeading.setTextSize(17);
            oneTapHeading.setTextColor(0xFF303846);
            root.addView(oneTapHeading);

            TextView oneTapHelp=new TextView(this);
            oneTapHelp.setText("In the test box above, TYPE a no-space example using "
                    +"Orivra Candidate (do not paste). Tap the first full-sentence "
                    +"suggestion once. Enter the intended sentence above, then "
                    +"press Check. Only the final text is compared locally; this "
                    +"button cannot measure how many times you tapped.");
            oneTapHelp.setTextColor(0xFF455263);
            root.addView(oneTapHelp);

            TextView oneTapStatus=new TextView(this);
            oneTapStatus.setText("Not checked yet.");
            oneTapStatus.setTextSize(15);
            oneTapStatus.setTextColor(0xFF303846);
            oneTapStatus.setTextIsSelectable(true);
            oneTapStatus.setPadding(dp(10),dp(10),dp(10),dp(10));

            Button verifyOneTap=new Button(this);
            verifyOneTap.setAllCaps(false);
            verifyOneTap.setText("Check actual one-tap insertion");
            verifyOneTap.setOnClickListener(v->{
                SentenceTapOutcome.Result outcome=SentenceTapOutcome.compare(
                        test.getText().toString(),expected.getText().toString());
                oneTapStatus.setText(SentenceTapOutcome.description(outcome)
                        +"\nText comparison only. The tap count is not recorded.");
            });
            root.addView(verifyOneTap);
            root.addView(oneTapStatus);

            expected.addTextChangedListener(new android.text.TextWatcher(){
                @Override public void beforeTextChanged(CharSequence s,int start,
                        int count,int after){}
                @Override public void onTextChanged(CharSequence s,int start,
                        int before,int count) {
                    previewDispatcher.cancel();
                    previewOutput.setText("Intended sentence changed. Tap Inspect again.");
                    oneTapStatus.setText("Not checked yet.");
                }
                @Override public void afterTextChanged(android.text.Editable s){}
            });

            test.addTextChangedListener(new android.text.TextWatcher(){
                @Override public void beforeTextChanged(CharSequence s,int start,
                        int count,int after){}
                @Override public void onTextChanged(CharSequence s,int start,
                        int before,int count) {
                    previewDispatcher.cancel();
                    previewOutput.setText("Text changed. Tap Inspect to view new candidates.");
                    oneTapStatus.setText("Not checked yet.");
                }
                @Override public void afterTextChanged(android.text.Editable s){}
            });
        }

        TextView privacy = new TextView(this);
        privacy.setText("Offline, no network permission. Test only non-sensitive phrases.");
        privacy.setTextColor(0xFF455263);
        root.addView(privacy);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
    }
    /** Strict user-selected URI access. No broad storage permissions. */
    @Override @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode,int resultCode,Intent result){
        super.onActivityResult(requestCode,resultCode,result);
        if(resultCode!=RESULT_OK || result==null || result.getData()==null)
            return;
        final Uri selected=result.getData();
        if(requestCode==REQUEST_EXPORT_LEARNING ||
                requestCode==REQUEST_EXPORT_CONTEXT ||
                requestCode==REQUEST_EXPORT_FREQUENCY ||
                requestCode==REQUEST_EXPORT_UNIFIED_V4){
            final boolean portable=requestCode==REQUEST_EXPORT_CONTEXT;
            final boolean includeFrequencies=requestCode==REQUEST_EXPORT_FREQUENCY;
            final boolean unifiedV4=requestCode==REQUEST_EXPORT_UNIFIED_V4;
            learningTransferIo.execute(()->{
                boolean saved=false;
                try{
                    String content=unifiedV4?
                            TypedLearningV4Transfer347.exportV4(this):
                            includeFrequencies?LearningTransferStore.exportV3(this):
                            portable?LearningTransferStore.exportV2(this):
                            LearningTransferStore.exportText(this);
                    LearningTransferDocumentIO.write(this,selected,content);
                    saved=true;
                }catch(Exception invalidOrWriteFailed){
                    // No raw words, document path or correction values are logged.
                }
                final boolean success=saved;
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    android.widget.Toast.makeText(this,
                            success?"Learning backup saved to selected document.":
                                    "Backup could not be saved.",
                            android.widget.Toast.LENGTH_LONG).show();
                });
            });
        }else if(requestCode==REQUEST_IMPORT_LEARNING){
            learningTransferIo.execute(()->{
                final FrequencyBackupFormat330.Data parsed;
                try{
                    String file=LearningTransferDocumentIO.read(this,selected);
                    parsed=FrequencyBackupFormat330.decode(file);
                }catch(Exception malformed){
                    runOnUiThread(()->{
                        if(isFinishing()||isDestroyed())return;
                        android.widget.Toast.makeText(this,
                                "Invalid, damaged or oversized Orivra learning backup.",
                                android.widget.Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    final LearningTransferStore.Preview preview;
                    try{preview=LearningTransferStore.previewV3(this,parsed);}
                    catch(IllegalArgumentException atCapacity){
                        android.widget.Toast.makeText(this,
                                "Not enough space in personal learning for this backup.",
                                android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("Review local learning import")
                        .setMessage("Validated backup: "+
                                parsed.earlier.legacy.words.size()+" saved words, "+
                                parsed.earlier.legacy.learnedPairs.size()+" correction pairs, "+
                                parsed.earlier.contexts.size()+" context patterns and "+
                                parsed.frequencies.size()+" learned word frequencies.\n"+
                                "Will add "+preview.newWords+" words, "+preview.newPairs+
                                " corrections and strengthen "+preview.strongerPairs+
                                " previous correction counts. Context patterns merge "+
                                "only when backed by approved words. Importing again "+
                                "does not duplicate "+
                                "or inflate entries. Learning stays in its current ON/OFF "+
                                "state. Proceed only with a backup you recognize.")
                        .setNegativeButton("Cancel",null)
                        .setPositiveButton("Import locally",(dialog,which)->
                                learningTransferIo.execute(()->{
                                    boolean completed=false;
                                    try{
                                        LearningTransferStore.importV3Confirmed(this,parsed);
                                        completed=true;
                                    }catch(Exception rejected){
                                        // No raw learned words or selected URI logged.
                                    }
                                    final boolean success=completed;
                                    runOnUiThread(()->{
                                        if(isFinishing()||isDestroyed())return;
                                        if(learningStatsView!=null)
                                            learningStatsView.setText(
                                                    PersonalLearning.diagnosticSummary(this));
                                        if(dictionaryInfoView!=null)
                                            dictionaryInfoView.setText(
                                                    PersonalDictionary.summary(this));
                                        android.widget.Toast.makeText(this,
                                                success?"Learning imported locally.":
                                                        "Learning import could not be completed.",
                                                android.widget.Toast.LENGTH_LONG).show();
                                    });
                                }))
                        .show();
                });
            });
        }else if(requestCode==REQUEST_IMPORT_UNIFIED_V4) {
            learningTransferIo.execute(()->{
                final TypedLearningV4Codec341.Data parsed;
                final TypedLearningV4Transfer347.Preview preview;
                try{
                    String file=LearningTransferDocumentIO.read(this,selected);
                    parsed=TypedLearningV4Codec341.decode(file);
                    preview=TypedLearningV4Transfer347.preview(this,parsed);
                }catch(Exception malformedOrFull){
                    runOnUiThread(()->{
                        if(isFinishing()||isDestroyed())return;
                        android.widget.Toast.makeText(this,
                                "Backup invalid, damaged, incompatible or over capacity.",
                                android.widget.Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("Review one-file local learning import")
                        .setMessage("Validated file: "+
                            parsed.previous.earlier.legacy.words.size()
                            +" dictionary words, "+
                            parsed.previous.earlier.legacy.learnedPairs.size()
                            +" corrections, "+
                            parsed.previous.earlier.contexts.size()
                            +" context patterns, "+
                            parsed.previous.frequencies.size()
                            +" frequency entries, "+
                            preview.savedTypedWords+" typed words and "+
                            preview.savedTypedPairs+" typed word pairs.\n"
                            +"New typed records: "+preview.newTypedWords+" words, "+
                            preview.newTypedPairs+" pairs; stronger counts: "+
                            preview.strongerWords+" words, "+
                            preview.strongerPairs+" pairs. Counts merge by MAX "
                            +"to avoid duplicate learning when reimported. "
                            +"This import will not enable typed-word memory "
                            +"or other Learning preferences. Reopen the "
                            +"keyboard afterward. Only import files you trust.")
                        .setNegativeButton("Cancel",null)
                        .setPositiveButton("Import approved backup",
                                (dialog,which)->learningTransferIo.execute(()->{
                            boolean completed=false;
                            try{
                                TypedLearningV4Transfer347.importConfirmed(
                                        this,parsed);
                                completed=true;
                            }catch(Exception storageIssue){
                                // A rare filesystem failure could partially
                                // import across legacy V1/V2/V3 preferences.
                                // No raw personal words or URIs are logged.
                            }
                            final boolean success=completed;
                            runOnUiThread(()->{
                                if(isFinishing()||isDestroyed())return;
                                if(learningStatsView!=null)
                                    learningStatsView.setText(
                                            PersonalLearning.diagnosticSummary(this));
                                if(dictionaryInfoView!=null)
                                    dictionaryInfoView.setText(
                                            PersonalDictionary.summary(this));
                                android.widget.Toast.makeText(this,success?
                                    "V4 learning imported. Reopen keyboard.":
                                    "Import not completed. Review data before retrying.",
                                    android.widget.Toast.LENGTH_LONG).show();
                            });
                        }))
                        .show();
                });
            });
        }
    }

    @Override protected void onStop() {
        previewDispatcher.cancel();
        super.onStop();
    }
    @Override protected void onDestroy() {
        previewDispatcher.close();
        learningTransferIo.shutdownNow();
        super.onDestroy();
    }
}
