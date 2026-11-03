param([string]$JavaHome = "")
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path $PSScriptRoot -Parent
$targetRoot = Join-Path $repoRoot "targets\26.2\fabric"
if (-not $JavaHome) {
    $candidate = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-25*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($candidate) { $JavaHome = $candidate.FullName } else { $JavaHome = $env:JAVA_HOME }
}
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe"))) { throw "Instale Java 25 ou informe -JavaHome." }
$env:JAVA_HOME = $JavaHome
$env:Path = (Join-Path $JavaHome "bin") + ";" + $env:Path
Push-Location $targetRoot
try {
    & .\gradlew.bat --no-daemon --console=plain runClient -PuseReleaseJar=true
    if ($LASTEXITCODE -ne 0) { throw "O cliente encerrou com codigo $LASTEXITCODE. Veja run\logs\latest.log." }
} finally { Pop-Location }
