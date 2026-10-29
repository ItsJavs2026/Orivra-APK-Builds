package com.chroomareborn.keyboard;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

/**
 * Build 302 research-only, verified offline vocabulary loader.
 *
 * The optional APK asset is generated from a pinned, properly attributed
 * ESDB/SCOWL-v2 size-60 US spelling wordlist in CI. Never use this source
 * as word frequency, to speculate typos, or to modify editor text.
 *
 * No file / network / keyboard/editor access. May run ONLY off the IME UI thread.
 * Missing/corrupted pack must leave the original Candidate 300 engine intact.
 */
final class CompressedLanguagePack {
    static final String ASSET_PATH="dictionaries/esdb-en-US-60.txt.gz";
    static final String EXPECTED_SOURCE_SHA256=
            "a5d2f75b95d3530485e539352dc01f9dd9079e03fdaca37466292bf1a360c600";
    static final int MAX_UNCOMPRESSED_BYTES=1_350_000;
    static final int EXPECTED_WORDS=78_942;

    private CompressedLanguagePack() {}

    static final class Loaded {
        final ExactOnlyLanguagePack pack;
        final long nanos;
        final int uncompressedBytes;
        Loaded(ExactOnlyLanguagePack words,long duration,int bytes){
            pack=words;nanos=duration;uncompressedBytes=bytes;
        }
    }

    /** Rejects a decompression bomb BEFORE the reader can allocate arbitrarily. */
    private static final class LimitedBytes extends FilterInputStream {
        private int remaining,used;
        LimitedBytes(InputStream upstream,int max) {
            super(upstream);remaining=max;
        }
        @Override public int read()throws IOException {
            if(remaining==0) {
                // Check one last byte so exactly-max-sized streams remain valid.
                if(super.read()!=-1)throw new IOException("Offline pack is too large");
                return -1;
            }
            int value=super.read();
            if(value!=-1){remaining--;used++;}
            return value;
        }
        @Override public int read(byte[] buffer,int off,int len)throws IOException {
            if(len==0)return 0;
            if(remaining==0) {
                int value=super.read();
                if(value!=-1)throw new IOException("Offline pack is too large");
                return -1;
            }
            int count=super.read(buffer,off,Math.min(len,remaining));
            if(count>0){remaining-=count;used+=count;}
            return count;
        }
        int byteCount(){return used;}
    }

    static Loaded readVerified(InputStream compressed)throws IOException{
        if(compressed==null)throw new IOException("Missing offline wordpack asset");
        long start=System.nanoTime();
        final MessageDigest digest;
        try{digest=MessageDigest.getInstance("SHA-256");}
        catch(NoSuchAlgorithmException unsupported){
            throw new IOException("No SHA-256 algorithm",unsupported);
        }
        try(GZIPInputStream gz=new GZIPInputStream(compressed,8192);
            LimitedBytes bounded=new LimitedBytes(gz,MAX_UNCOMPRESSED_BYTES);
            DigestInputStream checking=new DigestInputStream(bounded,digest);
            InputStreamReader utf8=new InputStreamReader(checking,StandardCharsets.UTF_8);
            BufferedReader reader=new BufferedReader(utf8,8192)){
            ExactOnlyLanguagePack words=ExactOnlyLanguagePack.fromReader(reader);
            if(words.size()!=EXPECTED_WORDS)
                throw new IOException("Unexpected offline pack word count: "+words.size());
            byte[] actual=digest.digest();
            StringBuilder hex=new StringBuilder(64);
            for(byte b:actual) {
                hex.append(Character.forDigit((b>>>4)&15,16));
                hex.append(Character.forDigit(b&15,16));
            }
            if(!EXPECTED_SOURCE_SHA256.equals(hex.toString()))
                throw new IOException("Offline pack checksum mismatch");
            return new Loaded(words,System.nanoTime()-start,bounded.byteCount());
        }
    }
    /** Quiet opt-out for a malformed or absent optional asset. No typed data. */
    static Loaded tryRead(InputStream compressed) {
        try{return readVerified(compressed);}
        catch(IOException|RuntimeException invalidPack){return null;}
    }
}
