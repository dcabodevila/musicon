<?php
$datos = [
    'id_artista' => '287',
    'accion' => 'Mod_fecha',
    'fecha' => '20-09-2025',
    'descripcion' => '15-09-2025*Sevilla*Dos Hermanas*Sala Alameda',
    'poblacion' => 'Sevilla',
    'municipio' => 'Dos Hermanas',
    'provincia' => 'Sevilla',
    'pais' => 'España',
    'nombre_local' => 'Sala Alameda',
    'estado' => 'O',
    'indicadores' => '111'
];

send($datos);

function send($datos) {
    $urlApi = 'http://localhost:8081/api/gestmanager/publicar';
    $ch = curl_init($urlApi);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($datos));
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    echo "Mod_fecha => HTTP $status => Respuesta: $response\n";
}
?>
