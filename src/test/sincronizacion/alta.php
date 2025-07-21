<?php
// Datos simulados que normalmente enviaría Gestmanager
$datos_envio = [
    'id_artista' => '1234',
    'accion' => 'alta',
    'fecha' => '19-07-2025',
    'descripcion' => '',
    'poblacion' => 'Prueba poblacion',
    'municipio' => 'Lugo',
    'provincia' => 'Lugo',
    'pais' => 'España',
    'nombre_local' => 'Sala Prueba',
    'estado' => 'O',
    'indicadores' => '101'
];
$archivo = "gfx/contenido.txt";
// ======================
// Módulo desacoplado de salida y API REST
// ======================
function registrarResultado($error, $correcto, $archivo) {
    $fp = fopen($archivo, "a");
    if ($fp) {
        if (!empty($error)) {

            $salida = "ERROR:".$error."\n";
        } else {
            $salida = "CORRECTO:".$correcto."\n";
        }
        fwrite($fp, date("d-m-Y H:i:s")." -> ".$salida);
        fclose($fp);
    }
}

function enviarDatosAGestmusica($datos_envio, $archivo) {
    $datos_json = json_encode($datos_envio);
    $url_api = "https://gestmusica.onrender.com/api/gestmanager/publicar";
    $ch = curl_init($url_api);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $datos_json);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json'
    ]);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);


    $response = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $fp = fopen($archivo, "a");
    if (curl_errno($ch)) {
        $mensaje_api = "Error API Gestmusica: ".curl_error($ch);
    } elseif ($http_code !== 200) {
        $mensaje_api = "Error API Gestmusica: HTTP ".$http_code."\nRespuesta: ".$response;
    } else {
        $mensaje_api = "Enviado a Gestmusica correctamente. Respuesta: ".$response;
    }
    if ($fp) {
        fwrite($fp, date("d-m-Y H:i:s")." -> ".$mensaje_api."\n");
        fclose($fp);
    }
    echo "\n".$mensaje_api;
    curl_close($ch);
}
registrarResultado($error ?? '', $correcto ?? '', $archivo);

// ======================
// Ejecución del módulo desacoplado
// ======================

enviarDatosAGestmusica($datos_envio, $archivo);

?>
