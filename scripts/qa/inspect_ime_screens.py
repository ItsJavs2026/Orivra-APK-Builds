#!/usr/bin/env python3
"""Read ONLY screenshots of disposable Orivra emulator UI.
No OCR, no data collection, no connection to the Android app.
The GitHub job pins Pillow; this script never runs on user devices.
"""
import argparse
from PIL import Image

def keyboard_top(path):
    im=Image.open(path).convert("RGB")
    w,h=im.size
    if w<480 or h<720:
        raise AssertionError("Unexpected screenshot dimensions")
    # Orivra's default fresh-install App Match light panel has a pastel blue
    # background. On this fixed Android 35 Pixel_6 visual QA device, its left
    # edge turns from nearly neutral app white to the blue keyboard backdrop.
    # Do not interpret a Google keyboard screenshot as successful Orivra UI.
    for y in range(int(h*.42),int(h*.89)):
        rs=[]
        for off in (0,4,8,12):
            r,g,b=im.getpixel((0,y+off))
            rs.append(r<242 and g-r>=4 and b-r>=14)
        if all(rs):
            return y
    raise AssertionError("Orivra light keyboard background not found; "+
                         "check the retained screenshot and selected IME")

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("screenshot")
    p.add_argument("--collapsed",type=int)
    p.add_argument("--density",type=float,default=2.75)
    p.add_argument("--mode",choices=["top","expanded","recollapsed"],default="top")
    a=p.parse_args()
    top=keyboard_top(a.screenshot)
    if a.mode=="expanded":
        delta=a.collapsed-top
        assert a.density*40 <= delta <= a.density*105, (
            f"Theme panel did not expand by two rows: pixel delta={delta}")
    if a.mode=="recollapsed":
        assert abs(a.collapsed-top)<=round(a.density*5),(
            f"Theme panel did not recover zero reserved height: "+
            f"before={a.collapsed}, after={top}")
    print(top)
