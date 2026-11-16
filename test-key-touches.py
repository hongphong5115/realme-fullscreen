"""Check physical edge-key coordinates in the expanded landscape keyboard."""
import argparse,json,subprocess,time,xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--adb',default='adb');p.add_argument('--rotation',type=int,choices=[1,3],default=1);p.add_argument('--edge',action='store_true');a=p.parse_args()
out=Path(__file__).parent/'test-results';out.mkdir(exist_ok=True)
def sh(*args):return subprocess.run([a.adb,'shell',*args],check=True,capture_output=True,timeout=25).stdout.decode('utf-8','replace')
old_auto=sh('settings','get','system','accelerometer_rotation').strip();old_rot=sh('settings','get','system','user_rotation').strip()
result={}
try:
 sh('settings','put','system','accelerometer_rotation','0');sh('settings','put','system','user_rotation',str(a.rotation))
 sh('am','start','-f','0x10008000','-n','dev.codex.realmefullscreen/.ProbeActivity','--ei','rotation',str(a.rotation));time.sleep(2)
 sh('input','tap','500','140');time.sleep(2)
 sh('input','tap','20' if a.edge else '118','603');sh('input','tap','2380' if a.edge else '2280','603');time.sleep(.5)
 sh('uiautomator','dump','/data/local/tmp/fullscreen-keys.xml')
 raw=sh('cat','/data/local/tmp/fullscreen-keys.xml');ui=ET.fromstring(raw)
 result['editor_text']=[n.get('text') for n in ui.iter('node') if n.get('content-desc')=='fullscreen-test-input']
 result['expected']='qp';result['passed']=result['editor_text']==['qp']
 (out/f'key-touches-{a.rotation}{"-edge" if a.edge else ""}.xml').write_text(raw,encoding='utf-8')
finally:
 sh('input','keyevent','4');sh('settings','put','system','user_rotation',old_rot);sh('settings','put','system','accelerometer_rotation',old_auto)
 (out/f'key-touches-{a.rotation}{"-edge" if a.edge else ""}.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result))
if not result.get('passed'):raise SystemExit(1)
