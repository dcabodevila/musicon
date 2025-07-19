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

require_once("config.php");
require_once("funciones.php");

/*Funcion que valida una fecha*/
function fecha_valida($fecha){
	if($fecha[2] == '-' && $fecha[5] == '-' && strlen($fecha) == 10 ){ 
		/*Formato correcto */
		list($dia,$mes,$ano) = explode("-",$fecha);
		
		if($mes > 0 && $mes < 13){
			if(($ano % 4 == 0 && $ano % 100 != 0) || $ano % 400 == 0){
			/*Año bisiesto*/
				if($mes == 2 || $mes == 02){
					if($dia > 0 && $dia < 30){
						return true;					
					}else{
						return false;					
					}
				}else{
					if($mes == 1 || $mes == 3 || $mes == 5 || $mes == 7 || $mes == 8 || $mes == 01 || $mes == 03 || $mes == 05 || $mes == 07 || $mes == 08 || $mes == 10 || $mes == 12 ){
						if($dia > 0 && $dia < 32){
							return true;						
						}else{
							return false;						
						}
					}else{
						if($dia > 0 && $dia < 31){
							return true;
						}else{
							return false;						
						}
					}
				}	
			}else{
			/*No es año bisiesto */
				if($mes == 2 || $mes == 02){
					if($dia > 0 && $dia < 29){
						return true;
					}else{
						return false;
					}
				}else{
					if($mes == 1 || $mes == 3 || $mes == 5 || $mes == 7 || $mes == 8 || $mes == 10 || $mes == 12 ){
						if($dia > 0 && $dia < 32){
							return true;
						}else{
							return false;						
						}
					}else{
						if($dia > 0 && $dia < 31){
							return true;						
						}else{
							return false; 
						}
					}
				}	
			}
		}else{
			return false;
			
		}
	}else{
		/*Formato incorrecto*/
		return false;
	}
}

/*Funcion que valida la hora */
function hora_correcta($hora){
	if($hora != ''){
		list($horas,$minutos) = explode(":",$hora);
		if($minutos == '00' || $minutos == '15' || $minutos == '30' || minutos == '45'){
			if($hora >= 0 && $hora < 25){
				return true; 
			}else{
				return false; 
			}
		}else{
			return false; 
		}
	}else{
		return true;
	}
}

//Funcion que valida si existe una provincia. Si no existe la crea
function validar_provincia($provincia){
global $pref, $db; 

	$query = "SELECT * FROM ".$pref."_netmanager_provincias"; 
	$resultado = $db->sql_query($query); 
	
	while($row = $db->sql_fetchrow($resultado)){
		if(strtolower($provincia) == strtolower($row['provincia'])){
			$id_provincia = $row['id_provincia']; 
			break; 
		}
	} // fin del while
	
	if($id_provincia == '' ){
		// Creamos la provincia
		$query = "INSERT INTO ".$pref."_netmanager_provincias (provincia) VALUES ('".$provincia."')"; 
		$db->sql_query($query); 
		$id_provincia = $db->sql_nextid();
	}
	
	return $id_provincia; 
	
}

//Funcion que valida si existe una provincia.
function id_provincia($provincia){
global $pref, $db; 

	$query = "SELECT * FROM ".$pref."_netmanager_provincias"; 
	$resultado = $db->sql_query($query); 
	
	while($row = $db->sql_fetchrow($resultado)){
		if(strtolower($provincia) == strtolower($row['provincia'])){
			$id_provincia = $row['id_provincia']; 
			break; 
		}
	} // fin del while	
		
	return $id_provincia; 
	
}

