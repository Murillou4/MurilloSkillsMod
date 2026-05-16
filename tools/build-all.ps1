param(
    [switch] $SkipTests
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$repoRoot = Split-Path -Parent $PSScriptRoot
$tempRoot = Join-Path $repoRoot ".codex-temp"
$jdkRoot = Join-Path $tempRoot "jdks"
$jdk21 = Join-Path $jdkRoot "jdk-21"
$buildRoot = Join-Path $tempRoot "multi-target-build"
$libRoot = Join-Path $tempRoot "lib"
$distRoot = Join-Path $repoRoot "dist"
$modVersion = "1.2.74"
$gsonVersion = "2.8.0"
$gsonJar = Join-Path $libRoot "gson-$gsonVersion.jar"

function Ensure-Jdk21 {
    if (Test-Path (Join-Path $jdk21 "bin\javac.exe")) {
        return
    }

    New-Item -ItemType Directory -Force -Path $jdkRoot | Out-Null
    $zip = Join-Path $jdkRoot "temurin-jdk21.zip"
    if (-not (Test-Path $zip)) {
        $url = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
        Write-Host "Downloading portable JDK 21..."
        Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    }

    $extract = Join-Path $jdkRoot "extract-21"
    if (Test-Path $extract) {
        Remove-Item -Recurse -Force -LiteralPath $extract
    }
    New-Item -ItemType Directory -Force -Path $extract | Out-Null
    Expand-Archive -Path $zip -DestinationPath $extract -Force
    $jdkExtracted = Get-ChildItem -Path $extract -Directory | Select-Object -First 1
    if (-not $jdkExtracted) {
        throw "JDK 21 archive did not contain a JDK directory."
    }
    if (Test-Path $jdk21) {
        Remove-Item -Recurse -Force -LiteralPath $jdk21
    }
    Move-Item -LiteralPath $jdkExtracted.FullName -Destination $jdk21
}

function Ensure-Libs {
    New-Item -ItemType Directory -Force -Path $libRoot | Out-Null
    if (-not (Test-Path $gsonJar)) {
        $url = "https://repo1.maven.org/maven2/com/google/code/gson/gson/$gsonVersion/gson-$gsonVersion.jar"
        Write-Host "Downloading Gson $gsonVersion..."
        Invoke-WebRequest -Uri $url -OutFile $gsonJar -UseBasicParsing
    }
}

function Write-TextFile($path, $content) {
    $parent = Split-Path -Parent $path
    if ($parent) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    [System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))
}

function Write-Stubs($sourceDir) {
    Write-TextFile (Join-Path $sourceDir "net\fabricmc\api\ModInitializer.java") @"
package net.fabricmc.api;
public interface ModInitializer {
    void onInitialize();
}
"@
    Write-TextFile (Join-Path $sourceDir "net\fabricmc\api\ClientModInitializer.java") @"
package net.fabricmc.api;
public interface ClientModInitializer {
    void onInitializeClient();
}
"@
    Write-TextFile (Join-Path $sourceDir "net\minecraftforge\fml\common\Mod.java") @"
package net.minecraftforge.fml.common;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String value() default "";
    String modid() default "";
    String name() default "";
    String version() default "";
}
"@
    Write-TextFile (Join-Path $sourceDir "net\minecraftforge\eventbus\api\SubscribeEvent.java") @"
package net.minecraftforge.eventbus.api;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {}
"@
    Write-TextFile (Join-Path $sourceDir "net\minecraftforge\eventbus\api\Event.java") @"
package net.minecraftforge.eventbus.api;
public class Event {}
"@
    Write-TextFile (Join-Path $sourceDir "net\minecraftforge\fml\common\eventhandler\SubscribeEvent.java") @"
package net.minecraftforge.fml.common.eventhandler;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {}
"@
    Write-TextFile (Join-Path $sourceDir "net\minecraftforge\fml\common\eventhandler\Event.java") @"
package net.minecraftforge.fml.common.eventhandler;
public class Event {}
"@
    Write-TextFile (Join-Path $sourceDir "net\neoforged\fml\common\Mod.java") @"
package net.neoforged.fml.common;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String value() default "";
}
"@
    Write-TextFile (Join-Path $sourceDir "net\neoforged\bus\api\SubscribeEvent.java") @"
package net.neoforged.bus.api;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {}
"@
    Write-TextFile (Join-Path $sourceDir "net\neoforged\bus\api\Event.java") @"
