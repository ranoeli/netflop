# Teste la connexion Xtream avec plusieurs User-Agent, a partir du lien M3U copie dans le presse-papiers.
# N'affiche que les codes de reponse, jamais le lien ni les identifiants.
Add-Type -AssemblyName System.Web
$link = [Uri](Get-Clipboard -Raw).Trim()
$q = [System.Web.HttpUtility]::ParseQueryString($link.Query)
$api = "$($link.Scheme)://$($link.Authority)/player_api.php?username=$([Uri]::EscapeDataString($q['username']))&password=$([Uri]::EscapeDataString($q['password']))"

$agents = [ordered]@{
    'Appli actuelle'   = 'Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36'
    'IPTV Smarters'    = 'IPTVSmartersPro'
    'okhttp'           = 'okhttp/4.12.0'
    'TiviMate'         = 'TiviMate/5.1.6 (Android 11)'
    'VLC'              = 'VLC/3.0.20 LibVLC/3.0.20'
    'Aucun'            = ''
}
foreach ($name in $agents.Keys) {
    try {
        $r = Invoke-WebRequest -Uri $api -UserAgent $agents[$name] -UseBasicParsing -TimeoutSec 20
        $auth = if ($r.Content -match '"auth"\s*:\s*1') { 'connexion OK' } else { 'reponse sans auth' }
        "{0,-16} HTTP {1}  {2}" -f $name, [int]$r.StatusCode, $auth
    } catch {
        $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { $_.Exception.Message }
        "{0,-16} HTTP {1}" -f $name, $code
    }
}

# Memes en-tetes que la bibliotheque reseau de l'appli (OkHttp)
try {
    $h = @{ 'Accept-Encoding' = 'gzip' }
    $r = Invoke-WebRequest -Uri $api -UserAgent $agents['Appli actuelle'] -Headers $h -UseBasicParsing -TimeoutSec 20
    "{0,-16} HTTP {1}" -f 'Comme l''appli', [int]$r.StatusCode
} catch {
    $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { $_.Exception.Message }
    "{0,-16} HTTP {1}" -f 'Comme l''appli', $code
}

# Caracteres speciaux dans les identifiants (peuvent etre mal transmis a l'emulateur)
$special = ($q['username'] + $q['password']) -replace '[A-Za-z0-9]', ''
"Caracteres speciaux dans identifiant/mot de passe : " + $(if ($special) { $special } else { 'aucun' })