// Funcion que comprueba si existe un municipio. Si no existe lo crea
function validar_municipio($municipio, $id_provincia){
global $pref, $db; 

	$query = "SELECT * FROM ".$pref."_netmanager_ayuntamientos ";
	$resultado = $db->sql_query($query); 
	
	while($row = $db->sql_fetchrow($resultado)){
		if(strtolower($municipio) == strtolower($row['ayuntamiento'])){
			if($id_provincia == $row['id_provincia']){
				$id_ayuntamiento = $row['ayuntamiento']; 
				break; 
			}else{
				$id_ayuntamiento = 'Undefined'; 
			}
			
		}
	} // fin del while
	
	
	if($id_ayuntamiento == '' || $id_ayuntamiento == 'Undefined'){
		// Creamos la provincia
		$query = "INSERT INTO ".$pref."_netmanager_ayuntamientos (ayuntamiento, id_provincia) VALUES ('".$municipio."', '".$id_provincia."')"; 
		$db->sql_query($query); 
		$id_ayuntamiento = $municipio;
	}
	
	return $id_ayuntamiento; 	
}


// Funcion que indica si existe o no un artista
function existe_artista($id_artista){
global $pref, $db; 

	$query = "SELECT id_artista FROM ".$pref."_netmanager_artistas WHERE id_artista = '".$id_artista."'"; 
	$resultado = $db->sql_query($query); 	
	if($db->sql_numrows($resultado) > 0 ){
		return true; 
	}else{
		return false; 
	}
}


// Funcion que indicar los numero de días del mes 
function diasMes($anho,$mes){
   if (((fmod($anho,4)==0) and (fmod($anho,100)!=0)) or (fmod($anho,400)==0)) {
       $dias_febrero = 29;
   } else {
       $dias_febrero = 28;
   }
   switch($mes) {
       case 1: return 31; break;
       case 2: return $dias_febrero; break;
       case 3: return 31; break;
       case 4: return 30; break;
       case 5: return 31; break;
       case 6: return 30; break;
       case 7: return 31; break;
       case 8: return 31; break;
       case 9: return 30; break;
       case 10: return 31; break;
       case 11: return 30; break;
       case 12: return 31; break;
   }
} 
$correcto = ''; 
$error = ''; 


