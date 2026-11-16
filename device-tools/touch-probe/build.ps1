param([string]$ToolsDirectory='')
$ErrorActionPreference='Stop'
$project=Split-Path (Split-Path $PSScriptRoot)
$tools=if($ToolsDirectory){$ToolsDirectory}else{Join-Path $project 'tools'}
$bt=Join-Path $tools 'buildtools34/android-14'
$android=Join-Path $tools 'platform33/android-33-ext5/android.jar'
$out=Join-Path $project 'build/touch-probe';New-Item -ItemType Directory -Force $out,(Join-Path $out 'classes'),(Join-Path $out 'dex') | Out-Null
function Check {if($LASTEXITCODE -ne 0){throw "Probe build failed: $LASTEXITCODE"}}
& javac --release 8 -encoding UTF-8 -cp $android -d (Join-Path $out 'classes') (Join-Path $PSScriptRoot 'MainActivity.java');Check
& jar cf (Join-Path $out 'classes.jar') -C (Join-Path $out 'classes') .;Check
& java -cp (Join-Path $tools 'r8-current.jar') com.android.tools.r8.D8 --min-api 33 --lib $android --output (Join-Path $out 'dex') (Join-Path $out 'classes.jar');Check
& (Join-Path $bt 'aapt2.exe') link -I $android --manifest (Join-Path $PSScriptRoot 'AndroidManifest.xml') -o (Join-Path $out 'unsigned.apk');Check
& jar uf (Join-Path $out 'unsigned.apk') -C (Join-Path $out 'dex') classes.dex;Check
& (Join-Path $bt 'zipalign.exe') -f 4 (Join-Path $out 'unsigned.apk') (Join-Path $out 'aligned.apk');Check
$env:FULLSCREEN_PROBE_PASSWORD=(Get-Content -LiteralPath (Join-Path $project '.local/password.txt') -Raw).Trim()
try {
 & java -jar (Join-Path $bt 'lib/apksigner.jar') sign --ks (Join-Path $project '.local/fullscreen.jks') --ks-key-alias fullscreen --ks-pass env:FULLSCREEN_PROBE_PASSWORD --out (Join-Path $out 'probe.apk') (Join-Path $out 'aligned.apk');Check
}finally{Remove-Item Env:FULLSCREEN_PROBE_PASSWORD}
