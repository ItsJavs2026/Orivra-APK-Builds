package com.chroomareborn.keyboard;

public final class EdgeKeyWeights378Research {
    private static int assertions;
    private static void check(boolean ok,String label){
        assertions++;if(!ok)throw new AssertionError(label);
    }
    public static void main(String[] ignored) {
        int[] sizes={10,9,7};
        for(int row=0;row<3;row++){
            int n=sizes[row];
            float total=0f,edge=EdgeKeyWeights378.weight(row,0,n);
            check(edge>1f && edge<=1.10f,"edge boosted");
            for(int col=0;col<n;col++){
                float weight=EdgeKeyWeights378.weight(row,col,n);
                check(Float.isFinite(weight) && weight>0.95f && weight<=1.10f,"reasonable width");
                if(col>0 && col<n-1)check(weight<1f,"interior conserved");
                total+=weight;
            }
            check(Math.abs(total-n)<0.00005f,"total row width exactly preserved");
            check(EdgeKeyWeights378.weight(row,n-1,n)==edge,"left/right symmetric");
        }
        check(EdgeKeyWeights378.weight(-1,0,10)==1f,"invalid row normal");
        check(EdgeKeyWeights378.weight(0,11,10)==1f,"invalid col normal");
        check(EdgeKeyWeights378.weight(0,0,4)==1f,"nonalphabet guard");
        for(int i=0;i<50000;i++){
            int row=i%3,n=sizes[row];
            int col=(i/3)%n;
            float w=EdgeKeyWeights378.weight(row,col,n);
            check(Float.isFinite(w) && w>0f,"finite stress width");
        }
        System.out.println("PASS EdgeKeyWeights378 "+assertions+" synthetic assertions");
    }
}
