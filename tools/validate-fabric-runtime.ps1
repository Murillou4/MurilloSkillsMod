param(
    [string[]]$Versions = @(),
    [switch]$SkipBuild,
    [switch]$SkipSmoke,
    [int]$PostWorldWaitSeconds = 24
)

$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$smokeScript = Join-Path $root '.codex-temp\smoke\fabric-client-smoke.ps1'
if (-not (Test-Path -LiteralPath $smokeScript)) {
    throw "Smoke script not found: $smokeScript"
}

$jdk21 = Join-Path $root '.codex-temp\jdks\jdk-21'
if (-not (Test-Path -LiteralPath $jdk21)) {
    throw "JDK 21 not found: $jdk21"
}
$jdk17 = Join-Path $root '.codex-temp\jdks\jdk-17'
if (-not (Test-Path -LiteralPath $jdk17)) {
    throw "JDK 17 not found: $jdk17"
}

$env:JAVA_HOME = $jdk21
$env:Path = "$jdk21\bin;$env:Path"

$targets = @(
    @{
        Version = '1.21.10'
        Prefix = 'fabric-1.21.10'
        TargetDir = $root
        MinecraftVersion = '1.21.10'
        YarnMappings = '1.21.10+build.3'
        FabricVersion = '0.138.3+1.21.10'
        LoaderVersion = '0.18.1'
        JavaHome = $jdk21
        GameJavaHome = $jdk21
        QuickplayWorld = 'CodexSmoke12110'
        SingleplayerY = 0.50
        WorldRowY = 0.30
    },
    @{
        Version = '1.21.1'
        Prefix = 'fabric-1.21.1'
        TargetDir = Join-Path $root '.codex-temp\native-fabric\1.21.1-fabric'
        MinecraftVersion = '1.21.1'
        YarnMappings = '1.21.1+build.3'
        FabricVersion = '0.116.6+1.21.1'
        LoaderVersion = '0.16.14'
        JavaHome = $jdk21
        GameJavaHome = $jdk21
        QuickplayWorld = 'CodexSmoke1211'
        SingleplayerY = 0.50
        WorldRowY = 0.30
    },
    @{
        Version = '1.20.1'
        Prefix = 'fabric-1.20.1'
        TargetDir = Join-Path $root '.codex-temp\native-fabric\1.20.1-fabric'
        MinecraftVersion = '1.20.1'
        YarnMappings = '1.20.1+build.10'
        FabricVersion = '0.92.6+1.20.1'
        LoaderVersion = '0.16.14'
        JavaHome = $jdk21
        GameJavaHome = $jdk17
        QuickplayWorld = 'CodexSmoke'
        SingleplayerY = 0.50
        WorldRowY = 0.30
    },
    @{
        Version = '1.19.2'
        Prefix = 'fabric-1.19.2'
        TargetDir = Join-Path $root '.codex-temp\native-fabric\1.19.2-fabric'
        MinecraftVersion = '1.19.2'
        YarnMappings = '1.19.2+build.28'
        FabricVersion = '0.77.0+1.19.2'
        LoaderVersion = '0.16.14'
        JavaHome = $jdk21
        GameJavaHome = $jdk17
        QuickplayWorld = 'CodexSmoke'
        SingleplayerY = 0.50
        WorldRowY = 0.19
    },
    @{
        Version = '1.18.2'
        Prefix = 'fabric-1.18.2'
        TargetDir = Join-Path $root '.codex-temp\native-fabric\1.18.2-fabric'
        MinecraftVersion = '1.18.2'
        YarnMappings = '1.18.2+build.4'
        FabricVersion = '0.77.0+1.18.2'
        LoaderVersion = '0.16.14'
        JavaHome = $jdk21
        GameJavaHome = $jdk17
        QuickplayWorld = 'CodexSmoke'
        SingleplayerY = 0.50
        WorldRowY = 0.27
    },
    @{
        Version = '1.16.5'
        Prefix = 'fabric-1.16.5'
        TargetDir = Join-Path $root '.codex-temp\native-fabric\1.16.5-fabric'
        MinecraftVersion = '1.16.5'
        YarnMappings = '1.16.5+build.10'
        FabricVersion = '0.42.0+1.16'
        LoaderVersion = '0.16.14'
        JavaHome = $jdk21
        GameJavaHome = $jdk17
        QuickplayWorld = 'CodexSmoke'
        SingleplayerY = 0.50
        WorldRowY = 0.30
    }
)

