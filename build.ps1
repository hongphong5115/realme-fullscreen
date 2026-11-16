param([string]$ToolsDirectory = '')
$ErrorActionPreference='Stop'
$projectDir=$PSScriptRoot
$manifestDoc=[xml](Get-Content -LiteralPath (Join-Path $projectDir 'AndroidManifest.xml') -Raw)
$version=$manifestDoc.DocumentElement.GetAttribute('versionName','http://schemas.android.com/apk/res/android')
$apkName="realme-fullscreen-$version.apk"
if (!$ToolsDirectory) { $ToolsDirectory=Join-Path $projectDir 'tools' }
$bt=Join-Path $ToolsDirectory 'buildtools34/android-14'
$androidJar=Join-Path $ToolsDirectory 'platform33/android-33-ext5/android.jar'
$xposedJar=Join-Path $ToolsDirectory 'xposed-api82.jar'
$out=Join-Path $projectDir 'build'
$classes=Join-Path $out ('classes-'+[guid]::NewGuid().ToString('N'))
$dex=Join-Path $out ('dex-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $out,$classes,$dex | Out-Null
function Check([string]$phase){if($LASTEXITCODE -ne 0){throw "$phase failed: $LASTEXITCODE"}}
$files=@(Get-ChildItem (Join-Path $projectDir 'src') -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac --release 8 -encoding UTF-8 -classpath "$androidJar;$xposedJar" -d $classes @files
Check 'javac'
& jar cf (Join-Path $out 'classes.jar') -C $classes .
Check 'jar'
& java -cp (Join-Path $ToolsDirectory 'r8-current.jar') com.android.tools.r8.D8 --min-api 33 --lib $androidJar --classpath $xposedJar --output $dex (Join-Path $out 'classes.jar')
Check 'D8'
& (Join-Path $bt 'aapt2.exe') compile --dir (Join-Path $projectDir 'res') -o (Join-Path $out 'res.zip')
Check 'resources'
& (Join-Path $bt 'aapt2.exe') link -I $androidJar --manifest (Join-Path $projectDir 'AndroidManifest.xml') -o (Join-Path $out 'unsigned.apk') (Join-Path $out 'res.zip')
Check 'link'
& jar uf (Join-Path $out 'unsigned.apk') -C $dex classes.dex -C $projectDir assets
Check 'package'
& (Join-Path $bt 'zipalign.exe') -f 4 (Join-Path $out 'unsigned.apk') (Join-Path $out 'aligned.apk')
Check 'align'
$local=Join-Path $projectDir '.local'
New-Item -ItemType Directory -Force $local | Out-Null
$key=Join-Path $local 'fullscreen.jks'
$pass=Join-Path $local 'password.txt'
if (!(Test-Path -LiteralPath $pass)) { Set-Content -LiteralPath $pass -Value ([guid]::NewGuid().ToString('N')) -NoNewline }
$env:FULLSCREEN_BUILD_PASSWORD=(Get-Content -LiteralPath $pass -Raw).Trim()
try {
 if (!(Test-Path -LiteralPath $key)) {
  & keytool -genkeypair -keystore $key -storepass:env FULLSCREEN_BUILD_PASSWORD -keypass:env FULLSCREEN_BUILD_PASSWORD -alias fullscreen -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Realme Fullscreen Local Build'
  Check 'signing key'
 }
 & java -jar (Join-Path $bt 'lib/apksigner.jar') sign --ks $key --ks-key-alias fullscreen --ks-pass env:FULLSCREEN_BUILD_PASSWORD --key-pass env:FULLSCREEN_BUILD_PASSWORD --out (Join-Path $out $apkName) (Join-Path $out 'aligned.apk')
 Check 'sign'
 & java -jar (Join-Path $bt 'lib/apksigner.jar') verify --verbose (Join-Path $out $apkName)
 Check 'verify'
} finally { Remove-Item Env:FULLSCREEN_BUILD_PASSWORD }
Get-FileHash (Join-Path $out $apkName) -Algorithm SHA256
