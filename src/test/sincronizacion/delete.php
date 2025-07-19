<?php
$datos = [
    'id_artista' => '1234',
    'accion' => 'delete',
    'fecha' => '19-07-2025',
    'descripcion' => 'Lugo*Lugo*Sala Prueba',
    'poblacion' => 'Prueba poblacion',
    'municipio' => 'Lugo',
    'provincia' => 'Lugo',
    'pais' => 'España',
    'nombre_local' => 'Sala Prueba',
    'estado' => 'O',
    'indicadores' => '000'
];

send($datos);

function send($datos) {
    $urlApi = 'https://gestmusica.onrender.com/api/gestmanager/publicar';
    $ch = curl_init($urlApi);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($datos));
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    echo "Delete => HTTP $status => Respuesta: $response\n";
}
?>
