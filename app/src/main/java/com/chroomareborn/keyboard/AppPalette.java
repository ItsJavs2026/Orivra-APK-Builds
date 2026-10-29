package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import java.util.Locale;

/**
 * Android Material-inspired keyboard colors. An IME cannot read other apps'
 * background pixels. App color is a best-effort icon/preset hue; dynamic colors
 * come from Android 12+ wallpaper palettes, respecting the system night mode.
 */
final class AppPalette {
    private AppPalette() {}
    static final String PREFS = "reborn_theme";
    static final String KEY_MODE = "mode";
    static final String KEY_FOLLOW_SYSTEM = "follow_system";
    static final String KEY_MANUAL_DARK = "manual_dark";

    // The previous experimental ICON=0 preference becomes the new default.
    static final int APP_SHADED = 0;
    static final int APP_MATCH = 1;
    static final int MATERIAL_AUTO = 2;
    static final int PURPLE = 3, TEAL = 4, ORANGE = 5, CLASSIC = 6;
    static final int CLEAN_NEUTRAL = 7; // Gboard-inspired neutral appearance, original Orivra colors.
    static final int MODE_COUNT = 8;
    static final int DEFAULT = APP_SHADED;

    static final class Result {
        final int background, keyBackground, accent;
        final int textColor, keyTextColor, secondaryTextColor, borderColor;
        final int specialBackground, suggestionBackground, suggestionTextColor, rippleColor;
        final boolean dark;
        final String label;
        Result(int bg, int key, int highlight, int text, int keyText, int subtle,
               int border, int special, int suggestion, int suggestionText,
               int ripple, boolean night, String source) {
            background=bg; keyBackground=key; accent=highlight;
            textColor=text; keyTextColor=keyText; secondaryTextColor=subtle;
            borderColor=border; specialBackground=special;
            suggestionBackground=suggestion; suggestionTextColor=suggestionText;
            rippleColor=ripple; dark=night; label=source;
        }
    }

    static int normalized(int mode) {
        return mode>=APP_SHADED && mode<MODE_COUNT ? mode : DEFAULT;
    }
    static int nextMode(int mode) { return (normalized(mode)+1)%MODE_COUNT; }
    static String modeName(int mode) {
        switch (normalized(mode)) {
            case APP_MATCH: return "App Match";
            case MATERIAL_AUTO: return "Material You";
            case PURPLE: return "Purple";
            case TEAL: return "Teal";
            case ORANGE: return "Orange";
            case CLASSIC: return "Classic";
            case CLEAN_NEUTRAL: return "Clean Neutral";
            default: return "App Match — Shaded";
        }
    }

