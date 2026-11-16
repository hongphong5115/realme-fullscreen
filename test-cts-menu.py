from pathlib import Path
import subprocess,time,re,json,xml.etree.ElementTree as E
B=Path(__file__).resolve().parent;ADB=B.parents[1]/'platform-tools/adb.exe';out=B/'test-results/cutout13';out.mkdir(exist_ok=True)
def cmd(*args):return subprocess.run([str(ADB),*map(str,args)],check=True,capture_output=True,timeout=30).stdout
def sh(*args):return cmd('shell',*args).decode('utf-8','replace')
sh('am','start','-W','-f','0x10008000','-n','dev.codex.realmegestures/.GestureTestActivity','--ei','rotation','3');time.sleep(.7)
sh('am','start','-W','-n','com.parallelc.micts/.ui.activity.MainActivity');time.sleep(1)
sh('uiautomator','dump','/data/local/tmp/cutout-menu.xml');raw=sh('cat','/data/local/tmp/cutout-menu.xml');root=E.fromstring(raw)
nodes=[n for n in root.iter('node') if n.get('content-desc')=='More Lens options'];assert len(nodes)==1
b=list(map(int,re.findall(r'\d+',nodes[0].get('bounds'))));x,y=(b[0]+b[2])//2,(b[1]+b[3])//2
sh('input','tap',x,y);time.sleep(.6)
sh('uiautomator','dump','/data/local/tmp/cutout-menu.xml');raw=sh('cat','/data/local/tmp/cutout-menu.xml');root=E.fromstring(raw)
texts=[n.get('text') for n in root.iter('node') if n.get('text')]
(out/'r3-menu.xml').write_text(raw,encoding='utf-8');(out/'r3-menu.png').write_bytes(cmd('exec-out','screencap','-p'))
print(json.dumps({'rotation':3,'tap':[x,y],'texts':texts},ensure_ascii=True))
sh('input','keyevent','BACK')
