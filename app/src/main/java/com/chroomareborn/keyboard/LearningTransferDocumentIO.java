package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Android Storage Access Framework only. A file is read/written only after
 * the user explicitly picks its URI. No storage permission, broad scanning,
 * network, clipboard, logs or automatic backup behavior.
 */
final class LearningTransferDocumentIO {
    private LearningTransferDocumentIO() {}
    static Intent createIntent(){
        // Existing V1 filename is frozen for rollbacks and older Orivra.
        return createNamedIntent("orivra-local-learning-backup.txt");
    }
    static Intent createContextIntent(){
        // V2 must not accidentally overwrite a previously exported V1 file.
        return createNamedIntent("orivra-context-learning-v2-backup.txt");
    }
    static Intent createFrequencyIntent(){
        return createNamedIntent("orivra-word-frequency-v3-backup.txt");
    }
    private static Intent createNamedIntent(String filename){
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE,filename);
        return intent;
    }
    static Intent openIntent(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        return intent;
    }
    static String read(Context context,Uri selected)throws IOException{
        if(context==null||selected==null)
            throw new IOException("No document selected");
        ByteArrayOutputStream buffered=new ByteArrayOutputStream();
        try(InputStream input=context.getContentResolver().openInputStream(selected)){
            if(input==null)throw new IOException("Could not open document");
            byte[] chunk=new byte[4096];
            int count;
            while((count=input.read(chunk))!=-1){
                if(count==0)continue;
                if(buffered.size()+count>LearningTransferCodec.MAX_UTF8_BYTES)
                    throw new IOException("Document exceeds safe import size");
                buffered.write(chunk,0,count);
            }
        }
        try{
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(buffered.toByteArray())).toString();
        }catch(java.nio.charset.CharacterCodingException invalid){
            throw new IOException("Invalid UTF-8 document",invalid);
        }
    }
    static void write(Context context,Uri selected,String body)throws IOException{
        if(context==null||selected==null||body==null)
            throw new IOException("No backup document selected");
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        if(bytes.length>LearningTransferCodec.MAX_UTF8_BYTES)
            throw new IOException("Backup exceeds safe file size");
        try(OutputStream output=context.getContentResolver().openOutputStream(
                selected,"w")){
            if(output==null)throw new IOException("Could not create document");
            output.write(bytes);
            output.flush();
        }
    }
}
