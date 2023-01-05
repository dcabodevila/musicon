$(document).ready(function(){
		
	$('#equipoSeleccionado').on('change', function (e) {		
		cargarContratos();
		cargarDatosSalariales();
	});
	
	$('#temporadaSeleccionada').on('change', function (e) {		
		cargarDatosSalariales();	
	});

});

function cargarContratos(){
	let idEquipo = $('#equipoSeleccionado').val();
	
	$.ajax({
	    url : '/contrato/lista/'+idEquipo,
	    type: "GET",
	    contentType: "application/json",
	    success: function(response){
	        $('#contratos-container').html(response);
	    },
	    error:function(response){
	    }
	});	

}

function cargarDatosSalariales(){
	let idEquipo = $('#equipoSeleccionado').val();
	let idTemporada = $('#temporadaSeleccionada').val();
	
	$.ajax({
	    url : '/contrato/datos-salariales/'+idEquipo+'/'+idTemporada,
	    type: "GET",
	    contentType: "application/json",
	    success: function(response){
	        $('#datos-salariales-container').html(response);
	    },
	    error:function(response){
	    }
	});	

}

