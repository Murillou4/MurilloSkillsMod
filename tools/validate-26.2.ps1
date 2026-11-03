param([string]$JavaHome = "", [switch]$SkipBuild)
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path $PSScriptRoot -Parent
$targetRoot = Join-Path $repoRoot "targets\26.2\fabric"
$modVersion = ((Get-Content -LiteralPath (Join-Path $targetRoot 'gradle.properties') | Where-Object { $_ -match '^mod_version=' }) -replace '^mod_version=', '').Trim()
if ($modVersion -notmatch '^2\.\d+\.\d+$') { throw 'Versao do alvo invalida.' }
if (-not $JavaHome) {
    $candidate = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-25*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($candidate) { $JavaHome = $candidate.FullName } else { $JavaHome = $env:JAVA_HOME }
}
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe"))) { throw "Java 25 requerido." }
$env:JAVA_HOME = $JavaHome
$env:Path = (Join-Path $JavaHome "bin") + ";" + $env:Path
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportRoot = Join-Path $targetRoot ("artifacts\general-" + $stamp)
$runtimeRoot = Join-Path $reportRoot "runtime"
New-Item -ItemType Directory -Force -Path $runtimeRoot | Out-Null
Push-Location $targetRoot
try {
    if (-not $SkipBuild) {
        & .\gradlew.bat --no-daemon --console=plain clean build gametestClasses -PuseReleaseJar=true 2>&1 | Tee-Object -FilePath (Join-Path $reportRoot "build.log")
        if ($LASTEXITCODE -ne 0) { throw "Build/JVM falhou." }
    }
    & .\gradlew.bat --no-daemon --console=plain runClientGameTest -PuseReleaseJar=true "-PevidenceDir=$runtimeRoot" 2>&1 | Tee-Object -FilePath (Join-Path $reportRoot "runtime.log")
    if ($LASTEXITCODE -ne 0) { throw "Revisao no Minecraft falhou." }
    foreach ($name in @("gameplay.txt","ui.txt")) {
        $lines = Get-Content -LiteralPath (Join-Path $runtimeRoot $name) | Where-Object { $_.Trim() }
        if ($lines[-1] -cne "PASS" -or ($lines | Where-Object { $_ -match "^(FAIL|ERROR)" })) { throw "Relatorio invalido: $name" }
    }
    if (Test-Path -LiteralPath (Join-Path $runtimeRoot "failure.txt")) { throw "Fixture registrou falha." }
    $summary = Get-Content -Raw -LiteralPath (Join-Path $runtimeRoot "summary.json") | ConvertFrom-Json
    $jar = Join-Path $targetRoot "build\libs\murilloskills-26.2-fabric-$modVersion.jar"
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $jar).Hash.ToLowerInvariant()
    if ($summary.sha256 -cne $hash) { throw "JAR executado difere do JAR de entrega." }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $forbidden = $zip.Entries | Where-Object { $_.FullName -match "com/murilloskills/validation/|murilloskills-test|techreborn|multi.loader|reflectionruntime" }
        if ($forbidden) { throw "JAR contem fixtures ou runtime externo." }
    } finally { $zip.Dispose() }
    $distribution = Join-Path $repoRoot "dist\26.2\fabric"
    $ready = Join-Path $repoRoot "PRONTO_26.2"
    New-Item -ItemType Directory -Force -Path $distribution,$ready | Out-Null
    Copy-Item -LiteralPath $jar -Destination $distribution -Force
    Copy-Item -LiteralPath $jar -Destination $ready -Force
    $apiRoot = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-api\0.161.0+26.2"
    $api = Get-ChildItem -LiteralPath $apiRoot -Recurse -File -Filter "fabric-api-0.161.0+26.2.jar" | Select-Object -First 1
    if (-not $api) { throw "Dependencia Fabric API nao encontrada no cache." }
    Copy-Item -LiteralPath $api.FullName -Destination $ready -Force
    foreach ($destination in @($distribution,$ready)) {
        $copied = Join-Path $destination (Split-Path $jar -Leaf)
        if ((Get-FileHash -Algorithm SHA256 -LiteralPath $copied).Hash.ToLowerInvariant() -cne $hash) { throw "Copia inconsistente: $copied" }
    }
    $instructions = @"
MURILLOSKILLS $modVersion / FABRIC 26.2

Para abrir o cliente isolado: use TESTAR_26.2.cmd na raiz do repositorio.
Para instalar no seu launcher: Minecraft 26.2, Java 25, Fabric Loader 0.20.1 ou mais recente.
Copie os dois JARs desta pasta para mods. Servidores tambem precisam dos dois.

