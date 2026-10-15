param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$results = [System.Collections.Generic.List[object]]::new()
function Check-Http {
    param([string]$Path, [int]$Expected = 200, [string]$Method = 'GET', $Body = $null, [string]$Token = '')
    $options = @{ Uri = "$BaseUrl$Path"; Method = $Method; UseBasicParsing = $true; TimeoutSec = 15 }
    if ($null -ne $Body) { $options.ContentType = 'application/json'; $options.Body = ($Body | ConvertTo-Json -Compress) }
    if ($Token) { $options.Headers = @{ Authorization = "Bearer $Token" } }
    $response = $null
    try { $response = Invoke-WebRequest @options; $actual = [int]$response.StatusCode }
    catch {
        if ($_.Exception.Response) { $actual = [int]$_.Exception.Response.StatusCode }
        else { $actual = 0 }
    }
    $results.Add([pscustomobject]@{ Method = $Method; Path = $Path; Expected = $Expected; Actual = $actual; Passed = ($actual -eq $Expected) })
    if ($response -and $response.Headers['Content-Type'] -match 'application/json') { return ($response.Content | ConvertFrom-Json) }
}
$staticRoot = Join-Path $PSScriptRoot 'src/main/resources/static'
Check-Http '/' | Out-Null
Get-ChildItem $staticRoot -Recurse -File | ForEach-Object {
    $path = '/' + $_.FullName.Substring($staticRoot.Length + 1).Replace('\', '/')
    Check-Http $path | Out-Null
}
Check-Http '/api/categories' | Out-Null
$publicEvents = @(Check-Http '/api/events/public')
Check-Http '/api/users/profile' -Expected 403 | Out-Null
Check-Http '/api/admin/users' -Expected 403 | Out-Null
Check-Http '/api/auth/login' -Method POST -Body @{usernameOrEmail = 'admin'; password = 'invalid-test-password'} -Expected 401 | Out-Null
$login = Check-Http '/api/auth/login' -Method POST -Body @{usernameOrEmail = 'admin'; password = '123456'}
if ($login.accessToken) {
    foreach ($path in @('/api/users/profile', '/api/admin/users', '/api/admin/events', '/api/reports/admin/dashboard', '/api/reports/admin/charts')) {
        Check-Http $path -Token $login.accessToken | Out-Null
    }
    foreach ($event in ($publicEvents | Select-Object -First 3)) {
        foreach ($path in @("/api/events/public/$($event.id)", "/api/events/public/$($event.id)/seats", "/api/feedbacks/event/$($event.id)")) {
            Check-Http $path | Out-Null
        }
        foreach ($kind in @('attendance', 'financial', 'feedback')) {
            Check-Http "/api/reports/event/$($event.id)/$kind" -Token $login.accessToken | Out-Null
        }
    }
}
$results | Format-Table -AutoSize
$failed = @($results | Where-Object { -not $_.Passed }).Count
$report = [pscustomobject]@{ Timestamp = (Get-Date -Format o); BaseUrl = $BaseUrl; Total = $results.Count; Failed = $failed; Results = $results }
$report | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $PSScriptRoot 'target/local-smoke-results.json') -Encoding UTF8
Write-Output "HTTP checks: $($results.Count), failures: $failed"
if ($failed -gt 0) { exit 1 }
