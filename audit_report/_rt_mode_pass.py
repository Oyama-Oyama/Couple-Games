import re, subprocess, os, json, time, sys
os.environ["MSYS_NO_PATHCONV"]="1"
CWD="D:/AndroidProject/VelvetCouples/audit_report"
def dump(tag="p"):
    subprocess.run(["adb","shell","uiautomator","dump","/sdcard/ui.xml"],capture_output=True)
    subprocess.run(["adb","pull","/sdcard/ui.xml",CWD+"/ui_%s.xml"%tag],capture_output=True)
    try: return open(CWD+"/ui_%s.xml"%tag,encoding='utf-8').read()
    except: return ""
def parse(s):
    out=[]
    for m in re.finditer(r'<node[^>]*>',s):
        t=m.group(0)
        g=lambda k,d="": (re.search(k+r'="([^"]*)"',t).group(1) if re.search(k+r'="([^"]*)"',t) else d)
        b=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',t)
        out.append((g('text'),g('resource-id'),(int(b.group(1)),int(b.group(2)),int(b.group(3)),int(b.group(4))) if b else None,g('enabled','true')))
    return out
def text_of(s,rid):
    for t,r,b,e in parse(s):
        if r.endswith(rid) and t: return t
    return ""
def modes(s):
    r=[]
    for t,rid,b,e in parse(s):
        if rid.endswith('tv_mode_title') and t and b and b[3]-b[1]>20:
            r.append((t,(b[0]+b[2])//2,(b[1]+b[3])//2))
    return r
def act_buttons(s):
    # 找候选互动按钮（排除返回/暂停/收藏/安全）
    cand=[]
    bad=('game_back','safe_pause','favorite','back','close','exit','share','letter_open','paywall')
    for t,rid,b,e in parse(s):
        rr=rid.split('/')[-1]
        if not rid.startswith('com.velvet') or not b: continue
        if e!='true': continue
        if b[1]<160 or b[3]>1440: continue
        if any(k in rr for k in bad): continue
        if rr.startswith(('btn_','tv_')) and any(k in rr for k in ('draw','spin','next','start','roll','random','shuffle','reveal','open','play','throw','flip','pick','step','action','primary','continue')):
            cand.append((rr,(b[0]+b[2])//2,(b[1]+b[3])//2,t))
    return cand
def swipe(y1,y2,dur=250):
    subprocess.run(["adb","shell","input","swipe","450",str(y1),"450",str(y2),str(dur)],capture_output=True)
def tap(x,y):
    subprocess.run(["adb","shell","input","tap",str(x),str(y)],capture_output=True)
def back():
    subprocess.run(["adb","shell","input","keyevent","4"],capture_output=True)

# 回到主页顶部
for _ in range(7): swipe(250,1450); time.sleep(0.2)
time.sleep(0.5)

results={}
stall=0
for step in range(45):
    s=dump("pass%d"%step)
    ms=modes(s)
    new=[m for m in ms if m[0] not in results]
    if not new:
        stall+=1
        if stall>=4: break
    else: stall=0
    for title,x,y in new:
        tap(x,y); time.sleep(1.6)
        d=dump("dst")
        dest=text_of(d,'tv_game_topbar_title')
        home=('<node' in d and 'nav_home' in d)
        paywall=('paywall' in d)
        dialogs=[t for t,r,b,e in parse(d) if r.split('/')[-1].startswith('tv_') and ('VIP' in t or '解锁' in t)][:2]
        inter='无互动按钮'
        # 尝试一次互动
        acts=act_buttons(d)
        if acts:
            rr,ax,ay,at=acts[0]
            before=parse(d)
            tap(ax,ay); time.sleep(0.9)
            d2=dump("inter")
            changed = (d2!=d)
            inter="%s(%s)%s"%(rr,at[:12],"有效" if changed else "无变化")
            # 若互动跳转到更深页面，多退一次
            if text_of(d2,'tv_game_topbar_title') and text_of(d2,'tv_game_topbar_title')!=dest:
                back(); time.sleep(0.8)
        back(); time.sleep(1.2)
        h=dump("homechk")
        okhome=('nav_home' in h)
        results[title]=dict(dest=dest or ('PAYWALL' if paywall else 'HOME/未跳转'),interaction=inter,back_to_home=okhome)
        print("%2d | %-36s | %-20s | %-34s | back=%s"%(len(results),title[:36],(dest or '-')[:20],inter[:34],okhome),flush=True)
    swipe(1300,850); time.sleep(0.45)

json.dump(results,open(CWD+"/mode_results.json","w"),ensure_ascii=False,indent=1)
print("TOTAL TESTED:",len(results))
