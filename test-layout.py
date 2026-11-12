"""ADB evidence collection for the independent fullscreen module. No gesture tests.

Rotates the primary display and uses only this module's ProbeActivity.
Restores rotation settings in finally. Screenshots/dumps stay local.
"""
import argparse,json,os,re,subprocess,time,xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--adb',default=os.environ.get('ADB','adb'));p.add_argument('--rotation',type=int,choices=[0,1,3],required=True);p.add_argument('--label',default='layout');a=p.parse_args()
out=Path(__file__).parent/'test-results';out.mkdir(exist_ok=True)
def cmd(*args):return subprocess.run([a.adb,*args],check=True,capture_output=True,timeout=25).stdout
def shell(*args):return cmd('shell',*args).decode('utf-8','replace')
def capture(name):
 (out/f'{name}.png').write_bytes(cmd('exec-out','screencap','-p'))
 shell('uiautomator','dump','/data/local/tmp/fullscreen-layout-ui.xml')
 raw=cmd('exec-out','cat','/data/local/tmp/fullscreen-layout-ui.xml')
 (out/f'{name}.xml').write_bytes(raw)
 return ET.fromstring(raw)
old_auto=shell('settings','get','system','accelerometer_rotation').strip();old_rot=shell('settings','get','system','user_rotation').strip()
record={'rotation':a.rotation,'label':a.label}
try:
 shell('settings','put','system','accelerometer_rotation','0');shell('settings','put','system','user_rotation',str(a.rotation))
 shell('am','force-stop','dev.codex.realmefullscreen');shell('am','start','-n','dev.codex.realmefullscreen/.ProbeActivity');time.sleep(2)
 ui=capture(f'{a.label}-{a.rotation}-content')
 record['content']=[n.get('text') for n in ui.iter('node') if n.get('text','').startswith('Content x=')]
 inputs=[n for n in ui.iter('node') if n.get('content-desc')=='fullscreen-test-input']
 if not inputs:raise RuntimeError('Probe input missing')
 coords=list(map(int,re.findall(r'\d+',inputs[0].get('bounds'))));x=(coords[0]+coords[2])//2;y=(coords[1]+coords[3])//2
 shell('input','tap',str(x),str(y));time.sleep(2)
 ui=capture(f'{a.label}-{a.rotation}-keyboard')
 (out/f'{a.label}-{a.rotation}-windows.txt').write_text(shell('dumpsys','window','windows'),encoding='utf-8')
 (out/f'{a.label}-{a.rotation}-activity.txt').write_text(shell('dumpsys','activity','top'),encoding='utf-8')
 record['keyboard_content']=[n.get('text') for n in ui.iter('node') if n.get('text','').startswith('Content x=')]
 record['bottom_button']=[n.get('bounds') for n in ui.iter('node') if 'Nút sát đáy' in n.get('text','') or 'NÚT SÁT ĐÁY' in n.get('text','')]
 shell('input','keyevent','4');time.sleep(.5)
 record['collection']='complete; inspect screenshots and bounds before acceptance'
finally:
 shell('settings','put','system','user_rotation',old_rot);shell('settings','put','system','accelerometer_rotation',old_auto)
 (out/f'{a.label}-{a.rotation}.json').write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(record,ensure_ascii=True))
