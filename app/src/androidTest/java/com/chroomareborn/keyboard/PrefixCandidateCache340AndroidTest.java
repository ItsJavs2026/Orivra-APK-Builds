package com.chroomareborn.keyboard;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Repeated render paths must not repeat preference scans for one prefix. */
@RunWith(AndroidJUnit4.class)
public final class PrefixCandidateCache340AndroidTest {
    @Test public void repeatedPrefixQueriesCallLoaderOnceUntilChanged(){
        PrefixCandidateCache340 cache=new PrefixCandidateCache340();
        AtomicInteger reads=new AtomicInteger();
        PrefixCandidateCache340.Lookup load=p->{
            reads.incrementAndGet();
            return List.of(p+"tinue");
        };
        assertEquals(List.of("continue"),cache.get("con",load));
        assertEquals(List.of("continue"),cache.get("con",load));
        assertEquals(1,reads.get());
        cache.get("cont",load);
        assertEquals(2,reads.get());
        cache.clear();
        cache.get("cont",load);
        assertEquals(3,reads.get());
    }
    @Test public void cachingDoesNotExposeMutableResultList(){
        PrefixCandidateCache340 cache=new PrefixCandidateCache340();
        ArrayList<String> original=new ArrayList<>(List.of("continue"));
        List<String> cached=cache.get("con",p->original);
        original.add("contact");
        assertEquals(List.of("continue"),cached);
        try {
            cached.add("unwanted");
            fail("Cache result must be immutable");
        } catch(UnsupportedOperationException expected) {
            assertEquals(1,cached.size());
        }
    }
}
