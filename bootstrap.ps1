# Windows / PowerShell 7. Downloads tools only; never changes an Android device.
param([string]$CacheDirectory = '')
$ErrorActionPreference = 'Stop'
if (-not $IsWindows) { throw 'This pinned toolchain is Windows-only. See docs/BUILD.md.' }
$toolsDir = Join-Path $PSScriptRoot 'tools'
New-Item -ItemType Directory -Force $toolsDir | Out-Null
$lock = Get-Content (Join-Path $PSScriptRoot 'tools.lock.json') -Raw | ConvertFrom-Json
foreach ($item in $lock) {
    $path = Join-Path $toolsDir $item.file
    if (-not (Test-Path -LiteralPath $path)) {
        $cached = if ($CacheDirectory) { Join-Path $CacheDirectory $item.file } else { '' }
        if ($cached -and (Test-Path -LiteralPath $cached)) { Copy-Item -LiteralPath $cached -Destination $path }
        else { Write-Host "Downloading $($item.file)"; Invoke-WebRequest -Uri $item.url -OutFile $path }
    }
    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $item.sha256) { throw "Checksum mismatch for $($item.file); remove that file and inspect the download." }
    if ($item.extract) {
        if (-not (Test-Path -LiteralPath (Join-Path $toolsDir $item.probe))) {
            Expand-Archive -LiteralPath $path -DestinationPath (Join-Path $toolsDir $item.extract) -Force
        }
    }
    Write-Host "Verified $($item.file)"
}
Write-Host 'Toolchain ready. Next: ./build.ps1'