O: painel / R: ativar ou encerrar / V: radial / G: ferramentas / B: UltPlace / U: desfazer.
Ultmine: segure a tecla configurada (ponto por padrao) E quebre o primeiro bloco.
Radial de Ultmine: segure apostrofo, aponte para a forma e solte. Tambem em G > Radial.
Instalacoes anteriores mantem as teclas salvas, inclusive acento grave. Veja Controles > MurilloSkills.
Ultmine exige Minerador escolhido e nivel 15. O HUD mostra quando esta ativo ou bloqueado.
O abre as quatro posicoes. Na primeira escolha, marque de uma a quatro profissoes e confirme uma unica vez.
Controles na propria profissao: utilidades independentes e atalhos novos sem tecla.
Admin: /murilloskills admin maxselected (suas escolhas no nivel 100).
Admin em outro jogador: /murilloskills admin <jogador> maxselected.
Ultmine pode alternar. UltPlace pode ser armado para usar com clique direito.
Pincel ativo: marque dois cantos com um bloco na mao e confirme com B.
Desfazer pode ser usado durante o Pincel, fora de combate. Uma tentativa sem colocar nao apaga o desfazer anterior.
Leia targets/26.2/fabric/README.md para mais detalhes e importacao opcional.

SHA256 do mod: $hash
Relatorio da revisao: $reportRoot
"@
    Set-Content -LiteralPath (Join-Path $ready "LEIA-ME.txt") -Value $instructions -Encoding UTF8
    # Keep only the current generated mod in the ready folder; archive previous distribution outputs.
    $currentName = Split-Path $jar -Leaf
    $previous = @(Get-ChildItem -LiteralPath $distribution -File -Filter 'murilloskills-26.2-fabric-*.jar' | Where-Object Name -NE $currentName)
    if ($previous.Count -gt 0) {
        $archive = Join-Path $distribution 'previous'
        New-Item -ItemType Directory -Force -Path $archive | Out-Null
        foreach ($old in $previous) { Move-Item -LiteralPath $old.FullName -Destination (Join-Path $archive $old.Name) -Force }
    }
    foreach ($old in @(Get-ChildItem -LiteralPath $ready -File -Filter 'murilloskills-26.2-fabric-*.jar' | Where-Object Name -NE $currentName)) { Remove-Item -LiteralPath $old.FullName }
    Copy-Item -LiteralPath "artifacts\balance-simulation.csv","artifacts\catalog.md" -Destination $reportRoot -Force
    Copy-Item -LiteralPath (Join-Path $runtimeRoot "catalog22.md") -Destination (Join-Path $reportRoot "catalog.md") -Force
    Copy-Item -LiteralPath "build/reports/tests/test" -Destination (Join-Path $reportRoot "jvm") -Recurse -Force
    Copy-Item -LiteralPath "build/test-results/test" -Destination (Join-Path $reportRoot "junit") -Recurse -Force
    $images = Get-ChildItem -LiteralPath $runtimeRoot -Filter "*.png" | Sort-Object Name
    $gallery = ($images | ForEach-Object { '<figure><a href="runtime/' + $_.Name + '"><img loading="lazy" src="runtime/' + $_.Name + '"></a><figcaption>' + $_.BaseName + '</figcaption></figure>' }) -join [Environment]::NewLine
    $html = '<!doctype html><meta charset="utf-8"><title>MurilloSkills ' + $modVersion + ' / Revisao geral</title><style>body{background:#12131b;color:#ece8df;font:16px system-ui;margin:36px}a{color:#d6ad66}.gallery{display:grid;grid-template-columns:repeat(auto-fit,minmax(420px,1fr));gap:20px}figure{margin:0}img{width:100%}figcaption{padding:8px 0;color:#bbb8c8}code{word-break:break-all}</style><h1>MurilloSkills ' + $modVersion + ' / Fabric 26.2</h1><p>Build, JVM, mundo integrado, reabertura e servidor dedicado: PASS.</p><p>JAR executado e copiado: <code>' + $hash + '</code></p><p><a href="runtime/summary.json">Resumo JSON</a> · <a href="runtime/gameplay.txt">Gameplay</a> · <a href="runtime/ui.txt">Interface</a> · <a href="balance-simulation.csv">Simulacao</a> · <a href="catalog.md">Catalogo</a></p><div class="gallery">' + $gallery + '</div>'
    Set-Content -LiteralPath (Join-Path $reportRoot "index.html") -Value $html -Encoding UTF8
    Set-Content -LiteralPath (Join-Path $targetRoot "artifacts\latest-report.txt") -Value $reportRoot
    Write-Host "PASS - Relatorio: $reportRoot"
    Write-Host "JAR: $jar"
} finally { Pop-Location }
