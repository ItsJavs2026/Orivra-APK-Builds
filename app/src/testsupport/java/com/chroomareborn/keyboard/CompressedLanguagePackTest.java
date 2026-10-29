package com.chroomareborn.keyboard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

/** Pure-Java defensive tests; no Android and no real user text. */
public final class CompressedLanguagePackTest {
    private static int checks;
    private static void verify(boolean truth,String description){
        checks++;if(!truth)throw new AssertionError(description);
    }
    private static byte[] gzip(byte[] uncompressed)throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(GZIPOutputStream stream=new GZIPOutputStream(out)){stream.write(uncompressed);}
        return out.toByteArray();
    }
    public static void main(String[] args)throws Exception {
        verify(CompressedLanguagePack.tryRead(null)==null,
                "missing wordpack must not break base typing");
        verify(CompressedLanguagePack.tryRead(
                new ByteArrayInputStream(new byte[]{1,2,3,4}))==null,
                "non-gzip bytes must fail quietly");
        byte[] tiny=gzip("sunrise\nheadphones\n".getBytes(StandardCharsets.UTF_8));
        verify(CompressedLanguagePack.tryRead(new ByteArrayInputStream(tiny))==null,
                "small unverified dictionary must not install");
        byte[] corrupt=tiny.clone();corrupt[corrupt.length-5]^=0x01;
        verify(CompressedLanguagePack.tryRead(new ByteArrayInputStream(corrupt))==null,
                "altered gzip must not install");
        byte[] expanded=new byte[CompressedLanguagePack.MAX_UNCOMPRESSED_BYTES+4096];
        Arrays.fill(expanded,(byte)'a');
        verify(CompressedLanguagePack.tryRead(new ByteArrayInputStream(gzip(expanded)))==null,
                "decompression bomb must be rejected before heap growth");
        verify(CompressedLanguagePack.ASSET_PATH.endsWith(".gz"),
                "compressed asset must be named explicitly");
        verify(CompressedLanguagePack.EXPECTED_SOURCE_SHA256.length()==64,
                "full SHA-256 integrity source pin");
        System.out.println("PASS: CompressedLanguagePackTest "+checks+" assertions");
    }
}
