# Verifies every Play listing field is inside its limit.
#
# Play truncates silently in some surfaces and rejects in others, and the counts
# are easy to break with a one-word edit months from now. Run this after any
# change to docs/16-store-listing.md.
#
#   powershell -File tools/check-listing-lengths.ps1

$ErrorActionPreference = 'Stop'
$doc = Join-Path $PSScriptRoot '..\docs\16-store-listing.md'

# 🔴 -Encoding UTF8 is not optional. Windows PowerShell 5.1 reads BOM-less files
# as the system ANSI codepage, which decodes the em dash in the title (U+2014,
# three UTF-8 bytes) as three separate characters. That reported the 29-character
# title as 31 and "failed" a field that was always fine — and the same mis-read
# in an editor is how "StockCut — ..." reaches Play Console as "StockCut â€" ...".
$text = Get-Content $doc -Raw -Encoding UTF8

# Each fenced block in the doc, in order: title, short description, full description.
$blocks = [regex]::Matches($text, '(?s)```\r?\n(.*?)```') | ForEach-Object { $_.Groups[1].Value.TrimEnd("`r", "`n") }

$fields = @(
    @{ Name = 'App name';          Limit = 30;   Value = $blocks[0] }
    @{ Name = 'Short description'; Limit = 80;   Value = $blocks[1] }
    @{ Name = 'Full description';  Limit = 4000; Value = $blocks[2] }
)

$failed = $false
foreach ($f in $fields) {
    $len = $f.Value.Length
    $ok = $len -le $f.Limit
    if (-not $ok) { $failed = $true }
    $mark = if ($ok) { 'OK  ' } else { 'OVER' }
    Write-Output ("{0} {1,-18} {2,5} / {3}" -f $mark, $f.Name, $len, $f.Limit)
}

if ($failed) {
    Write-Output ''
    Write-Output 'A field is over its limit. Play will reject or silently truncate it.'
    exit 1
}
Write-Output ''
Write-Output 'All listing fields are within their limits.'