    static boolean followsSystem(Context ctx) {
        return ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getBoolean(KEY_FOLLOW_SYSTEM,true);
    }
    static boolean isNight(Context ctx) {
        if(!followsSystem(ctx))
            return ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                    .getBoolean(KEY_MANUAL_DARK,true);
        return (ctx.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
    }
    static Result resolve(Context ctx,String packageName) {
        final boolean dark=isNight(ctx);
        int mode=normalized(ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                .getInt(KEY_MODE,DEFAULT));
        if(mode==MATERIAL_AUTO) return material(ctx,dark);
        if(mode==CLEAN_NEUTRAL) {
            // High-contrast, clean keycaps reminiscent of the familiar
            // neutral mobile-keyboard aesthetic. No third-party graphics.
            return dark ?
                    result(0xFF202124,0xFF383A40,0xFF4A4C52,0xFF8AB4F8,
                            true,"Clean Neutral • Dark"):
                    result(0xFFF1F3F4,0xFFFFFFFF,0xFFDDE1E6,0xFF1A73E8,
                            false,"Clean Neutral • Light");
        }

        Float hue=null;
        String source="";
        if(mode==PURPLE) { hue=273f;source="Purple"; }
        else if(mode==TEAL) { hue=174f;source="Teal"; }
        else if(mode==ORANGE) { hue=22f;source="Orange"; }
        else if(mode==CLASSIC) { hue=225f;source="Classic"; }
        else {
            hue=iconHue(ctx,packageName);
            if(hue!=null)source="app icon estimate";
            else {
                hue=presetHue(packageName);
                if(hue!=null)source="app preset estimate";
            }
            if(hue==null)return material(ctx,dark,modeName(mode)+" • Material fallback");
        }
        boolean shaded=mode!=APP_MATCH;
        return tones(hue,dark,shaded,modeName(mode)+" • "+source);
    }
    private static Float presetHue(String packageName) {
        String pkg=packageName==null?"":packageName.toLowerCase(Locale.ROOT);
        if(pkg.contains("whatsapp")||pkg.contains("spotify"))return 146f;
        if(pkg.contains("youtube"))return 0f;
        if(pkg.contains("telegram"))return 201f;
        if(pkg.contains("discord"))return 235f;
        if(pkg.contains("music"))return 155f;
        if(pkg.contains("reddit"))return 20f;
        return null;
    }
    private static int hsv(float hue,float sat,float value) {
        return Color.HSVToColor(new float[]{
            (hue%360+360)%360,Math.max(0,Math.min(1,sat)),
            Math.max(0,Math.min(1,value))});
    }
    private static int mix(int a,int b,float portionOfB) {
        float t=Math.max(0,Math.min(1,portionOfB));
        return Color.rgb(Math.round(Color.red(a)*(1-t)+Color.red(b)*t),
                Math.round(Color.green(a)*(1-t)+Color.green(b)*t),
                Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t));
    }
    private static int contrastText(int bg) {
        // Perceived luminance (sRGB approximation). Keep keycaps legible.
        double y=(0.2126*Color.red(bg)+0.7152*Color.green(bg)+
                0.0722*Color.blue(bg))/255.0;
        return y>.57?0xFF202229:Color.WHITE;
    }
    private static Result tones(float hue,boolean dark,boolean shaded,String label) {
        int bg,key,special,accent;
        if(dark) {
            bg=hsv(hue,shaded?.19f:.48f,shaded?.16f:.26f);
            key=hsv(hue,shaded?.12f:.30f,shaded?.30f:.43f);
            special=hsv(hue,.28f,.37f);
            accent=hsv(hue,.66f,.91f);
        } else {
            bg=hsv(hue,shaded?.075f:.23f,shaded?.96f:.93f);
            key=hsv(hue,shaded?.035f:.14f,1f);
            special=hsv(hue,.16f,.88f);
            accent=hsv(hue,.67f,.58f);
        }
        return result(bg,key,special,accent,dark,label);
    }
    private static Result material(Context ctx,boolean dark) {
        return material(ctx,dark,"Material You • system palette");
    }
    private static Result material(Context ctx,boolean dark,String label) {
        if(Build.VERSION.SDK_INT>=31) {
            try {
                int bg=ctx.getColor(dark?android.R.color.system_neutral1_900:
                        android.R.color.system_neutral1_50);
                int key=ctx.getColor(dark?android.R.color.system_neutral2_800:
                        android.R.color.system_neutral2_100);
                int special=ctx.getColor(dark?android.R.color.system_neutral2_700:
                        android.R.color.system_neutral2_200);
                int accent=ctx.getColor(dark?android.R.color.system_accent1_200:
                        android.R.color.system_accent1_600);
                return result(bg,key,special,accent,dark,label);
            } catch (RuntimeException unavailable) {
                // Some device variants omit dynamic resources.
            }
        }
        return tones(225f,dark,true,label+" • fallback");
    }
    private static Result result(int bg,int key,int special,int accent,
                                 boolean dark,String label) {
        int text=contrastText(bg),keyText=contrastText(key);
        int subtle=mix(text,bg,.34f);
        int border=mix(key,bg,dark?.35f:.12f);
        int suggestion=key;
        int ripple=(0x68<<24)|(accent & 0x00FFFFFF);
        return new Result(bg,key,accent,text,keyText,subtle,border,special,
                suggestion,contrastText(suggestion),ripple,dark,label);
    }
    private static Float iconHue(Context ctx,String pkg) {
        if(pkg==null||pkg.isEmpty())return null;
        final Drawable icon;
        try { icon=ctx.getPackageManager().getApplicationIcon(pkg); }
        catch (PackageManager.NameNotFoundException|RuntimeException e) { return null; }
        Bitmap bitmap=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        icon.setBounds(0,0,48,48);icon.draw(canvas);
        float[] weights=new float[24],hsv=new float[3];
        for(int y=1;y<47;y+=2)for(int x=1;x<47;x+=2) {
            int pixel=bitmap.getPixel(x,y);
            if(Color.alpha(pixel)<210)continue;
            Color.colorToHSV(pixel,hsv);
            if(hsv[1]<.30f||hsv[2]<.16f||hsv[2]>.96f)continue;
            int index=Math.min(23,(int)(hsv[0]/15f));
            weights[index]+=hsv[1]*hsv[1]*(0.3f+hsv[2]);
        }
        bitmap.recycle();
        int best=-1;float highest=0;
        for(int i=0;i<24;i++)if(weights[i]>highest) {
            best=i;highest=weights[i];
        }
        return best<0||highest<1f?null:(best+.5f)*15f;
    }
}
