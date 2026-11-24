import re, subprocess, sys, os
os.environ["MSYS_NO_PATHCONV"]="1"
tag=sys.argv[2] if len(sys.argv)>2 else "z"
subprocess.run(["adb","shell","uiautomator","dump","/sdcard/ui.xml"],capture_output=True)
subprocess.run(["adb","pull","/sdcard/ui.xml","./ui_%s.xml"%tag],capture_output=True)
s=open("./ui_%s.xml"%tag,encoding='utf-8').read()
rid=sys.argv[1]
pat=r'<node[^>]*resource-id="[^"]*'+re.escape(rid)+r'"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
m=re.search(pat,s)
if not m:
    pat=r'<node[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*?resource-id="[^"]*'+re.escape(rid)+r'"'
    m=re.search(pat,s)
if not m:
    print("NOTFOUND",rid); sys.exit(1)
x=(int(m.group(1))+int(m.group(3)))//2; y=(int(m.group(2))+int(m.group(4)))//2
cmd=sys.argv[3] if len(sys.argv)>3 else "tap"
if cmd=="tap":
    subprocess.run(["adb","shell","input","tap",str(x),str(y)],capture_output=True)
    print("TAPPED",rid,x,y)
elif cmd=="loc":
    print("LOC",rid,x,y)
