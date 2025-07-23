<?php 
# ============================================================================
# GLOBAL WEB: Control Avanzado Contenido
# ============================================================================
# Desarrollado y programado por: ACV Galaica
# Copyright (c) ACV Galaica - http://www.acvgalaica.com
# ============================================================================
# ============================================================================
# Código para recibir los datos de la aplicación que tienes ellos e insertalos en 
# la web de una forma sencilla. Los datos que se reciben son: 
# --> id_artista= Id_del artista
# --> situacion= poblacion,municipio,provincia,pais
# --> descripcion= valores viejos de fecha y/o agrupacion,que han de ser sustituidos 
# --> fecha= fecha de la actuacion
# --> poblacion= Poblacion de actuacion
# --> municipio= Municipio de actuacion
# --> provincia= Provincia de actuacion
# --> pais=  Pais de actuacion
# --> nombre_local= Local de Actuacion
# --> accion= Accion que se realizara sobre la Tabla.
# -----------> alta =  alta de una nueva actuacion
# -----------> delete = eliminar la fecha
# -----------> Mod_fecha = modificar la fecha de una actuacion
# -----------> Mod_agr = modificar la agrupacion de una actuacion
# -----------> Mod_fecha_agr = modificar la agrupacion y fecha de una actuacion
# ----------->  Modificar = modificar algún otro dato 
# -----------> Cualquier otra acutacion sería añadirlo a la base de datos 

# ============================================================================
# ============================================================================
# Sincronización 

// Guardamos la información que me entra por POST 
$archivo = "gfx/contenido.txt";
$fp = fopen($archivo, "a");
if($fp){
	fwrite($fp, "-------  ACTUALIZACIÓN ".date("d-m-Y H:i:s")."  ------------\n\n");
	foreach($_POST as $nombre_campo => $valor){
		$asignacion = "\$" . $nombre_campo . "='" . $valor . "';";
		fwrite($fp, $asignacion."\n\"");
	}
	fwrite($fp, "------- xxxxxxxxxx ------------\n\n");
	fclose($fp);
}


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
    'indicadores' => $indicadores
];
$acciones_enviar = ['Modificar', 'alta', 'delete', 'Mod_fecha_agr', 'Mod_agr', 'Mod_fecha'];
if (in_array($accion, $acciones_enviar)) {
    enviarDatosAGestmusica($datos_envio, $archivo);
}
registrarResultado($error ?? '', $correcto ?? '', $archivo);

