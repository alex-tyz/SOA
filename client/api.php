<?php
declare(strict_types=1);

header('Content-Type: application/xml; charset=UTF-8');
header('Cache-Control: no-store');

function fail(int $status, string $message): never
{
    http_response_code($status);
    echo '<error><code>' . $status . '</code><message>'
        . htmlspecialchars($message, ENT_XML1 | ENT_QUOTES, 'UTF-8')
        . '</message></error>';
    exit;
}

$service = $_GET['service'] ?? null;
$path = $_GET['path'] ?? null;
$query = $_GET['query'] ?? '';
$transportMethod = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$override = $_GET['method'] ?? null;
if ($override !== null && ($transportMethod !== 'POST'
    || !is_string($override) || !in_array($override, ['PUT', 'DELETE'], true))) {
    fail(400, 'Некорректный метод запроса');
}
$method = $override ?? $transportMethod;

if (!is_string($service) || !is_string($path) || !is_string($query)
    || strlen($query) > 4096 || preg_match('/[\x00-\x1f#]/', $query)) {
    fail(400, 'Некорректный адрес запроса');
}

if ($service === 'marine') {
    $base = 'https://localhost:8181/api/v1';
    $peer = 'localhost';
    $certificate = __DIR__ . '/.certs/spacemarine.crt';
    $allowed = preg_match('#^/space-marines(?:/(?:[^/]+|by-weapon-type/[^/]+|achievements/unique))?$#', $path)
        && !str_contains($path, '..')
        && in_array($method, ['GET', 'POST', 'PUT', 'DELETE'], true);
} elseif ($service === 'starship') {
    $base = 'https://127.0.0.1:21138/api/v1';
    $peer = 'helios.cs.ifmo.ru';
    $certificate = __DIR__ . '/.certs/starship.crt';
    $allowed = preg_match('#^/starship/[^/]+/(?:load/[^/]+|unload-all)$#', $path)
        && !str_contains($path, '..') && $method === 'POST';
} else {
    fail(404, 'Неизвестный сервис');
}

if (!$allowed) {
    fail(404, 'Неизвестная операция');
}

$headers = "Accept: application/xml\r\n";
$body = file_get_contents('php://input');
if ($body === false) {
    fail(400, 'Не удалось прочитать запрос');
}
if ($body !== '') {
    $headers .= "Content-Type: application/xml\r\n";
}

$url = $base . $path . ($query === '' ? '' : '?' . $query);
$context = stream_context_create([
    'http' => [
        'method' => $method,
        'header' => $headers,
        'content' => $body,
        'ignore_errors' => true,
        'follow_location' => 0,
        'timeout' => 10,
    ],
    'ssl' => [
        'verify_peer' => true,
        'verify_peer_name' => true,
        'peer_name' => $peer,
        'SNI_enabled' => true,
        'SNI_server_name' => $peer,
        'cafile' => $certificate,
    ],
]);

$response = @file_get_contents($url, false, $context);
if ($response === false) {
    fail(502, 'Локальный сервис недоступен');
}

$status = 502;
foreach ($http_response_header ?? [] as $line) {
    if (preg_match('#^HTTP/\S+\s+(\d{3})#', $line, $match)) {
        $status = (int) $match[1];
    }
}
http_response_code($status);
echo $response;
