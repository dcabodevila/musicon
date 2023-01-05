$(document).ready(function(){
		
	$('#equipoSeleccionado').on('change', function (e) {		
		cargarContratos();
	});
	
	
	$('#ofrecer-traspaso').on('click', function (e) {
		let idsJugadoresOrigen = [];
		let idsJugadoresDestino = [];
		let idRondaOrigen = $('.ronda-origen').val();
		let idRondaDestino = $('.ronda-destino').val();
		$( ".jugador-origen:checked" ).each(function( index, value ) {
			idsJugadoresOrigen.push(value.id);
		});
		
		$( ".jugador-destino:checked" ).each(function( index, value ) {
			idsJugadoresDestino.push(value.id);
		});
		
		ofrecerTraspaso(idsJugadoresOrigen, idsJugadoresDestino, idRondaOrigen, idRondaDestino);
		
	});

	$('#validar-traspaso').on('click', function (e) {
		let idsJugadoresOrigen = [];
		let idsJugadoresDestino = [];
		let idRondaOrigen = $('.ronda-origen').val();
		let idRondaDestino = $('.ronda-destino').val();
		$( ".jugador-origen:checked" ).each(function( index, value ) {
			idsJugadoresOrigen.push(value.id);
		});
		
		$( ".jugador-destino:checked" ).each(function( index, value ) {
			idsJugadoresDestino.push(value.id);
		});
		
		validarTraspaso(idsJugadoresOrigen, idsJugadoresDestino, idRondaOrigen, idRondaDestino);
		
	});

});

function cargarContratos(){
	let idEquipo = $('#equipoSeleccionado').val();
	
	$.ajax({
	    url : '/traspaso/lista/'+idEquipo,
	    type: "GET",
	    contentType: "application/json",
	    success: function(response){
	        $('#equipo-destino-container').html(response);
	    },
	    error:function(response){
	    }
	});	

}


function ofrecerTraspaso(idsJugadoresOrigen, idsJugadoresDestino, idRondaOrigen, idRondaDestino){
	let idEquipoOrigen = $('#equipoUsuario').val();
	let idEquipoDestino = $('#equipoSeleccionado').val();
	$('.spinner-ofrecer').attr('hidden', false);
	$('#ofrecer-traspaso').addClass('disabled');
	
	
	$.ajax({
	    url : '/traspaso/ofrecer-traspaso/',
	    type: "GET",
	    dataType: "json",
	    data: {
	    	"idEquipoOrigen": idEquipoOrigen,
	    	"idEquipoDestino": idEquipoDestino,
	    	"idsJugadoresOrigen" : JSON.stringify(idsJugadoresOrigen),
	    	"idsJugadoresDestino" : JSON.stringify(idsJugadoresDestino),
	    	"idRondaOrigen": idRondaOrigen,
	    	"idRondaDestino": idRondaDestino
    	},
	    contentType: "application/json",
	    success: function(response){
	    	$('.spinner-ofrecer').attr('hidden', true);
	    	$('#ofrecer-traspaso').removeClass('disabled');
	    	let tipoMensaje = 'success';
	    	
	    	if (response.valido){
	    		tipoMensaje = 'success';	    	
	    	}
	    	else {
	    		tipoMensaje = 'danger';
	    	}

	    		
    		for (var i=0; i<response.listaMensajes.length; i++) {	    			 
    			 notificar(response.listaMensajes[i], tipoMensaje);
			}
    		
    		window.location.href = '/traspaso/ofrecidos/'+ idEquipoOrigen;
	    	
	    },
	    error:function(response){
	    	$('.spinner-ofrecer').attr('hidden', true);
	    	$('#ofrecer-traspaso').removeClass('disabled');
	    	notificar('Error al enviar el traspaso', 'danger');   	
	    }
	});	

}


function validarTraspaso(idsJugadoresOrigen, idsJugadoresDestino, idRondaOrigen, idRondaDestino){
	let idEquipoOrigen = $('#equipoUsuario').val();
	let idEquipoDestino = $('#equipoSeleccionado').val();
	$('.spinner-validar').attr('hidden', false);
	$('#validar-traspaso').addClass('disabled');
	
	$.ajax({
	    url : '/traspaso/validar-traspaso/',
	    type: "GET",
	    dataType: "json",
	    data: {
	    	"idEquipoOrigen": idEquipoOrigen,
	    	"idEquipoDestino": idEquipoDestino,
	    	"idsJugadoresOrigen" : JSON.stringify(idsJugadoresOrigen),
	    	"idsJugadoresDestino" : JSON.stringify(idsJugadoresDestino),
	    	"idRondaOrigen": idRondaOrigen,
	    	"idRondaDestino": idRondaDestino
    	},
	    contentType: "application/json",
	    success: function(response){
	    	$('.spinner-validar').attr('hidden', true);
	    	$('#validar-traspaso').removeClass('disabled');
	    	let tipoMensaje = 'success';
	    	
	    	if (response.valido){
	    		tipoMensaje = 'success';	    	
	    	}
	    	else {
	    		tipoMensaje = 'danger';
	    	}

	    		
    		for (var i=0; i<response.listaMensajes.length; i++) {	    			 
    			 notificar(response.listaMensajes[i], tipoMensaje);
			}	    		
	    	
	    	
	    },
	    error:function(response){
	    	$('.spinner-validar').attr('hidden', true);
	    	$('#validar-traspaso').removeClass('disabled');
	    	notificar('Error al validar el traspaso', 'danger');
	    }
	});	

}


