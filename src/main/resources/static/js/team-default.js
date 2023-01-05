$(document).ready(function(){

	$('#busquedaJugador').on('change', function (e) {
		cargarListaJugadores();	
	});

	$(document).on('keypress',function(e) {
	    if(e.which == 13) {
	    	$('#busquedaJugador').focusout();
	    }
	});
	cargarListaJugadores();
	
});


function recargarEventosJsListaJugadores(){
	
	$('.selectJugadorDefault').on('click', function(e){
		  let idJugador = this.dataset.id;
		  
		  cargarDatosJugador(idJugador);

		  desmarcarJugadoresList();
		  $( this ).addClass( "active" );
		    
	});

}

function cargarDatosJugador(idJugador){
	$('#player-card-container').fadeOut("slow");
	
	$.ajax({
	    url : '/jugadordefault/'+idJugador,
	    type: "GET",
	    contentType: "application/json",
	    success: function(response){
	        $('#player-card-container').html(response);
			mostrarPlayerCardOcultandoLista();
	        recargarEventosJsJugador();
	    },
	    error:function(response){
	    	recargarEventosJsJugador();
	    }
	});
}

function recargarEventosJsJugador(){
	$('#back-button').on('click', function(e){
		$('#player-card-container').fadeOut("slow");
		$('#list-container').removeClass('d-none');
		$('#list-container').removeClass('d-sm-block');
	});
	
	$('.btn-firmar').on('click',function (e) {
		let idEquipo = $('#idEquipo').val();
		let idJugador =  this.dataset.id;
		$('.spinner').attr('hidden', false);
		$('.btn-firmar').addClass('disabled');		
		firmarJugadorDefault(idEquipo, idJugador);
	});
}

function mostrarPlayerCardOcultandoLista(){
	 
 	$('#list-container').addClass('d-none');
    $('#list-container').addClass('d-sm-block');	  		
	$('#player-card-container').fadeIn("slow"); 

}

function desmarcarJugadoresList(){
	$( ".selectJugadorDefault" ).each(function() {
		$( this ).removeClass( "active" );
	});
}

function cargarListaJugadores(){
	let idCompeticion = $('#idCompeticion').val();
	let nombreJugador = $('#busquedaJugador').val();
	let urlLista = '/equipo/'+idCompeticion+'/buscarJugadores';
	if (nombreJugador!=""){
		urlLista = '/equipo/'+idCompeticion+'/buscarJugadores/'+nombreJugador;
	}
	
	$.ajax({
	    url : urlLista,
	    type: "GET",
	    contentType: "application/json",
	    success: function(response){
	        $('#lista-jugadores-container').html(response);
	        recargarEventosJsListaJugadores();
	    },
	    error:function(response){
	    	recargarEventosJsListaJugadores();
	    }
	});
	
	
}

function firmarJugadorDefault(idEquipo, idJugador){
	$.ajax({
	    url :"/jugadordefault/firmar",
	    type: "GET",
	    contentType: "json",
	    data: {
	    	"idEquipo" : idEquipo,
	        "idJugador":idJugador
	    },
	    success: function(response){
	        notificar('Jugador firmado', 'success');
	        $('#player-card-container').fadeOut("slow");
			$('#list-container').removeClass('d-none');
			$('#list-container').removeClass('d-sm-block');
			$('.btn-firmar').removeClass('disabled');
			$('.spinner').attr('hidden', true);			
	        cargarListaJugadores();
	    },
	    error:function(response){
	    	notificar('Error al firmar el jugador', 'danger');
	    	$('.btn-firmar').removeClass('disabled');
	    	$('.spinner').attr('hidden', true);
	    }
	});
	


}