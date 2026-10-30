package com.chroomareborn.keyboard;

import android.content.Context;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/**
 * Android 35 emulator research. No editor access, no keyboard activation and
 * no user text. Validates the wordlist actually embedded in test APK.
 * Never interpret emulator p95 as Samsung on-device latency.
 */
@RunWith(AndroidJUnit4.class)
public final class OfflineLanguagePackAndroidTest {
    private static final String TAG="OrivraWordpack302";
    private static Context ctx(){
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
    private static CompressedLanguagePack.Loaded load()throws Exception {
        String pkg=ctx().getPackageName();
        String[] found=ctx().getAssets().list("dictionaries");
        Log.i(TAG,"targetPackage="+pkg+" assetList="+Arrays.toString(found));
        assertEquals("com.chroomareborn.keyboard.build361trial",pkg);
        try(InputStream stream=ctx().getAssets().open(CompressedLanguagePack.ASSET_PATH)){
            return CompressedLanguagePack.readVerified(stream);
        }
    }
    @Test public void officialPackIsInApkAndMatchesImmutableSourceHash()throws Exception{
        CompressedLanguagePack.Loaded data=load();
        assertNotNull(data);
        assertEquals(CompressedLanguagePack.EXPECTED_WORDS,data.pack.size());
        assertEquals(1022018,data.uncompressedBytes);
        for(String word:new String[]{"sunrise","headphones","astronomy",
                "charger","concert","bicycles","notebooks","midnight"})
            assertTrue("missing expected spelling "+word,data.pack.contains(word));
        assertFalse(data.pack.contains("notareallexicalwordz"));
        Log.i(TAG,"verified words="+data.pack.size()+
                " loadMs="+data.nanos/1_000_000);
    }
    @Test public void backgroundLoadFinishesOnMainThreadWithoutEditing()throws Exception {
        final CountDownLatch latch=new CountDownLatch(1);
        final AtomicReference<CompressedLanguagePack.Loaded> loaded=new AtomicReference<>();
        final AtomicBoolean mainThread=new AtomicBoolean(false);
        OfflineLanguagePackWorker coordinator=new OfflineLanguagePackWorker();
        try{
            assertTrue(coordinator.start(ctx(),pack->{
                loaded.set(pack);
                mainThread.set(Looper.myLooper()==Looper.getMainLooper());
                latch.countDown();
            }));
            assertFalse("only one load should be scheduled",
                    coordinator.start(ctx(),pack->{}));
            assertTrue("asset load must complete",latch.await(30,TimeUnit.SECONDS));
            assertTrue("callback must occur on the main thread",mainThread.get());
            assertNotNull("valid asset must have been loaded",loaded.get());
            assertEquals(78942,loaded.get().pack.size());
        }finally{
            coordinator.close();
        }
    }
    private static double p95(ArrayList<Long> times){
        times.sort(Long::compare);
        return times.get((int)Math.ceil(times.size()*.95)-1)/1_000_000.0;
    }
    private static double benchmark(String[] phrases,int reps){
        ArrayList<Long> samples=new ArrayList<>();
        for(int i=0;i<reps;i++){
            for(String text:phrases){
                long start=System.nanoTime();
                assertNotNull(PredictionLite.suggestions(text));
                samples.add(System.nanoTime()-start);
            }
        }
        return p95(samples);
    }
    @Test public void warmedPredictionLatencyAndCorrectionGuards()throws Exception{
        final String[] inputs={
            "hello","thetrainarrivedbeforesunrise",
            "yourheadphonessoundveryclear","thegardenneedsfreshwater",
            "thepaxkaxearrivedearliertoday",
            "iwanttogohometonight","ineedtochargemytablet",
            "ilovelearningaboutastronomy",
            "pleaseremembertolockthedoor",
            "therewillbeameetingtomorrow",
            "thefamilyenjoyedthemovie","myphonebatteryneedsattention"};
        PredictionLite.setResearchExactOnlyPack(null);
        // Warm all predictor static and hotspot code before measuring.
        benchmark(inputs,2);
        double baseline=benchmark(inputs,5);
        String preserved=PredictionLite.suggestions("thegardenneedsfreshwater").get(0);
        String typoPreserved=PredictionLite.suggestions(
                "thepaxkaxearrivedearliertoday").get(0);
        CompressedLanguagePack.Loaded data=load();
        try {
            PredictionLite.setResearchExactOnlyPack(data.pack);
            benchmark(inputs,2);
            double experimental=benchmark(inputs,5);
            assertEquals(preserved,
                    PredictionLite.suggestions("thegardenneedsfreshwater").get(0));
            assertEquals(typoPreserved,
                    PredictionLite.suggestions("thepaxkaxearrivedearliertoday").get(0));
            assertEquals("sunrise",PredictionLite.suggestions("sunrise").get(0));
            assertEquals("headphones",PredictionLite.suggestions("headphones").get(0));
            assertTrue("p95 research performance guard: "+experimental+
                    "ms baseline "+baseline+"ms",
                    experimental<=Math.max(250.0,baseline*2.5+25.0));
            Log.i(TAG,"ANDROID35 emulator warm n=60 baselineP95Ms="+
                    baseline+" packP95Ms="+experimental+
                    " sourceLoadMs="+data.nanos/1_000_000+
                    " (NOT Samsung S26 Ultra timing)");
        }finally{
            PredictionLite.setResearchExactOnlyPack(null);
        }
    }
}
