package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

/**
 * Small, in-memory, best-effort app icon hue cache.
 *
 * Crucial difference from the old AppPalette.iconHue(): PackageManager,
 * drawable rasterization, pixel extraction and hue analysis ALL happen off
 * the IME/UI thread. Only a nullable float is cached for a bounded time.
 * A failed/hidden launcher icon falls back to an existing Material palette.
 *
 * No screen capture, AccessibilityService, QUERY_ALL_PACKAGES, network,
 * preferences, editor contents, package enumeration or icon bitmap storage.
 */
final class AppIconHueCache366 {
    private AppIconHueCache366() {}
    private static final int MAX=40,MAX_IN_FLIGHT=2,ICON_PX=64;
    private static final long SUCCESS_TTL_MS=6*60*60*1000L;
    private static final long FAILURE_TTL_MS=5*60*1000L;
    private static final Object LOCK=new Object();
    private static final class HueEntry {
        final Float hue;
        final long expiry;
        HueEntry(Float value,long expire){hue=value;expiry=expire;}
    }
    private static final LinkedHashMap<String,HueEntry> CACHE=
            new LinkedHashMap<String,HueEntry>(MAX+1,.75f,true) {
                @Override protected boolean removeEldestEntry(
                        Map.Entry<String,HueEntry> eldest) {
                    return size()>MAX;
                }
            };
    private static final HashMap<String,Boolean> IN_FLIGHT=new HashMap<>();
    // One current icon and ONE pending latest app. Never build a backlog
    // of old PackageManager icon jobs when repeatedly switching apps.
    private static final ThreadPoolExecutor WORKER=new ThreadPoolExecutor(
            1,1,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),
            runnable->{
                Thread t=new Thread(runnable,"Orivra-App-Color");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY+1);
                return t;
            });
    private static final Handler MAIN=new Handler(Looper.getMainLooper());

    private static final class HueTask implements Runnable {
        final Context app;
        final String pkg;
        final Runnable onReady;
        HueTask(Context context,String name,Runnable callback){
            app=context;pkg=name;onReady=callback;
        }
        @Override public void run(){
            Float hue=null;
            try {hue=iconHue(app,pkg);}
            catch(Throwable unavailable){
                // Launcher assets are optional; avoid logging app names.
                hue=null;
            }
            final Float result=hue;
            synchronized(LOCK){
                long ttl=result!=null?SUCCESS_TTL_MS:FAILURE_TTL_MS;
                CACHE.put(pkg,new HueEntry(result,
                        SystemClock.elapsedRealtime()+ttl));
                IN_FLIGHT.remove(pkg);
            }
            if(onReady!=null)MAIN.post(onReady);
        }
    }

    private static boolean valid(String pkg) {
        if(pkg==null || pkg.length()<3 || pkg.length()>160)return false;
        for(int i=0;i<pkg.length();i++) {
            char ch=pkg.charAt(i);
            if(!(ch=='.'||ch=='_'||ch=='-'||
                    (ch>='a'&&ch<='z')||(ch>='A'&&ch<='Z')||
                    (ch>='0'&&ch<='9')))return false;
        }
        return pkg.indexOf('.')>0;
    }

    /** Fast, safe, UI-thread cached read; never touches PackageManager. */
    static Float cachedHue(String pkg) {
        if(!valid(pkg))return null;
        synchronized(LOCK) {
            HueEntry entry=CACHE.get(pkg);
            if(entry==null)return null;
            if(SystemClock.elapsedRealtime()>=entry.expiry) {
                CACHE.remove(pkg);
                return null;
            }
            return entry.hue;
        }
    }

    /** Drop optional cached icon-color metadata when Android trims the
     * hidden keyboard process; no user content, bitmaps or preferences.
     * Already-running tasks finish and remove their in-flight markers.
     */
    static void trimMemory() {
        synchronized(LOCK) {
            CACHE.clear();
            // No need to retain a queued old app's icon when the keyboard
            // has already become hidden; the active worker finishes safely.
            Runnable queued=WORKER.getQueue().poll();
            if(queued instanceof HueTask)
                IN_FLIGHT.remove(((HueTask)queued).pkg);
        }
    }
    static int cacheSizeForTest() {
        synchronized(LOCK) { return CACHE.size(); }
    }
    static int inFlightForTest() {
        synchronized(LOCK) { return IN_FLIGHT.size(); }
    }
    /** Test-only read: metadata bit, never an icon or package enumeration. */
    static boolean cacheContainsForTest(String pkg) {
        if(!valid(pkg))return false;
        synchronized(LOCK) {
            HueEntry entry=CACHE.get(pkg);
            return entry!=null &&
                    SystemClock.elapsedRealtime()<entry.expiry;
        }
    }

    /** At most one icon decode per package at a time; one completion callback.
     * Other concurrent setColors calls simply benefit from the cached result.
     */
    static void request(Context context,String pkg,Runnable onReady) {
        if(!valid(pkg)||context==null)return;
        final Context app=context.getApplicationContext()!=null?
                context.getApplicationContext():context;
        // Atomically replace ONLY the queued stale icon request. The
        // running lookup cannot be preempted, but a new notification/app
        // no longer waits behind multiple obsolete icons.
        synchronized(LOCK) {
            HueEntry entry=CACHE.get(pkg);
            if(entry!=null && SystemClock.elapsedRealtime()<entry.expiry)return;
            if(IN_FLIGHT.containsKey(pkg))return;
            Runnable stale=WORKER.getQueue().poll();
            if(stale instanceof HueTask)
                IN_FLIGHT.remove(((HueTask)stale).pkg);
            if(IN_FLIGHT.size()>=MAX_IN_FLIGHT)return;
            IN_FLIGHT.put(pkg,Boolean.TRUE);
            try {
                WORKER.execute(new HueTask(app,pkg,onReady));
            } catch(RejectedExecutionException disabledWorker) {
                IN_FLIGHT.remove(pkg);
            }
        }
    }

    private static Float iconHue(Context context,String pkg) {
        final Drawable icon;
        try {icon=context.getPackageManager().getApplicationIcon(pkg);}
        catch(PackageManager.NameNotFoundException|RuntimeException missing) {
            return null;
        }
        if(icon==null)return null;
        Bitmap bitmap=null;
        try {
            bitmap=Bitmap.createBitmap(ICON_PX,ICON_PX,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);
            Drawable art=icon.mutate();
            art.setBounds(0,0,ICON_PX,ICON_PX);
            art.draw(canvas);
            int[] pixels=new int[ICON_PX*ICON_PX];
            bitmap.getPixels(pixels,0,ICON_PX,0,0,ICON_PX,ICON_PX);
            AppIconColorSignature366.Match signature=
                    AppIconColorSignature366.detect(
                            pixels,ICON_PX,ICON_PX);
            return signature==null?null:signature.hue;
        } finally {
            if(bitmap!=null && !bitmap.isRecycled())bitmap.recycle();
        }
    }
}
