<?php
$datos = [
    'id_artista' => '287',
    'accion' => 'Mod_agr',
    'fecha' => '15-09-2025',
    'descripcion' => '286*Sevilla*Dos Hermanas*Sala Central',
    'poblacion' => 'Sevilla',
    'municipio' => 'Dos Hermanas',
    'provincia' => 'Sevilla',
    'pais' => 'España',
    'nombre_local' => 'Sala Central',
    'estado' => 'R',
    'indicadores' => '010'
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
    echo "Mod_agr => HTTP $status => Respuesta: $response\n";
}
?>
