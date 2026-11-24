import re, subprocess, sys, os
def dump(tag="x"):
    os.environ["MSYS_NO_PATHCONV"]="1"
    subprocess.run(["adb","shell","uiautomator","dump","/sdcard/ui.xml"],capture_output=True)
    subprocess.run(["adb","pull","/sdcard/ui.xml","./ui_%s.xml"%tag],capture_output=True)
    try:
        s=open("./ui_%s.xml"%tag,encoding='utf-8').read()
    except: return []
    out=[]
    for m in re.finditer(r'<node[^>]*?text="([^"]*)"[^>]*?resource-id="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',s):
        t,r,x1,y1,x2,y2=m.group(1),m.group(2),int(m.group(3)),int(m.group(4)),int(m.group(5)),int(m.group(6))
        out.append((t,r,(x1+x2)//2,(y1+y2)//2,x2-x1,y2-y1))
    return out
def texts(tag="x"):
    return [(t,r) for t,r,*_ in dump(tag)]
def tap(x,y):
    subprocess.run(["adb","shell","input","tap",str(x),str(y)],capture_output=True)
def tap_text(s,tag="t",exact=False):
    for t,r,cx,cy,w,h in dump(tag):
        if (t==s) if exact else (s in t and t.strip()):
            tap(cx,cy); return True
    return False
if __name__=="__main__":
    cmd=sys.argv[1]
    if cmd=="dump":
        for t,r,cx,cy,w,h in dump(sys.argv[2] if len(sys.argv)>2 else "x"):
            if t.strip(): print("%-42s | %-58s | %4d,%4d %3dx%3d"%(repr(t),r.split('/')[-1],cx,cy,w,h))
    elif cmd=="tap":
        print("TAPPED:",tap_text(sys.argv[2],"tap"))
    elif cmd=="tapxy":
        tap(int(sys.argv[2]),int(sys.argv[3]))