if ($Versions.Count -gt 0) {
    $targets = $targets | Where-Object { $Versions -contains $_.Version }
    if (-not $targets -or $targets.Count -eq 0) {
        throw "No Fabric runtime targets matched: $($Versions -join ', ')"
    }
}

function Get-SelfTestLogPath([string]$TargetDir, [string]$FileName) {
    $candidates = @(
        (Join-Path $TargetDir "run\logs\$FileName"),
        (Join-Path $TargetDir "logs\$FileName")
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    return $candidates[0]
}

function Assert-SelfTestPass([string]$TargetDir, [string]$FileName, [string]$Name) {
    $path = Get-SelfTestLogPath $TargetDir $FileName
    if (-not (Test-Path -LiteralPath $path)) {
        throw "$Name log was not written: $path"
    }

    $lines = Get-Content -LiteralPath $path
    $badLines = @($lines | Where-Object { $_ -match '^(FAIL|ERROR)(\s|$)' })
    $lastLine = ($lines | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Last 1)
    if ($badLines.Count -gt 0 -or $lastLine.Trim() -ne 'PASS') {
        throw "$Name self-test failed: $path"
    }
    return $path
}

function Clear-SelfTestLogs([string]$TargetDir) {
    foreach ($fileName in @('murilloskills-skill-selftest.log', 'murilloskills-ui-selftest.log')) {
        foreach ($base in @((Join-Path $TargetDir 'run\logs'), (Join-Path $TargetDir 'logs'))) {
            $path = Join-Path $base $fileName
            if (Test-Path -LiteralPath $path) {
                Remove-Item -LiteralPath $path -Force
            }
        }
    }
}

$summary = @()
$previousSelfTest = $env:MURILLOSKILLS_SELFTEST
$env:MURILLOSKILLS_SELFTEST = '1'

try {
    foreach ($target in $targets) {
        $targetDir = $target.TargetDir
        if (-not (Test-Path -LiteralPath $targetDir)) {
            throw "Target directory not found for $($target.Version): $targetDir"
        }

        Write-Host "== Fabric $($target.Version) =="
        if (-not $SkipSmoke) {
            Clear-SelfTestLogs $targetDir
        }

        if (-not $SkipBuild) {
            & (Join-Path $targetDir 'gradlew.bat') --no-daemon --console=plain clean build
            if ($LASTEXITCODE -ne 0) {
                throw "Gradle build failed for $($target.Version)"
            }
        }

        $smokeOutput = @()
        if (-not $SkipSmoke) {
            try {
                $quickplayWorld = $target.QuickplayWorld
                $smokeOutput = & $smokeScript `
                    -Root $root `
                    -TargetDir $targetDir `
                    -Prefix $target.Prefix `
                    -MinecraftVersion $target.MinecraftVersion `
                    -YarnMappings $target.YarnMappings `
                    -FabricVersion $target.FabricVersion `
                    -LoaderVersion $target.LoaderVersion `
                    -SingleplayerY $target.SingleplayerY `
                    -WorldRowY $target.WorldRowY `
                    -PostWorldWaitSeconds $PostWorldWaitSeconds `
                    -JavaHome $target.JavaHome `
                    -GameJavaHome $target.GameJavaHome `
                    -QuickplayWorld $quickplayWorld
            } catch {
                throw "Client smoke failed for $($target.Version): $($_.Exception.Message)"
            }
        }

        $skillLog = Assert-SelfTestPass $targetDir 'murilloskills-skill-selftest.log' 'Skill runtime'
        $uiLog = Assert-SelfTestPass $targetDir 'murilloskills-ui-selftest.log' 'Client UI'

        $shots = @{}
        foreach ($line in $smokeOutput) {
            if ($line -match '^(WORLD_SHOT|MENU_SHOT|MAIN_SHOT|SINGLEPLAYER_SHOT)=(.+)$') {
                $shots[$matches[1]] = $matches[2]
            }
        }

        $summary += [pscustomobject]@{
            Version = $target.Version
            SkillLog = $skillLog
            UiLog = $uiLog
            WorldShot = $shots['WORLD_SHOT']
            MenuShot = $shots['MENU_SHOT']
        }
    }
} finally {
    $env:MURILLOSKILLS_SELFTEST = $previousSelfTest
}

$summaryPath = Join-Path $root '.codex-temp\runtime-validation-summary.json'
$summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $summaryPath -Encoding utf8
$summary | Format-Table -AutoSize
Write-Host "SUMMARY=$summaryPath"
