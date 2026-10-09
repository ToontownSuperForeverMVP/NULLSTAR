param(
    [string]$Artifact = (Join-Path $PSScriptRoot '../build/libs/nullstar-1.0.0.jar'),
    [string]$Output = (Join-Path $PSScriptRoot '../docs/evidence/artifact-validation.json')
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$artifactPath = (Resolve-Path -LiteralPath $Artifact).Path
$archive = [IO.Compression.ZipFile]::OpenRead($artifactPath)
try {
    $metadataEntry = $archive.GetEntry('fabric.mod.json')
    if ($null -eq $metadataEntry) { throw 'Missing fabric.mod.json' }
    $reader = [IO.StreamReader]::new($metadataEntry.Open())
    try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    $common = $archive.GetEntry('com/nullstar/Nullstar.class')
    $client = $archive.GetEntry('com/nullstar/client/NullstarClient.class')
    $testEntries = @($archive.Entries | Where-Object { $_.FullName -match '(^|/)test/|GameTest|ClientGameTest|nullstar-test' })
    if ($null -eq $common -or $null -eq $client) { throw 'Missing production entrypoint class' }
    $stream = $common.Open()
    try {
        $header = [byte[]]::new(8)
        $stream.ReadExactly($header, 0, 8)
        $major = [int]$header[6] * 256 + [int]$header[7]
    } finally { $stream.Dispose() }
    if ($metadata.id -ne 'nullstar' -or $metadata.depends.minecraft -ne '=26.3' -or $major -ne 69 -or $testEntries.Count -ne 0) {
        throw 'Production artifact validation failed'
    }
    if (@($metadata.entrypoints.main) -notcontains 'com.nullstar.Nullstar') { throw 'Wrong main entrypoint' }
    $result = [ordered]@{
        Version = $metadata.version
        Entrypoint = @($metadata.entrypoints.main)
        Minecraft = $metadata.depends.minecraft
        TestClasses = $testEntries.Count
        Common = $true
        Client = $true
        ClassMajor = $major
        Sha256 = (Get-FileHash -LiteralPath $artifactPath -Algorithm SHA256).Hash
        Generator = 'scripts/verify-artifact.ps1'
        Command = 'pwsh -File scripts/verify-artifact.ps1'
    }
    $json = $result | ConvertTo-Json -Depth 5
    [IO.File]::WriteAllText([IO.Path]::GetFullPath($Output), $json + [Environment]::NewLine)
    Write-Output $json
} finally { $archive.Dispose() }
