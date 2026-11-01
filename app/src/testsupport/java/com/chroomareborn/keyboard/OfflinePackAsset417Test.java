package com.chroomareborn.keyboard;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

/** Runs locally on the generated archive; never reads or stores user text. */
public final class OfflinePackAsset417Test {
    private static void verify(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception{
        verify(args.length==1,"expected one generated asset path");
        File asset=new File(args[0]);
        verify(asset.isFile() && asset.length()>200_000 &&
                asset.length()<500_000,"expected compressed ESDB archive");
        CompressedLanguagePack.Loaded loaded;
        try(FileInputStream stream=new FileInputStream(asset)){
            loaded=CompressedLanguagePack.readVerified(stream);
        }
        verify(loaded.pack.size()==78_942,"expected 78942 exact words");
        verify(loaded.uncompressedBytes==1_022_018,"expected source byte count");
        verify(loaded.pack.contains("keyboard"),"must recognize keyboard");
        verify(loaded.pack.contains("electric"),"must recognize electric");
        verify(loaded.pack.contains("tomorrow"),"must recognize tomorrow");
        verify(!loaded.pack.contains("this is not a word"),"no phrase word");
        verify(CompressedLanguagePack.tryRead(
                new ByteArrayInputStream(new byte[]{1,2,3,4}))==null,
                "corrupt archive fails closed");
        System.out.println("Build 417 VERIFIED offline archive: "+
                loaded.pack.size()+" words, "+
                loaded.uncompressedBytes+" original bytes, "+
                asset.length()+" compressed bytes, loaded in "+
                (loaded.nanos/1_000_000L)+" ms");
    }
}
