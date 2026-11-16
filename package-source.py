"""Create a source-only handoff; private keys, device dumps and caches are excluded."""
from pathlib import Path
import argparse,hashlib,zipfile
p=argparse.ArgumentParser();p.add_argument('output',type=Path);a=p.parse_args()
root=Path(__file__).resolve().parent
allowed={'src','res','assets','device-tools','.github','release'}
files=sorted(f for f in root.rglob('*') if f.is_file() and (len(f.relative_to(root).parts)==1 and f.suffix in {'.md','.ps1','.json','.xml','.py'} or f.parent==root and f.name in {'LICENSE','NOTICE','.gitignore','.gitattributes'} or f.relative_to(root).parts[0] in allowed) and f.suffix not in {'.jks','.db','.apk','.dex'})
if a.output.exists():raise SystemExit('Output already exists; choose another path')
a.output.parent.mkdir(parents=True,exist_ok=True)
manifest=[]
with zipfile.ZipFile(a.output,'w',compression=zipfile.ZIP_DEFLATED) as z:
 for f in files:
  name=f'realme-fullscreen-1.3/{f.relative_to(root).as_posix()}';data=f.read_bytes();z.writestr(name,data);manifest.append(f'{hashlib.sha256(data).hexdigest()}  {name}')
 z.writestr('realme-fullscreen-1.3/EXPORT-SHA256SUMS','\n'.join(manifest)+'\n')
print(f'{len(files)} source files; SHA256 {hashlib.sha256(a.output.read_bytes()).hexdigest()}')
