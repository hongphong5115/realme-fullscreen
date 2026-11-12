param([string]$ToolsDirectory)
$ErrorActionPreference='Stop'
$out=Join-Path $PSScriptRoot '../build/device-tools'
New-Item -ItemType Directory -Force $out | Out-Null
& javac --release 8 -cp (Join-Path $ToolsDirectory 'platform33/android-33-ext5/android.jar') -d $out (Join-Path $PSScriptRoot 'LsposedSetup.java')
if($LASTEXITCODE){throw 'compile failed'}
& jar cf (Join-Path $out 'setup.jar') -C $out LsposedSetup.class
& java -cp (Join-Path $ToolsDirectory 'r8-current.jar') com.android.tools.r8.D8 --min-api 33 --output $out (Join-Path $out 'setup.jar')
if($LASTEXITCODE){throw 'DEX failed'}
