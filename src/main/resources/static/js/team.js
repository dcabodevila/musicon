$(document).ready(function(){
	
	cargarPlantilla();	
	
	$('#equipoSeleccionado').on('change', function (e) {		
		cargarPlantilla();	
		mostrarListaOcultarDetalle();
	});

});

function recargarEventosJsListaJugadores(){
	$('.selectJugador').on('click', function(e){
		  let idJugador = this.dataset.id;
		  $('#player-card-container').hide();
		  $('#contract-card-container').hide();
		  
		  cargarDatosJugador(idJugador);

		  desmarcarJugadoresList();
		  $( this ).addClass( "active" );
	});
}


function cargarDatosJugador(idJugador){
	$('#player-card-container').fadeOut("slow");
	$('#contract-card-container').fadeOut("slow");
	
	$.ajax({
	    url : '/jugador/'+idJugador,
	    type: "GET",
	    contentType: "application/json",
	    success: function(response){
	    	cargarDatosContrato(idJugador);
	        $('#player-card-container').html(response);

	    },
	    error:function(response){
	    	recargarEventosJsJugador();
	    }
	});
	

	
}

function cargarDatosContrato(idJugador){
	$.ajax({
	    url : '/jugador/contrato/'+idJugador,
	    type: "GET",
	    contentType: "application/json",
	    success: function(response){
	        $('#contract-card-container').html(response);
			mostrarPlayerCardOcultandoLista();
	        recargarEventosJsJugador();
	    },
	    error:function(response){
	    }
	});	
}

function recargarEventosJsJugador(){
	$('#back-button').on('click', function(e){
		mostrarListaOcultarDetalle();
	});
	
	$('.btn-firmar').on('click',function (e) {
		let idEquipo = $('#idEquipo').val();
		let idJugador =  this.dataset.id;
		$('.spinner').attr('hidden', false);
		$('.btn-firmar').addClass('disabled');		
		firmarJugadorDefault(idEquipo, idJugador);
	});
}

function mostrarListaOcultarDetalle(){
	$('#player-card-container').hide();
	$('#contract-card-container').hide();
	$('#plantilla-container').removeClass('d-none');
	$('#plantilla-container').removeClass('d-sm-block');
}


function mostrarPlayerCardOcultandoLista(){
	 
 	$('#plantilla-container').addClass('d-none');
    $('#plantilla-container').addClass('d-sm-block');	  		
	$('#player-card-container').fadeIn("slow");
	$('#contract-card-container').fadeIn("slow");

}

function desmarcarJugadoresList(){
	$( ".selectJugador" ).each(function() {
		$( this ).removeClass( "active" );
	});
}

function cargarPlantilla(){
	let idEquipo = $('#equipoSeleccionado').val();
	let idCompeticion = $('#idCompeticion').val();
	$.ajax({
	    url : '/equipo/getplantilla/',
	    type: "GET",
	    contentType: "application/json",
	    data : {
	    	idEquipo : idEquipo,
	    	idCompeticion : idCompeticion
	    },
	    success: function(response){
	        $('#plantilla-container').html(response);
	        recargarEventosJsListaJugadores();
	    },
	    error:function(response){
	    	recargarEventosJsListaJugadores();
	    }
	});	

}

