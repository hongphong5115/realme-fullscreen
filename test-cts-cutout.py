"""Real CTS close-button regression using visible bounds; no coordinate compensation."""
from pathlib import Path
import argparse,subprocess,time,json,re,xml.etree.ElementTree as E
p=argparse.ArgumentParser();p.add_argument('--rotation',type=int,choices=[0,1,3],required=True);p.add_argument('--source',choices=['pill','micts'],required=True);p.add_argument('--count',type=int,default=3);p.add_argument('--label',default='');a=p.parse_args()
B=Path(__file__).resolve().parent;ADB=B.parents[1]/'platform-tools/adb.exe';out=B/'test-results/cutout13';out.mkdir(parents=True,exist_ok=True)
def adb(*args):
 r=subprocess.run([str(ADB),*map(str,args)],capture_output=True,timeout=30)
 if r.returncode:raise RuntimeError(r.stderr.decode(errors='replace'))
 return r.stdout
def sh(*args):return adb('shell',*args).decode('utf-8','replace')
def ui():
 sh('uiautomator','dump','/data/local/tmp/cutout13.xml');raw=sh('cat','/data/local/tmp/cutout13.xml');return E.fromstring(raw),raw
label=f'{a.label+"-" if a.label else ""}{a.source}-r{a.rotation}';rows=[]
for i in range(a.count):
 sh('am','start','-W','-f','0x10008000','-n','dev.codex.realmegestures/.GestureTestActivity','--ei','rotation',a.rotation);time.sleep(.5)
 deadline=time.monotonic()+4
 while True:
  viewport=sh('dumpsys','input')
  if re.search(r'Viewport INTERNAL:.*?orientation='+str(a.rotation),viewport):break
  if time.monotonic()>deadline:raise RuntimeError('Requested orientation did not become active')
  time.sleep(.2)
 if a.source=='pill':
  x,y=(540,2375) if a.rotation==0 else (1200,1055);sh('input','swipe',x,y,x,y,1000)
 else:sh('am','start','-W','-n','com.parallelc.micts/.ui.activity.MainActivity')
 time.sleep(1)
 root,raw=ui();nodes=[n for n in root.iter('node') if n.get('content-desc')=='Close']
 if len(nodes)!=1:
  (out/f'{label}-{i}-failure.xml').write_text(raw,encoding='utf-8');raise RuntimeError('CTS Close not found uniquely')
 bounds=list(map(int,re.findall(r'\d+',nodes[0].get('bounds'))));x,y=(bounds[0]+bounds[2])//2,(bounds[1]+bounds[3])//2
 if i==0:
  (out/f'{label}-open.xml').write_text(raw,encoding='utf-8');(out/f'{label}-open.png').write_bytes(adb('exec-out','screencap','-p'))
 sh('input','tap',x,y);time.sleep(.8)
 focus=sh('dumpsys','window');focus='\n'.join(line for line in focus.splitlines() if 'mCurrentFocus=' in line)
 ok='GestureTestActivity' in focus and 'LensientActivity' not in focus
 row=dict(index=i+1,rotation=a.rotation,source=a.source,bounds=bounds,tap=[x,y],closed=ok,focus=focus);rows.append(row);print(json.dumps(row),flush=True)
 (out/f'{label}.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
 if not ok:raise RuntimeError('Close did not return to the correct background')
print('PASS',label,len(rows))