package net.neoforged.bus.api;
public class Event {}
"@
}

function Get-JavaFiles($path) {
    Get-ChildItem -Path $path -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
}

function Invoke-Javac($sources, $destination, $classpath = "") {
    if ($sources.Count -eq 0) {
        throw "No Java sources were found for $destination"
    }
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    $javac = Join-Path $jdk21 "bin\javac.exe"
    $argFile = Join-Path $buildRoot ("javac-" + [guid]::NewGuid().ToString("N") + ".args")
    $normalizedDestination = $destination -replace "\\", "/"
    $args = @("--release", "8", "-encoding", "UTF-8", "-d", "`"$normalizedDestination`"")
    if ($classpath) {
        $normalizedClasspath = $classpath -replace "\\", "/"
        $args += @("-classpath", "`"$normalizedClasspath`"")
    }
    $args += ($sources | ForEach-Object { "`"$($_ -replace "\\", "/")`"" })
    Set-Content -Path $argFile -Value ($args -join [Environment]::NewLine) -Encoding ASCII
    & $javac "@$argFile"
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed for $destination"
    }
}

function Copy-ClassTree($from, $to, $relative) {
    $source = Join-Path $from $relative
    if (Test-Path $source) {
        $target = Join-Path $to $relative
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
        Copy-Item -Recurse -Force -Path $source -Destination (Split-Path -Parent $target)
    }
}

function Copy-ClassFile($from, $to, $relative) {
    $source = Join-Path $from $relative
    if (-not (Test-Path $source)) {
        throw "Missing class file $source"
    }
    $target = Join-Path $to $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Copy-Item -Force -Path $source -Destination $target
}

function Write-FabricMetadata($resourceDir, $mc, $loaderName) {
    $loaderId = if ($loaderName -eq "legacy-fabric") { "fabric" } else { "fabric" }
    Write-TextFile (Join-Path $resourceDir "fabric.mod.json") @"
{
  "schemaVersion": 1,
  "id": "murilloskills",
  "version": "$modVersion",
  "name": "Murillo Skills",
  "description": "Multi-version MurilloSkills runtime for Minecraft $mc ($loaderName).",
  "authors": ["Murillo"],
  "contact": {},
  "license": "MIT",
  "icon": "assets/murilloskills/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": ["com.murilloskills.platform.fabric.MurilloSkillsFabricEntrypoint"],
    "client": ["com.murilloskills.platform.fabric.MurilloSkillsFabricClientEntrypoint"]
  },
  "depends": {
    "fabricloader": ">=0.14.0",
    "minecraft": "$mc"
  },
  "custom": {
    "murilloskills:loader": "$loaderId",
    "murilloskills:runtime": "reflective-core"
  }
}
"@
}

function Get-MinecraftVersionRange($mc) {
    switch ($mc) {
        "1.12.2" { return "[1.12.2,1.13)" }
        "1.16.5" { return "[1.16.5,1.17)" }
        "1.18.2" { return "[1.18.2,1.19)" }
        "1.19.2" { return "[1.19.2,1.20)" }
        "1.20.1" { return "[1.20.1,1.21)" }
        "1.21.1" { return "[1.21.1,1.22)" }
        default { return "[$mc,)" }
    }
}

function Write-ForgeMetadata($resourceDir, $mc, $forgeMajor, $legacy) {
    if ($legacy) {
        Write-TextFile (Join-Path $resourceDir "mcmod.info") @"
[
  {
    "modid": "murilloskills",
    "name": "Murillo Skills",
    "description": "Multi-version MurilloSkills runtime for Minecraft $mc Forge.",
    "version": "$modVersion",
    "mcversion": "$mc",
    "authorList": ["Murillo"]
  }
]
"@
        return
    }

    New-Item -ItemType Directory -Force -Path (Join-Path $resourceDir "META-INF") | Out-Null
    $minecraftRange = Get-MinecraftVersionRange $mc
    Write-TextFile (Join-Path $resourceDir "META-INF\mods.toml") @"
modLoader="javafml"
loaderVersion="[$forgeMajor,)"
license="MIT"
issueTrackerURL="https://github.com/"

[[mods]]
modId="murilloskills"
version="$modVersion"
displayName="Murillo Skills"
authors="Murillo"
description='''Multi-version MurilloSkills runtime for Minecraft $mc Forge.'''

[[dependencies.murilloskills]]
modId="forge"
mandatory=true
versionRange="[$forgeMajor,)"
ordering="NONE"
side="BOTH"

[[dependencies.murilloskills]]
modId="minecraft"
mandatory=true
versionRange="$minecraftRange"
ordering="NONE"
side="BOTH"
"@
}

function Write-NeoForgeMetadata($resourceDir, $mc, $neoVersion) {
    New-Item -ItemType Directory -Force -Path (Join-Path $resourceDir "META-INF") | Out-Null
    $minecraftRange = Get-MinecraftVersionRange $mc
    Write-TextFile (Join-Path $resourceDir "META-INF\neoforge.mods.toml") @"
modLoader="javafml"
loaderVersion="[1,)"
license="MIT"
issueTrackerURL="https://github.com/"

[[mods]]
modId="murilloskills"
version="$modVersion"
displayName="Murillo Skills"
authors="Murillo"
description='''Multi-version MurilloSkills runtime for Minecraft $mc NeoForge.'''

[[dependencies.murilloskills]]
modId="neoforge"
type="required"
versionRange="[$neoVersion,)"
ordering="NONE"
side="BOTH"

[[dependencies.murilloskills]]
modId="minecraft"
type="required"
versionRange="$minecraftRange"
ordering="NONE"
side="BOTH"
"@
}

function Copy-SharedResources($resourceDir, $mc, $loader) {
    $assetsSource = Join-Path $repoRoot "src\main\resources\assets\murilloskills"
    $assetsTarget = Join-Path $resourceDir "assets\murilloskills"
    if (Test-Path $assetsSource) {
        New-Item -ItemType Directory -Force -Path $assetsTarget | Out-Null
        Copy-Item -Recurse -Force -Path (Join-Path $assetsSource "lang") -Destination $assetsTarget
        Copy-Item -Force -Path (Join-Path $assetsSource "icon.png") -Destination $assetsTarget
    }
    Write-TextFile (Join-Path $resourceDir "murilloskills-target.json") @"
{
  "minecraft": "$mc",
  "loader": "$loader",
  "runtime": "reflective-core",
  "saveSchema": "murilloskills/players/<uuid>.json"
}
"@
}

function Build-Target($target, $commonClasses, $runtimeClasses, $gsonClasses) {
    $mc = $target.minecraft
    $loader = $target.loader
    Write-Host ("Packaging {0} / {1}..." -f $mc, $loader)
    $work = Join-Path $buildRoot ("jar-" + $mc + "-" + $loader)
    $jarRoot = Join-Path $work "root"
    $resourceDir = Join-Path $work "resources"
    if (Test-Path $work) {
        Remove-Item -Recurse -Force -LiteralPath $work
    }
    New-Item -ItemType Directory -Force -Path $jarRoot, $resourceDir | Out-Null

    Copy-ClassTree $commonClasses $jarRoot "com\murilloskills\core"
    Copy-ClassTree $runtimeClasses $jarRoot "com\murilloskills\runtime"

    if ($loader -eq "fabric" -or $loader -eq "legacy-fabric") {
        Copy-ClassTree $runtimeClasses $jarRoot "com\murilloskills\platform\fabric"
        Write-FabricMetadata $resourceDir $mc $loader
    } elseif ($loader -eq "forge") {
        Copy-ClassFile $runtimeClasses $jarRoot "com\murilloskills\platform\forge\ForgeRuntimeRegistration.class"
        if ([bool]$target.legacy) {
            Copy-ClassFile $runtimeClasses $jarRoot "com\murilloskills\platform\forge\MurilloSkillsForgeLegacyEntrypoint.class"
            Copy-ClassFile $runtimeClasses $jarRoot "com\murilloskills\platform\forge\ForgeLegacyEventSink.class"
        } else {
            Copy-ClassFile $runtimeClasses $jarRoot "com\murilloskills\platform\forge\MurilloSkillsForgeEntrypoint.class"
            Copy-ClassFile $runtimeClasses $jarRoot "com\murilloskills\platform\forge\ForgeModernEventSink.class"
        }
        Write-ForgeMetadata $resourceDir $mc $target.forgeMajor ([bool]$target.legacy)
    } elseif ($loader -eq "neoforge") {
        Copy-ClassTree $runtimeClasses $jarRoot "com\murilloskills\platform\neoforge"
        Write-NeoForgeMetadata $resourceDir $mc $target.neoVersion
    } else {
        throw "Unknown loader '$loader'"
    }

    Copy-SharedResources $resourceDir $mc $loader
    Copy-Item -Recurse -Force -Path (Join-Path $resourceDir "*") -Destination $jarRoot

    $manifest = Join-Path $work "MANIFEST.MF"
    Write-TextFile $manifest @"
Manifest-Version: 1.0
Implementation-Title: MurilloSkills
Implementation-Version: $modVersion
Specification-Version: $mc-$loader

"@
    $outDir = Join-Path $distRoot "$mc\$loader"
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $jarName = "murilloskills-$modVersion+mc$mc-$loader.jar"
    $jarPath = Join-Path $outDir $jarName
    if (Test-Path $jarPath) {
        Remove-Item -Force -LiteralPath $jarPath
    }
    $jar = Join-Path $jdk21 "bin\jar.exe"
    Push-Location $jarRoot
    try {
        & $jar --create --file "$jarPath" --manifest "$manifest" -C "$jarRoot" .
        if ($LASTEXITCODE -ne 0) {
            throw "jar failed for $mc/$loader"
        }
    } finally {
        Pop-Location
    }
    Write-Host ("Built {0} / {1}: {2}" -f $mc, $loader, $jarPath)
}

Ensure-Jdk21
Ensure-Libs
$env:JAVA_HOME = $jdk21
$env:Path = "$jdk21\bin;$env:Path"

if (Test-Path $buildRoot) {
    Remove-Item -Recurse -Force -LiteralPath $buildRoot
}
New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null

$stubSource = Join-Path $buildRoot "stubs-src"
$stubClasses = Join-Path $buildRoot "stubs-classes"
Write-Stubs $stubSource
Invoke-Javac @(Get-JavaFiles $stubSource) $stubClasses

$commonClasses = Join-Path $buildRoot "common-core-classes"
$runtimeClasses = Join-Path $buildRoot "runtime-classes"
Invoke-Javac @(Get-JavaFiles (Join-Path $repoRoot "common-core\src\main\java")) $commonClasses $gsonJar
Invoke-Javac @(Get-JavaFiles (Join-Path $repoRoot "multi-loader-runtime\src\main\java")) $runtimeClasses "$gsonJar;$commonClasses;$stubClasses"

$gsonExtract = Join-Path $buildRoot "gson-classes"
New-Item -ItemType Directory -Force -Path $gsonExtract | Out-Null
Push-Location $gsonExtract
try {
    & (Join-Path $jdk21 "bin\jar.exe") -xf $gsonJar
} finally {
    Pop-Location
}

$targets = @(
    @{ minecraft = "1.21.1"; loader = "fabric" },
    @{ minecraft = "1.21.1"; loader = "forge"; forgeMajor = "52" },
    @{ minecraft = "1.21.1"; loader = "neoforge"; neoVersion = "21.1.230" },
    @{ minecraft = "1.20.1"; loader = "fabric" },
    @{ minecraft = "1.20.1"; loader = "forge"; forgeMajor = "47" },
    @{ minecraft = "1.19.2"; loader = "fabric" },
    @{ minecraft = "1.19.2"; loader = "forge"; forgeMajor = "43" },
    @{ minecraft = "1.18.2"; loader = "fabric" },
    @{ minecraft = "1.18.2"; loader = "forge"; forgeMajor = "40" },
    @{ minecraft = "1.16.5"; loader = "fabric" },
    @{ minecraft = "1.16.5"; loader = "forge"; forgeMajor = "36" },
    @{ minecraft = "1.12.2"; loader = "legacy-fabric" },
    @{ minecraft = "1.12.2"; loader = "forge"; forgeMajor = "14"; legacy = $true }
)

foreach ($target in $targets) {
    Build-Target $target $commonClasses $runtimeClasses $gsonExtract
}

Write-Host ""
Write-Host "Publishing current Fabric 1.21.10 jar via Gradle..."
$gradle = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradle)) {
    throw "Gradle wrapper not found at $gradle"
}
Push-Location $repoRoot
try {
    & $gradle --no-daemon --console=plain publishCurrentFabricJar
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle publishCurrentFabricJar failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "All requested MurilloSkills target jars were published under $distRoot"
