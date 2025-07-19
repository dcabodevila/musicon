<?php
// Datos simulados que normalmente enviaría Gestmanager
$datos = [
    'id_artista' => '1234',
    'accion' => 'alta',
    'fecha' => '19-07-2025',
    'descripcion' => 'proba descricion',
    'poblacion' => 'Prueba poblacion',
    'municipio' => 'Lugo',
    'provincia' => 'Lugo',
    'pais' => 'España',
    'nombre_local' => 'Sala Prueba',
    'estado' => 'O',
    'indicadores' => '101'
];

// URL de tu API Spring Boot en local
$urlApi = 'http://localhost:8081/api/gestmanager/publicar';

// Configurar cURL para enviar como JSON (igual que relay)
$ch = curl_init($urlApi);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json'
]);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($datos));

// Ejecutar la petición
$response = curl_exec($ch);
$http_status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

// Mostrar el resultado
echo "Respuesta del API: " . htmlspecialchars($response) . "<br>";
echo "HTTP Status: " . $http_status . "<br>";
?>
