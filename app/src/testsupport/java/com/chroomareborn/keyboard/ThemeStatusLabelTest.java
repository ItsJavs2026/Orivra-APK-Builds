package com.chroomareborn.keyboard;

import java.util.Random;

public final class ThemeStatusLabelTest {
    private static int checks;
    private static void assertTrue(boolean pass,String reason) {
        checks++;
        if(!pass)throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        String[] modes={"App Match — Shaded","App Match","Material You",
                "Purple","Teal","Orange","Classic","Clean Neutral"};
        for(String name:modes) {
            for(boolean dark:new boolean[]{true,false}) {
                for(boolean system:new boolean[]{true,false}) {
                    String label=ThemeStatusLabel.compact(name,dark);
                    String chip=ThemeStatusLabel.expandedChip(name,dark);
                    assertTrue(chip.endsWith(dark?" D":" L"),
                            "expanded theme must show actual light/dark");
                    assertTrue(chip.length()<=10,
                            "expanded theme chip too wide");
                    assertTrue(!chip.startsWith("Unknown"),
                            "valid theme not identified");
                    String info=ThemeStatusLabel.details(name,dark,system,
                            name+" • app icon estimate");
                    assertTrue(label.startsWith("Theme: "+name+" • "),
                            "selected mode not visible");
                    assertTrue(label.endsWith(dark?"Dark":"Light"),
                            "resolved appearance not visible");
                    assertTrue(info.contains(system?"Follows system":"Manual light/dark"),
                            "light/dark preference not explained");
                    assertTrue(info.contains("Applied: "+name),
                            "actual resolved source missing");
                    assertTrue(!label.contains("null")&&!info.contains("null"),
                            "invalid status text");
                }
            }
        }
        assertTrue("Shaded D".equals(
                ThemeStatusLabel.expandedChip("App Match — Shaded",true)),
                "shaded chip label");
        assertTrue("MYou L".equals(
                ThemeStatusLabel.expandedChip("Material You",false)),
                "Material You chip label");
        assertTrue("Clean D".equals(
                ThemeStatusLabel.expandedChip("Clean Neutral",true)),
                "Clean Neutral chip label");
        assertTrue("Theme: Unknown • Dark".equals(
                ThemeStatusLabel.compact(null,true)),"null mode");
        assertTrue("Theme: Unknown • Light".equals(
                ThemeStatusLabel.compact("   ",false)),"empty mode");
        assertTrue(!ThemeStatusLabel.details("Purple",true,true,null)
                .contains("Applied:"),"unknown source must not be shown");
        Random random=new Random(2922026L);
        for(int i=0;i<50000;i++) {
            String mode=modes[random.nextInt(modes.length)];
            boolean dark=random.nextBoolean();
            boolean follow=random.nextBoolean();
            String info=ThemeStatusLabel.details(mode,dark,follow,
                    mode+" • Material fallback");
            assertTrue(info.contains(mode)&&info.contains(
                    dark?"Dark":"Light")&&info.contains(
                    follow?"Follows system":"Manual light/dark"),
                    "random theme state not correctly represented");
        }
        System.out.println("ThemeStatusLabelTest passed: "+checks+" checks");
    }
}
