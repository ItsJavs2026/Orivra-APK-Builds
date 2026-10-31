package com.chroomareborn.keyboard;

/**
 * Offline, Android-free colour signature for a launcher's icon.
 *
 * This is NOT colour sampling of another app's UI or a screenshot. It uses
 * only pixels of the icon already returned by Android PackageManager and
 * fails closed for monochrome, tiny-accent, or ambiguous multi-hue icons.
 * No editor text, package identifier, image or pixel data is persisted.
 */
final class AppIconColorSignature366 {
    private AppIconColorSignature366() {}
    private static final int BINS=36; // 10° bins
    static final class Match {
        final float hue;
        final float concentration;
        final float chromaticCoverage;
        Match(float h,float strength,float coverage) {
            hue=h;
            concentration=strength;
            chromaticCoverage=coverage;
        }
    }

    static Match detect(int[] pixels,int width,int height) {
        if(pixels==null || width<12 || height<12 ||
                width>256 || height>256 ||
                pixels.length!=(long)width*height)return null;
        float[] weights=new float[BINS], hueSin=new float[BINS],
                hueCos=new float[BINS];
        int opaque=0, chromatic=0;
        float total=0;
        int stride=Math.max(1,Math.min(width,height)/40);
        for(int y=stride/2;y<height;y+=stride) {
            for(int x=stride/2;x<width;x+=stride) {
                int argb=pixels[y*width+x];
                int alpha=(argb>>>24)&255;
                if(alpha<185)continue;
                opaque++;
                float r=((argb>>>16)&255)/255f;
                float g=((argb>>>8)&255)/255f;
                float b=(argb&255)/255f;
                float max=Math.max(r,Math.max(g,b));
                float min=Math.min(r,Math.min(g,b));
                float delta=max-min;
                float saturation=max==0?0:delta/max;
                if(saturation<.23f || max<.18f || max>.995f ||
                        delta<.09f)continue;
                chromatic++;
                float hue;
                if(max==r)hue=60f*((g-b)/delta);
                else if(max==g)hue=60f*(2f+(b-r)/delta);
                else hue=60f*(4f+(r-g)/delta);
                if(hue<0)hue+=360f;
                if(hue>=360)hue-=360f;
                int bin=Math.min(BINS-1,(int)(hue/10f));
                // Slight preference for genuine colorful areas and the
                // central graphic, not tiny high-saturation edge antialias.
                float dx=(x+.5f-width/2f)/width;
                float dy=(y+.5f-height/2f)/height;
                float central=1.15f-.45f*Math.min(1f,4f*(dx*dx+dy*dy));
                float weight=central*(.45f+.55f*saturation)*
                        (.35f+.65f*max)*(alpha/255f);
                weights[bin]+=weight;
                double radians=Math.toRadians(hue);
                hueSin[bin]+=(float)Math.sin(radians)*weight;
                hueCos[bin]+=(float)Math.cos(radians)*weight;
                total+=weight;
            }
        }
        if(opaque<36 || chromatic<12 ||
                chromatic/(float)opaque<.045f || total<3.5f)
            return null;
        int best=0;
        float maxCluster=0;
        for(int i=0;i<BINS;i++) {
            float cluster=weights[(i+BINS-1)%BINS]+weights[i]+
                    weights[(i+1)%BINS];
            if(cluster>maxCluster){best=i;maxCluster=cluster;}
        }
        float concentration=maxCluster/total;
        // An icon with equally prominent unrelated hues should not be
        // presented as a confident single app colour.
        if(concentration<.53f)return null;
        float sin=0,cos=0;
        for(int off=-1;off<=1;off++) {
            int idx=(best+off+BINS)%BINS;
            sin+=hueSin[idx];
            cos+=hueCos[idx];
        }
        if(Math.abs(sin)+Math.abs(cos)<.001f)return null;
        float result=(float)Math.toDegrees(Math.atan2(sin,cos));
        if(result<0)result+=360f;
        return new Match(result,concentration,chromatic/(float)opaque);
    }
}
