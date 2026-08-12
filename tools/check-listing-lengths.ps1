# Verifies the Play listing copy is within limits AND shaped correctly.
#
#   powershell -File tools/check-listing-lengths.ps1
#
# Play truncates silently in some surfaces and rejects in others, and the copy is
# easy to break with a one-word edit months from now. This checks four things,
# each of which has already gone wrong at least once:
#
#   1. LENGTH   - every field inside its limit.
#   2. LAYOUT   - the full description is not hard-wrapped. Play preserves the
#                 line breaks you paste, so a description wrapped at 80 columns
#                 by an editor renders with ragged breaks mid-sentence on a
#                 phone. Length is not layout, and the length check passed
#                 happily while the copy was unreadable.
#   3. HEADINGS - the "N / M" written in each heading matches the real count.
#                 Those numbers were hand-maintained and drifted: the short
#                 description read "79 / 80" when it was 76, and the full
#                 description read "2,038" when it was 2,193.
#   4. ENCODING - no mojibake. The em dashes in the copy are U+2014; a tool that
#                 reads the file as Windows ANSI turns each into two junk chars.
#
# 🔴 THIS FILE IS DELIBERATELY PURE ASCII. Windows PowerShell 5.1 reads a .ps1
# without a BOM as the system ANSI codepage, so a literal em dash or bullet in
# the SOURCE becomes mojibake and the script fails to parse. The first version
# of this rewrite did exactly that - it warned about the encoding trap in a
# comment containing the trap. Non-ASCII characters the checks need are built
# from codepoints below; do not paste them in literally.
#
# 🔴 Set-StrictMode is load-bearing. Without it, an absent fenced block yields
# $null, $null.Length is 0, and 0 -le 4000 is $true - so the script printed
# "OK  Full description  0 / 4000" and exited 0 at exactly the moment the
# document had broken. A guard that passes when its input is missing is worse
# than no guard, because it is trusted.

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$doc = Join-Path $PSScriptRoot '..\docs\16-store-listing.md'

# 🔴 -Encoding UTF8 is not optional, for the same reason as above: the markdown
# has no BOM, and read as ANSI a 29-character title measures 31.
$lines = Get-Content $doc -Encoding UTF8

$BULLET  = [char]0x2022  # the bullet used in the description
$EMDASH  = [char]0x2014  # the dash used in the headings
$MOJIBAKE = [string]([char]0x00E2) + [string]([char]0x20AC)  # UTF-8 read as ANSI
$REPLACEMENT = [char]0xFFFD

$fields = @(
    @{ Name = 'App name';          Heading = '## App name';          Limit = 30;   CheckWrap = $false }
    @{ Name = 'Short description'; Heading = '## Short description'; Limit = 80;   CheckWrap = $false }
    @{ Name = 'Full description';  Heading = '## Full description';  Limit = 4000; CheckWrap = $true }
)

# Anchored to its heading, NOT to ordinal position. Positional lookup
# ($blocks[0..2]) meant inserting any fenced example earlier in the document
# silently measured the wrong text against each limit - a 4000-character body
# against the 80-character short-description limit, or a title against 4000.
function Get-BlockAfterHeading {
    param([string[]] $Lines, [string] $Heading)

    $start = -1
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i].StartsWith($Heading)) { $start = $i; break }
    }
    if ($start -lt 0) { return $null }

    $open = -1
    for ($i = $start + 1; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i].Trim() -eq '```') { $open = $i; break }
    }
    if ($open -lt 0) { return $null }

    $close = -1
    for ($i = $open + 1; $i -lt $Lines.Count; $i++) {
        if ($Lines[$i].Trim() -eq '```') { $close = $i; break }
    }
    if ($close -lt 0) { return $null }

    return [PSCustomObject]@{
        HeadingLine = $Lines[$start]
        Body        = ($Lines[($open + 1)..($close - 1)] -join "`n").TrimEnd()
    }
}

$script:failed = $false
function Fail([string] $Message) {
    Write-Output "FAIL $Message"
    $script:failed = $true
}

foreach ($f in $fields) {
    $block = Get-BlockAfterHeading -Lines $lines -Heading $f.Heading
    if ($null -eq $block) {
        Fail "$($f.Name): no fenced block found under '$($f.Heading)'."
        continue
    }

    $text = $block.Body
    $len = $text.Length

    if ($len -eq 0) {
        Fail "$($f.Name): the fenced block is empty."
        continue
    }

    # 1. Length
    if ($len -gt $f.Limit) {
        Fail ("{0}: {1} characters, limit {2}. Play will reject or truncate it." -f $f.Name, $len, $f.Limit)
    } else {
        Write-Output ("OK   {0,-18} {1,5} / {2}" -f $f.Name, $len, $f.Limit)
    }

    # 3. The count written in the heading must match reality.
    $countPattern = '(' + [regex]::Escape($EMDASH) + '|-)\s*([\d,]+)\s*/\s*([\d,]+)\s*$'
    $m = [regex]::Match($block.HeadingLine, $countPattern)
    if (-not $m.Success) {
        Fail "$($f.Name): heading does not end with a 'N / M' count: '$($block.HeadingLine)'"
    } else {
        $claimed = [int]($m.Groups[2].Value -replace ',', '')
        $limit = [int]($m.Groups[3].Value -replace ',', '')
        if ($claimed -ne $len) {
            Fail ("{0}: heading claims {1} characters but the text is {2}. Update the heading." -f $f.Name, $claimed, $len)
        }
        if ($limit -ne $f.Limit) {
            Fail ("{0}: heading claims a limit of {1}, but Play's limit is {2}." -f $f.Name, $limit, $f.Limit)
        }
    }

    # 2. Layout - no hard wrapping.
    # Every non-blank line that follows another non-blank line must begin a new
    # item (a bullet or a numbered step). Anything else is a continuation line,
    # which means a paragraph was wrapped and will render broken on a phone.
    if ($f.CheckWrap) {
        $itemPattern = '^(' + [regex]::Escape($BULLET) + '|\d+\.)\s'
        $body = $text -split "`n"
        for ($i = 1; $i -lt $body.Count; $i++) {
            $prev = $body[$i - 1].Trim()
            $cur = $body[$i].Trim()
            if ($prev -ne '' -and $cur -ne '' -and $cur -notmatch $itemPattern) {
                Fail ("{0}: line {1} continues the line above it - the text is hard-wrapped." -f $f.Name, ($i + 1))
                Write-Output "       ends: ...$($prev.Substring([Math]::Max(0, $prev.Length - 40)))"
                Write-Output "       next: $($cur.Substring(0, [Math]::Min(40, $cur.Length)))..."
                Write-Output "     Play preserves your line breaks. Join each paragraph onto one line."
                break
            }
        }
    }

    # 4. Encoding
    if ($text.Contains($REPLACEMENT) -or $text.Contains($MOJIBAKE)) {
        Fail "$($f.Name): contains mojibake - this file was read or written as ANSI instead of UTF-8."
    }
}

if ($script:failed) {
    Write-Output ''
    Write-Output 'Listing copy is not ready to paste.'
    exit 1
}
Write-Output ''
Write-Output 'All listing fields are within limits, correctly counted, and unwrapped.'
