"""Verify APK identity, packaging boundaries and independent source handoff."""
import argparse,hashlib,json,struct,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--apk',type=Path,required=True);a=p.parse_args()
root=Path(__file__).resolve().parent
manifest=ET.parse(root/'AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
assert manifest.get('package')=='dev.codex.realmefullscreen'
assert manifest.get(ns+'versionName')=='1.0' and manifest.get(ns+'versionCode')=='1'
provenance=root/'SOURCE-SHA256.json'
if provenance.exists():
 for name,expected in json.loads(provenance.read_text(encoding='utf-8')).items():
  data=(root/name).read_bytes().replace(b'\r\n',b'\n')
  assert hashlib.sha256(data).hexdigest()==expected,f'Original functional source changed: {name}'
original=root/'release/realme-fullscreen-1.0.apk'
if original.exists():
 assert hashlib.sha256(original.read_bytes()).hexdigest()=='be3cb7e2a77023becda8cf3cca2ebf73195c0617b08a9584eb831bca624a9bc8','Original APK changed'
def classes(data):
 assert data.startswith(b'dex\n')
 def u32(off):return struct.unpack_from('<I',data,off)[0]
 def uleb(off):
  for _ in range(5):
   byte=data[off];off+=1
   if byte<128:return off
  raise ValueError('bad ULEB')
 strings=[]
 for i in range(u32(56)):
  start=uleb(u32(u32(60)+4*i));strings.append(data[start:data.index(b'\0',start)].decode('utf-8','replace'))
 types=[strings[u32(u32(68)+i*4)] for i in range(u32(64))]
 return {types[u32(u32(100)+i*32)] for i in range(u32(96))}
with zipfile.ZipFile(a.apk) as z:
 names=z.namelist();assert z.read('assets/xposed_init').strip()==b'dev.codex.realmefullscreen.Hooks'
 assert not any('quickstep' in n or n.endswith(('.jks','.db','.java')) for n in names)
 found=classes(z.read('classes.dex'))
 assert all(n.startswith('Ldev/codex/realmefullscreen/') or n=='Lcom/android/tools/r8/annotations/LambdaMethod;' for n in found),found
 assert not any('LsposedSetup' in n for n in found)
 for name in ['ConfigProvider','Hooks','MainActivity','ProbeActivity','ScopeTool']:
  assert f'Ldev/codex/realmefullscreen/{name};' in found
source='\n'.join(f.read_text(encoding='utf-8') for f in (root/'src').rglob('*.java'))
for forbidden in ['com.google.android.googlequicksearchbox','KEYCODE_POWER','showSession','InputConsumer','FLAG_SECURE']:
 assert forbidden not in source,forbidden
print(f'PASS: {len(found)} own/compiler classes; no original gesture payload, maintenance writer, private key or database')
print('APK SHA256',hashlib.sha256(a.apk.read_bytes()).hexdigest())
