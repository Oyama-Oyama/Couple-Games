import re, subprocess, os, time, json
os.environ["MSYS_NO_PATHCONV"]="1"
CWD="D:/AndroidProject/VelvetCouples/audit_report"
def sh(*a):
    return subprocess.run(["adb","shell"]+list(a),capture_output=True).stdout.decode('utf-8','replace')
def dump(tag="v"):
    sh("uiautomator","dump","/sdcard/ui.xml")
    subprocess.run(["adb","pull","/sdcard/ui.xml",CWD+"/ui_%s.xml"%tag],capture_output=True)
    try: return open(CWD+"/ui_%s.xml"%tag,encoding='utf-8').read()
    except: return ""
def texts(s):
    return re.findall(r'text="([^"]*)"',s)
def tap_text(s,needle):
    for m in re.finditer(r'<node[^>]*>',s):
        t=m.group(0)
        x=re.search(r'text="([^"]*)"',t)
        b=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',t)
        if x and needle in x.group(1) and b:
            xx=(int(b.group(1))+int(b.group(3)))//2; yy=(int(b.group(2))+int(b.group(4)))//2
            sh("input","tap",str(xx),str(yy)); return True
    return False
def prefs():
    return sh("run-as","com.velvet.couplesgame","ls","/data/data/com.velvet.couplesgame/shared_prefs/")
def start():
    sh("am","start","-n","com.velvet.couplesgame/.MainActivity")
    time.sleep(4)
def launch_fresh():
    sh("am","force-stop","com.velvet.couplesgame")
    sh("pm","clear","com.velvet.couplesgame")
    start()
def onboard():
    s=dump("ob1")
    if tap_text(s,"继续"): time.sleep(1.2)
    s=dump("ob2")
    if tap_text(s,"开始今夜"): time.sleep(1.5)
    return dump("ob3")

print("### 路径1：确认成年",flush=True)
launch_fresh(); s=onboard()
print("弹窗文本:",[t for t in texts(s) if '成年' in t or '18' in t][:4],flush=True)
print("tap confirm:",tap_text(s,"我已年满 18 岁"),flush=True)
time.sleep(1.5)
print("prefs:",prefs().strip().replace("\n"," | "),flush=True)
s=dump("p1after")
print("主页可见:", '大厅' in ' '.join(texts(s)), "| 年龄弹窗仍在:", '仅限成年人' in ' '.join(texts(s)),flush=True)
print("开关机重启:")
sh("am","force-stop","com.velvet.couplesgame"); start()
s=dump("p1restart")
print("  重启后年龄弹窗:", '仅限成年人' in ' '.join(texts(s)),flush=True)

print("### 路径2：拒绝（软门禁）",flush=True)
launch_fresh(); s=onboard()
print("tap decline:",tap_text(s,"我未满 18 岁"),flush=True)
time.sleep(1.0)
s=dump("p2declined")
print("declined 文案:",[t for t in texts(s) if '基础玩法' in t or '未尽' in t or '未满' in t or '这台设备' in t][:5],flush=True)
print("弹窗仍在:",'仅限成年人' in ' '.join(texts(s)),flush=True)
print("tap positive(继续):",tap_text(s,"我已年满 18 岁"),flush=True)
time.sleep(1.5)
print("prefs:",prefs().strip().replace("\n"," | "),flush=True)
s=dump("p2after")
print("主页可见:", '今晚心动尺度' in ' '.join(texts(s)), "| 年龄弹窗仍在:", '仅限成年人' in ' '.join(texts(s)),flush=True)
