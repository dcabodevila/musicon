<?php
$datos = [
    'id_artista' => '287',
    'accion' => 'Modificar',
    'fecha' => '23-08-2025',
    'descripcion' => 'Sevilla*Dos Hermanas*Sala Antigua',
    'poblacion' => 'Sevilla',
    'municipio' => 'Dos Hermanas',
    'provincia' => 'Sevilla',
    'pais' => 'España',
    'nombre_local' => 'Sala Alameda',
    'estado' => 'R',
    'indicadores' => '000'
];

send($datos);

function send($datos) {
    $urlApi = 'http://localhost:8081/api/gestmanager/publicar';
    $ch = curl_init($urlApi);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($datos));
    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    echo "Modificar => HTTP $status => Respuesta: $response\n";
}
?>
