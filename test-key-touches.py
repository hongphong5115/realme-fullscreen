"""Check physical edge-key coordinates in the expanded landscape keyboard."""
import argparse,json,subprocess,time,xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--adb',default='adb');a=p.parse_args()
out=Path(__file__).parent/'test-results';out.mkdir(exist_ok=True)
def sh(*args):return subprocess.run([a.adb,'shell',*args],check=True,capture_output=True,timeout=25).stdout.decode('utf-8','replace')
old_auto=sh('settings','get','system','accelerometer_rotation').strip();old_rot=sh('settings','get','system','user_rotation').strip()
result={}
try:
 sh('settings','put','system','accelerometer_rotation','0');sh('settings','put','system','user_rotation','1')
 sh('am','force-stop','dev.codex.realmefullscreen');sh('am','start','-n','dev.codex.realmefullscreen/.ProbeActivity');time.sleep(2)
 sh('input','tap','500','140');time.sleep(2)
 sh('input','tap','118','603');sh('input','tap','2280','603');time.sleep(.5)
 sh('uiautomator','dump','/data/local/tmp/fullscreen-keys.xml')
 raw=sh('cat','/data/local/tmp/fullscreen-keys.xml');ui=ET.fromstring(raw)
 result['editor_text']=[n.get('text') for n in ui.iter('node') if n.get('content-desc')=='fullscreen-test-input']
 result['expected']='qp';result['passed']=result['editor_text']==['qp']
 (out/'key-touches.xml').write_text(raw,encoding='utf-8')
finally:
 sh('input','keyevent','4');sh('settings','put','system','user_rotation',old_rot);sh('settings','put','system','accelerometer_rotation',old_auto)
 (out/'key-touches.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result))
if not result.get('passed'):raise SystemExit(1)
