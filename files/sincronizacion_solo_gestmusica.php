<?php
// Forzar que toda salida sea UTF-8 (importante para caracteres en español)
header('Content-Type: text/plain; charset=utf-8');

// Archivo para registro de logs
$archivo = "logs.txt";
$fp = fopen($archivo, "a");

if ($fp) {
    fwrite($fp, "-------  ACTUALIZACIÓN " . date("d-m-Y H:i:s") . "  ------------\n\n");
    foreach ($_POST as $nombre_campo => $valor) {
        $asignacion = "\$" . $nombre_campo . "='" . $valor . "';";
        fwrite($fp, $asignacion . "\n");
    }
    fwrite($fp, "------- xxxxxxxxxx ------------\n\n");
    fclose($fp);
}

function enviarDatosAGestmusica($datos_envio, $archivo) {
    $datos_json = json_encode($datos_envio);
    $url_api = "https://festia.es/api/gestmanager/publicar";
    $ch = curl_init($url_api);

    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $datos_json);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
    ]);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

    $response = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);

    if (curl_errno($ch)) {
        echo "ERROR: Fallo en conexión CURL - " . curl_error($ch);
        curl_close($ch);
        return;
    }

    if ($http_code !== 200) {
        echo "ERROR: Respuesta HTTP " . $http_code . " - " . $response;
        curl_close($ch);
        return;
    }

    if (empty($response)) {
        echo "ERROR: Respuesta vacía del servidor.";
    } else {
        echo "CORRECTO: " . trim($response);
    }

    curl_close($ch);
}

$id_artista = $_POST['id_artista'] ?? '';
$accion = $_POST['accion'] ?? '';
$fecha = $_POST['fecha'] ?? '';
$descripcion = $_POST['descripcion'] ?? '';
$poblacion = $_POST['poblacion'] ?? '';
$municipio = $_POST['municipio'] ?? '';
$provincia = $_POST['provincia'] ?? '';
$pais = $_POST['pais'] ?? '';
$nombre_local = $_POST['nombre_local'] ?? '';
$estado = $_POST['estado'] ?? '';
$indicadores = $_POST['indicadores'] ?? '';

$datos_envio = [
    'id_artista' => $id_artista,
    'accion' => $accion,
    'fecha' => $fecha,
    'descripcion' => $descripcion,
    'poblacion' => $poblacion,
    'municipio' => $municipio,
    'provincia' => $provincia,
    'pais' => $pais,
    'nombre_local' => $nombre_local,
    'estado' => $estado,
    'indicadores' => $indicadores,
];

$acciones_enviar = ['Modificar', 'alta', 'delete', 'Mod_fecha_agr', 'Mod_agr', 'Mod_fecha'];

if (in_array($accion, $acciones_enviar)) {
    enviarDatosAGestmusica($datos_envio, $archivo);
} else {
    echo "CORRECTO: Accion no aceptada, pero registrada por el sistema.";
}