import re, subprocess, os, json, sys, time
os.environ["MSYS_NO_PATHCONV"]="1"
CWD="D:/AndroidProject/VelvetCouples/audit_report"
def dump(tag="m"):
    subprocess.run(["adb","shell","uiautomator","dump","/sdcard/ui.xml"],capture_output=True)
    subprocess.run(["adb","pull","/sdcard/ui.xml",CWD+"/ui_%s.xml"%tag],capture_output=True)
    try: return open(CWD+"/ui_%s.xml"%tag,encoding='utf-8').read()
    except: return ""
def nodes(s):
    out=[]
    for m in re.finditer(r'<node[^>]*>',s):
        t=m.group(0)
        g=lambda k,d="": (re.search(k+r'="([^"]*)"',t).group(1) if re.search(k+r'="([^"]*)"',t) else d)
        b=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',t)
        out.append(dict(text=g('text'),rid=g('resource-id'),cls=g('class'),
                        bounds=(int(b.group(1)),int(b.group(2)),int(b.group(3)),int(b.group(4))) if b else None,
                        enabled=g('enabled','true')))
    return out
def visible_modes(s):
    r=[]
    for n in nodes(s):
        if n['rid'].endswith('tv_mode_title') and n['text']:
            x1,y1,x2,y2=n['bounds']
            r.append((n['text'],(x1+x2)//2,(y1+y2)//2,y1))
    return r
def swipe(y1,y2,dur=300):
    subprocess.run(["adb","shell","input","swipe",str(450),str(y1),str(450),str(y2),str(dur)],capture_output=True)
def tap(x,y):
    subprocess.run(["adb","shell","input","tap",str(x),str(y)],capture_output=True)
def back():
    subprocess.run(["adb","shell","input","keyevent","4"],capture_output=True)

if __name__=="__main__":
    # 从当前位置滚到顶部
    for _ in range(6): swipe(300,1400,300); time.sleep(0.25)
    time.sleep(0.5)
    seen=[]
    for step in range(11):
        s=dump("scan%d"%step)
        vs=visible_modes(s)
        added=0
        for t,x,y,yy in vs:
            if t not in [a[0] for a in seen]:
                seen.append((t,x,y,yy)); added+=1
        if step>=10: break
        swipe(1400,350,300); time.sleep(0.4)
    print("收集到模式标题数:",len(seen))
    for i,(t,x,y,yy) in enumerate(seen,1): print("%2d. %s"%(i,t))
    json.dump(seen,open(CWD+"/modes_seen.json","w"),ensure_ascii=False)