if(isset($_POST['id_artista']) && $_POST['id_artista'] != '' ){
	
	if(existe_artista($_POST['id_artista'])){
		// Existe el artista 
		switch($_POST['accion']){
			case "alta": 			
				// Damos de alta un nuevo evento 
				if(isset($_POST['fecha']) && $_POST['fecha'] != '' && fecha_valida($_POST['fecha']) ){					
					// Comprobamos la provincia y el municipio 
					if($_POST['provincia'] != '.      .'){ 
						$id_provincia = validar_provincia($_POST['provincia']); 
					}else{
						$id_provincia = ''; 
					}
					
					if($_POST['municipio'] != '.      .' && $id_provincia != '' ){ 
						$id_ayuntamiento = validar_municipio($_POST['municipio'], $id_provincia); 
					}else{
						$id_ayuntamiento = ''; 
					}
					// Controlamos que el usuario meta algún valor - Si no inbtroduce nada o datos incorrectos tomaremos como valor por defecto 0
					$indicadores = $_POST['indicadores'];
					// MT
					$mt = $indicadores[0];
					if ($indicadores[0] != 0 and $indicadores[0] != 1)
					{
						$mt = 0;
					}
					// SMP
					$smt = $indicadores[1];
					if ($indicadores[1] != 0 and $indicadores[1] != 1)
					{
						$smt = 0;
					}
					// SF
					$sf = $indicadores[2];
					if ($indicadores[2] != 0 and $indicadores[2] != 1)
					{
						$sf = 0;
					}
						
					
					
					if($id_ayuntamiento != 'Undefined'){
						$query = "INSERT INTO ".$pref."_netmanager_ocupaciones ( id_artista, fecha, id_provincia, estado, ayuntamiento, lugar , poblacion, observaciones, pais,mt,smt,sf) VALUES ('".$_POST['id_artista']."', '".ver_fecha_sql($_POST['fecha'])."', '".$id_provincia."', 'ocupado', '".$id_ayuntamiento."','".$_POST['nombre_local']."', '".$_POST['poblacion']."', '', '".$_POST['pais']."', '".$mt."', '".$smt."' , '".$sf."')"; 
						/*$query = "INSERT INTO ".$pref."_netmanager_ocupaciones ( id_artista, fecha, id_provincia, estado, ayuntamiento, lugar , poblacion, observaciones, pais) VALUES ('".$_POST['id_artista']."', '".ver_fecha_sql($_POST['fecha'])."', '".$id_provincia."', 'ocupado', '".$id_ayuntamiento."','".$_POST['nombre_local']."', '".$_POST['poblacion']."', '', '".$_POST['pais']."' )";*/
						$db->sql_query($query); 
						$correcto = "El evento ha sido a&ntilde;adido correctamente"; 
					}else{
						$error = "El ayuntamiento y provincia son incorrectos";
					}
				}else{
					if(isset($_POST['fecha']) && $_POST['fecha'] == ''){
						$error = "La fecha del evento est&aacute; vac&iacute;o";
					}else{
						if(!fecha_valida($_POST['fecha'])){
							$error = "La fecha del evento es incorrecta";
						}else{
							$error = "Hora incorrecta";
						}
					} 
				}
			break; 
			
			case "delete": 
				// Eliminamos un evento de la base de datos 				
				list($provincia,$poblacion,$local ) = explode("*",$_POST['descripcion']); 
				
				if($provincia == ".      ."){
					$id_provincia = '0'; 
				}else{
					$id_provincia = id_provincia($provincia); 
				}
				
				$poblacion = trim($poblacion); 
				if($id_provincia != '' ){					
					$query = "SELECT id_ocupacion FROM ".$pref."_netmanager_ocupaciones WHERE id_artista = '".$_POST['id_artista']."' AND fecha = '".ver_fecha_sql($_POST['fecha'])."' "; 
					if($local == ".      ."){
						$query .= " AND lugar = '."; 
						for($i = 0; $i < 5 ; $i++){
							$query .= " ";
						}
						$query .= " .'";  
					}else{
						$query .= " AND lugar = '".$local."'"; 
					}
					
					if($poblacion == ".      ."){
						$query .= " AND poblacion = '."; 
						for($i = 0; $i < 5 ; $i++){
							$query .= " ";
						}
						$query .= " .'"; 
					}else{
						$query .= " AND poblacion = '".$poblacion."'"; 
					}
					
					$query .= "  AND id_provincia = '".$id_provincia."' ";				
					
					$resultado = $db->sql_query($query);
					if($db->sql_numrows($resultado) > 0 ){
						$row = $db->sql_fetchrow($resultado); 
						// Borramos la actuacion
						$query_delete = "DELETE FROM ".$pref."_netmanager_ocupaciones WHERE id_ocupacion = '".$row['id_ocupacion']."'"; 
						$db->sql_query($query_delete);					
						
						$correcto = "El evento ha sido eliminado correctamente";
					}else{
						$error = "No existe ning&uacute;n evento con esas caracter&iacute;sticas";
					}
				}else{
					$error = "No existe dicha provincia en la base de datos"; 
				}
			break; 
			
			case "Mod_fecha": 
				// Modificamos la fecha de un evento 	
				
				if(isset($_POST['fecha']) && $_POST['fecha'] != '' && fecha_valida($_POST['fecha']) ){
					
					list($fecha_ant,$provincia,$poblacion,$local ) = explode("*",$_POST['descripcion']); 				
					$id_provincia = id_provincia($provincia); 
					if($id_provincia != '' ){ 
						$query= "SELECT id_ocupacion FROM ".$pref."_netmanager_ocupaciones WHERE id_artista = '".$_POST['id_artista']."' AND fecha = '".ver_fecha_sql($fecha_ant)."' AND lugar = '".$local."' AND id_provincia = '".$id_provincia."' AND poblacion = '".$poblacion."'"; 
						$resultado = $db->sql_query($query); 
						if($db->sql_numrows($resultado) > 0 ){
							$row = $db->sql_fetchrow($resultado); 								
							
							//Modificamos la fecha 						
							$query_update = "UPDATE ".$pref."_netmanager_ocupaciones SET fecha = '".ver_fecha_sql($_POST['fecha'])."' WHERE id_ocupacion = '".$row['id_ocupacion']."'"; 
							$db->sql_query($query_update); 
							$correcto = "La fecha se ha modificado correctamente";
						
						}else{
							$error = "Este artista no tiene eventos para esa fecha"; 
						}
					}else{
						$error = "No existe dicha provincia en la base de datos"; 
					}
				}else{
					$error = "Falta informaci&oacute;n para realizar la accion"; 
				}
			break; 			
			
			case "Mod_agr": 
			
				list($artista_ant,$provincia,$poblacion,$local ) = explode("*",$_POST['descripcion']); 				
				$id_provincia = id_provincia($provincia); 
				// Modificar la agrupacion para una tarifa				
				if(isset($_POST['fecha']) && $_POST['fecha'] != '' && fecha_valida($_POST['fecha']) && $id_provincia != ''  ){					
					if(existe_artista($artista_ant)){
						$query= "SELECT id_ocupacion FROM ".$pref."_netmanager_ocupaciones WHERE id_artista = '".$artista_ant."' AND fecha = '".ver_fecha_sql($_POST['fecha'])."'  AND lugar = '".$local."' AND id_provincia = '".$id_provincia."' AND poblacion = '".$poblacion."'"; 
						$resultado = $db->sql_query($query); 
						if($db->sql_numrows($resultado) > 0 ){
							$row = $db->sql_fetchrow($resultado); 							
							// Quitamos la tarifa a la agrupacion anterior 
							$query_update = "UPDATE ".$pref."_netmanager_ocupaciones SET id_artista = '".$_POST['id_artista']."' WHERE id_ocupacion = '".$row['id_ocupacion']."'";
							$db->sql_query($query_update); 
							
							$correcto = "La agrupacion se ha modificado correctamente";
														
							
						}else{
							$error = "Este artista no tiene eventos para esa fecha"; 
						}
					}else{
						$error = "No existe el artista"; 
					}
				}else{
					$error = "Falta información para realizar la accion"; 
				}
			break; 
			
			case "Mod_fecha_agr":
				// Modificamos la fecha y la agrupacion para una tarifa
				if(isset($_POST['fecha']) && $_POST['fecha'] != '' && fecha_valida($_POST['fecha']) && $_POST['id_artista'] != '' && $_POST['descripcion'] != '' ){	
					list($artista_ant,$fecha_ant,$provincia,$poblacion,$local ) = explode("*",$_POST['descripcion']); 				
					$id_provincia = id_provincia($provincia); 
					
					if(existe_artista($artista_ant) && existe_artista($_POST['id_artista']) ){
						$query= "SELECT id_ocupacion FROM ".$pref."_netmanager_ocupaciones WHERE id_artista = '".$artista_ant."' AND fecha = '".ver_fecha_sql($fecha_ant)."' AND lugar = '".$local."' AND id_provincia = '".$id_provincia."' AND poblacion = '".$poblacion."'"; 
						$resultado = $db->sql_query($query); 
						if($db->sql_numrows($resultado) > 0 ){
							$row = $db->sql_fetchrow($resultado); 
							
							$query_update = "UPDATE ".$pref."_netmanager_ocupaciones SET id_artista = '".$_POST['id_artista']."', fecha = '".ver_fecha_sql($_POST['fecha'])."' WHERE id_ocupacion = '".$row['id_ocupacion']."'"; 								
							$db->sql_query($query_update);
							
							$correcto = "La agrupacion y la fecha se ha modificado correctamente";								
						}else{
							$error = "Este artista no tiene eventos para esa fecha"; 
						}
					}else{
						$error = "No existe alguno de los artistas"; 
					}
				}else{
					$error = "Falta informaci&oacute;n para realizar la accion"; 
				}
			break; 
			
			case "Modificar": 
				// Modificar los datos de una actuacion 
				
				list($provincia,$poblacion,$local ) = explode("*",$_POST['descripcion']); 				
				$id_provincia = id_provincia($provincia); 
				
				if(isset($_POST['fecha']) && $_POST['fecha'] != '' && fecha_valida($_POST['fecha']) && $_POST['id_artista'] != '' ){	
					$query= "SELECT id_ocupacion FROM ".$pref."_netmanager_ocupaciones WHERE id_artista = '".$_POST['id_artista']."' AND fecha = '".ver_fecha_sql($_POST['fecha'])."' "; 
					$resultado = $db->sql_query($query); 
					if($db->sql_numrows($resultado) > 0 ){
						$row = $db->sql_fetchrow($resultado); 
						$query_update = "UPDATE ".$pref."_netmanager_ocupaciones SET pais = '".$_POST['pais']."' "; 
						if(isset($_POST['provincia']) && $_POST['provincia'] != '' ){
							$id_provincia = validar_provincia($_POST['provincia']); 
							$query_update .= ", id_provincia = '".$id_provincia."'";
							$paso = 1; 
						}
						if(isset($_POST['municipio'])){
							if($paso == 1){
								$id_ayuntamiento = validar_municipio($_POST['municipio'],$id_provincia); 
								if($id_ayuntamiento != 'Undefined'){
									$query_update .= ", ayuntamiento = '".$id_ayuntamiento."'"; 
									$paso = 1; 
								}
							}else{
								$error = "Falta informacion para realizar la acci&oacute;n"; 
							}
						}
						
						if(isset($_POST['nombre_local']) ){
							if($paso == 1){
								$query_update .= ", lugar = '".$_POST['nombre_local']."'"; 
								$paso = 1; 
							}
						}
						
						if(isset($_POST['poblacion']) ){
							if($paso == 1){
								$query_update .= ", poblacion = '".$_POST['poblacion']."'"; 
								$paso = 1; 
							}
						}
						
						// Controlamos que el usuario meta algún valor - Si no inbtroduce nada o datos incorrectos tomaremos como valor por defecto 0
						$indicadores = $_POST['indicadores'];
						// MT
						$mt = $indicadores[0];
						if ($indicadores[0] != 0 and $indicadores[0] != 1)
							$mt = 0;
						// SMP
						$smt = $indicadores[1];
						if ($indicadores[1] != 0 and $indicadores[1] != 1)
							$smt = 0;
						// SF
						$sf = $indicadores[2];
						if ($indicadores[2] != 0 and $indicadores[2] != 1)
							$sf = 0;
							

						$query_update .= ", mt = '".$mt."' , smt = '".$smt."' ,  sf = '".$sf."' "; 

						
						$query_update .= " WHERE id_ocupacion = '".$row['id_ocupacion']."'"; 
												
						$db->sql_query($query_update);
						
						$correcto = "Los datos de la actuacion se han modificado correctamente";
						
					}else{
						$error = "Este artista no tiene eventos para esa fecha"; 
					}
					
				}else{
					$error = "Falta informaci&oacute;n para realizar la accion"; 
				}
			break;
			
			case "tarifas": 
				// Actualizar las tarifas de un artista 
				if($_POST['ano'] != '' && $_POST['mes'] != '' ){
					$i = 1; 
					while($i <= diasMes($_POST['ano'],$_POST['mes'])){
						$campo_tarifa = "D"; 
						if($i < 10){ $campo_tarifa .= "0"; } 
						$campo_tarifa .= $i; 
						
						$valor_tarifa = $_POST[$campo_tarifa]; 
						if($valor_tarifa == 'NULL'){ $valor_tarifa = ''; } 
						if($_POST['matinal'] == 'NULL'){ $valor_matinal = ''; } else { $valor_matinal = $_POST['matinal']; } 
						
						$fecha_tarifa = date("Y-m-d",mktime(12,12,12,$_POST['mes'],$i,$_POST['ano'])); 
						
						$query = "SELECT id_tarifa FROM ".$pref."_netmanager_tarifas WHERE fecha = '".$fecha_tarifa."' AND id_artista = '".$_POST['id_artista']."'";
						$resultado = $db->sql_query($query); 
						if($db->sql_numrows($resultado) > 0 ){
							$row = $db->sql_fetchrow($resutado); 
							$query_tarifa = "UPDATE ".$pref."_netmanager_tarifas SET tarifa = '".$valor_tarifa."', matinal = '".$valor_matinal."' WHERE id_tarifa = '".$row['id_tarifa']."'"; 							
						}else{
							$query_tarifa = "INSERT INTO ".$pref."_netmanager_tarifas (id_artista, fecha, tarifa, matinal ) VALUES ('".$_POST['id_artista']."', '".$fecha_tarifa."', '".$valor_tarifa."', '".$valor_matinal."')";							
						}
						//echo $query_tarifa."<br>"; 
						$db->sql_query($query_tarifa); 
						$i++;
					} // Fin del while
					$correcto = "Se han actualizado las tarifas correctamente"; 
				}else{
					$error = "Falta información para realizar la acción"; 
				}
			break; 
			
			case "vaciar": 
				$vector_tablas = explode("*",$_POST['descripcion']); 
				$aux = "Las tablas: "; 
				for($i = 0; $i < count($vector_tablas); $i++){
					$query = "TRUNCATE TABLE ".$pref."_netmanager_".$vector_tablas[$i]; 
					if($db->sql_query($query)){
						$aux .= $vector_tablas[$i]; 
					}else{
						$error = "Se ha producido un error en el sql "; 
					}
				} // Fin del for
				
			break;
			
			case "incrementoadd": 
				// Damos de alta un incremento
				if(isset($_POST['id_artista']) && $_POST['id_artista'] != '' && isset($_POST['provincia']) && $_POST['provincia'] != ''  && $_POST['descripcion'] != '' ){					
					// Comprobamos la provincia
					$id_provincia = validar_provincia($_POST['provincia']); 			
					list($tipo,$incremento) = explode("*",$_POST['descripcion']);
					
					switch($tipo){
						case "a": $tipo_incremento = "absoluto"; break; 
						case "p": $tipo_incremento = "porcentaje"; break; 
					}
					$query = "SELECT id_incremento FROM ".$pref."_netmanager_incrementostarifas WHERE id_artista = '".$_POST['id_artista']."' AND id_provincia = '".$id_provincia."'"; 
					$resultado = $db->sql_query($query); 
					if($db->sql_numrows($resultado) > 0 ) {
						$error = "El artista ya tiene un incremento para esta provincia";
					}else{ 
						$query = "INSERT INTO ".$pref."_netmanager_incrementostarifas (id_artista, id_provincia, tipo, incremento ) VALUES ('".$_POST['id_artista']."', '".$id_provincia."', '".$tipo_incremento."', '".$incremento."')"; 	
						$db->sql_query($query); 
						$correcto = "El incremento ha sido creado correctamente"; 					
					}
				}else{
					$error = "No tenemos datos para realizar la acción"; 
				}
			break;  
			
			case "incrementodel": 
				// Eliminamos un incremento
				if(isset($_POST['id_artista']) && $_POST['id_artista'] != '' && isset($_POST['provincia']) && $_POST['provincia'] != '' ){					
					// Comprobamos la provincia
					$id_provincia = validar_provincia($_POST['provincia']); 			
									
					$query = "DELETE FROM ".$pref."_netmanager_incrementostarifas WHERE id_artista = '".$_POST['id_artista']."' AND id_provincia = '".$id_provincia."'"; 	
					$db->sql_query($query); 
					$correcto = "El incremento ha sido eliminado correctamente"; 					
				}else{
					$error = "No tenemos datos para realizar la acción"; 
				}
			break; 
			
			default: 
				$error = " No existe la acci&oacute;n"; 
			break; 	
			
			
			
			
		} // fin del swtich
	}else{
		$error = "No existe el artista"; 
	}
}else{
	$error = "Falta informacion para realizar la accion"; 
}



if($error != ''){   echo "ERROR:".$error; 
}else{	echo "CORRECTO: ".$correcto; } 