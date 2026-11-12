"""Verify the hide and landscape-cutout switches independently through the UI."""
import argparse,json,re,subprocess,time,xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--adb',default='adb');a=p.parse_args()
out=Path(__file__).parent/'test-results';out.mkdir(exist_ok=True)
def run(*args):return subprocess.run([a.adb,*args],check=True,capture_output=True,timeout=25).stdout
def sh(*args):return run('shell',*args).decode('utf-8','replace')
def dump():
 sh('uiautomator','dump','/data/local/tmp/fullscreen-options.xml');return ET.fromstring(run('exec-out','cat','/data/local/tmp/fullscreen-options.xml'))
def setting(label,on):
 sh('am','force-stop','dev.codex.realmefullscreen');sh('am','start','-n','dev.codex.realmefullscreen/.MainActivity');time.sleep(1.5)
 node=next(n for n in dump().iter('node') if n.get('text')==label)
 if (node.get('checked')=='true')!=on:
  x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')));sh('input','tap',str((x1+x2)//2),str((y1+y2)//2))
 time.sleep(1.5)
def probe(name):
 sh('am','force-stop','dev.codex.realmefullscreen');sh('am','start','-n','dev.codex.realmefullscreen/.ProbeActivity');time.sleep(2)
 ui=dump();(out/f'{name}.png').write_bytes(run('exec-out','screencap','-p'))
 return [n.get('text') for n in ui.iter('node') if n.get('text','').startswith('Content x=')]
old=sh('settings','get','system','user_rotation').strip();old_auto=sh('settings','get','system','accelerometer_rotation').strip();results={}
try:
 sh('settings','put','system','accelerometer_rotation','0')
 setting('Tràn vùng camera khi ngang (cả Gboard)',False);sh('settings','put','system','user_rotation','1');results['cutout_off']=probe('cutout-off')
 setting('Tràn vùng camera khi ngang (cả Gboard)',True);results['cutout_on']=probe('cutout-on')
 setting('Ẩn hàng giờ / pin / sóng',False);sh('settings','put','system','user_rotation','0');results['hide_off']=probe('hide-off')
 setting('Ẩn hàng giờ / pin / sóng',True);results['hide_on']=probe('hide-on')
finally:
 sh('settings','put','system','user_rotation',old)
 sh('settings','put','system','accelerometer_rotation',old_auto)
 setting('Tràn vùng camera khi ngang (cả Gboard)',True);setting('Ẩn hàng giờ / pin / sóng',True)
 (out/'options.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(results,ensure_ascii=True))
