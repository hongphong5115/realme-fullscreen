"""Exercise configuration through the module UI, not a public write API.
Requires an unlocked phone and ADB; restores enabled=true and Chrome exception=false.
"""
import argparse,json,re,subprocess,time,xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--adb',default='adb');a=p.parse_args()
out=Path(__file__).parent/'test-results';out.mkdir(exist_ok=True)
def run(*args):return subprocess.run([a.adb,*args],capture_output=True,check=True,timeout=25).stdout
def sh(*args):return run('shell',*args).decode('utf-8','replace')
def dump():
 sh('uiautomator','dump','/data/local/tmp/fullscreen-config-ui.xml')
 return ET.fromstring(run('exec-out','cat','/data/local/tmp/fullscreen-config-ui.xml'))
def tap(node):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')));sh('input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.6)
def text(ui,s):return next(n for n in ui.iter('node') if n.get('text')==s)
def openmain():
 # Oplus may bring ProbeActivity back for a reused app task; start clean.
 sh('am','force-stop','dev.codex.realmefullscreen');sh('am','start','-n','dev.codex.realmefullscreen/.MainActivity');time.sleep(1.2)
def switch(on):
 openmain();node=text(dump(),'Bật module')
 if (node.get('checked')=='true')!=on:tap(node)
 time.sleep(1.2)
def screenshot(name):(out/f'{name}.png').write_bytes(run('exec-out','screencap','-p'))
results={}
try:
 switch(False);sh('am','start','-n','dev.codex.realmefullscreen/.ProbeActivity');time.sleep(2)
 ui=dump();screenshot('config-disabled')
 results['disabled']=[n.get('text') for n in ui.iter('node') if n.get('text','').startswith('Content x=')]
 switch(True);sh('am','start','-n','dev.codex.realmefullscreen/.ProbeActivity');time.sleep(2)
 ui=dump();screenshot('config-enabled')
 results['enabled']=[n.get('text') for n in ui.iter('node') if n.get('text','').startswith('Content x=')]
 openmain();ui=dump();query=next(n for n in ui.iter('node') if n.get('class')=='android.widget.EditText');tap(query);sh('input','text','com.android.chrome');sh('input','keyevent','4');time.sleep(.5)
 ui=dump();row=next(n for n in ui.iter('node') if n.get('text','').startswith('Chrome\ncom.android.chrome'));tap(row)
 tap(text(dump(),'Ngoại lệ: dùng bố trí gốc'))
 results['exception_on']=sh('content','call','--uri','content://dev.codex.realmefullscreen.config','--method','read')
 sh('am','force-stop','com.android.chrome');sh('am','start','-a','android.intent.action.VIEW','-d','about:blank','com.android.chrome');time.sleep(2);screenshot('chrome-exception')
 openmain();ui=dump();query=next(n for n in ui.iter('node') if n.get('class')=='android.widget.EditText');tap(query);sh('input','text','com.android.chrome');sh('input','keyevent','4');time.sleep(.5)
 ui=dump();row=next(n for n in ui.iter('node') if n.get('text','').startswith('Chrome\ncom.android.chrome'));tap(row);tap(text(dump(),'Áp dụng theo công tắc chung'))
 results['exception_off']=sh('content','call','--uri','content://dev.codex.realmefullscreen.config','--method','read')
 sh('am','force-stop','com.android.chrome');sh('am','start','-a','android.intent.action.VIEW','-d','about:blank','com.android.chrome');time.sleep(2);screenshot('chrome-enabled')
finally:
 switch(True)
 (out/'config.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(results,ensure_ascii=True))
