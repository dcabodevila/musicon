$(document).ready(function(){
      //Animation Slideup
      $('.dropdown').on('show.bs.dropdown', function(e){
        $(this).find('.dropdown-menu').first().stop(true, true).slideDown().delay(200);
      });
      //Animation Slidedown
      $('.dropdown').on('hide.bs.dropdown', function(e){
        $(this).find('.dropdown-menu').first().stop(true, true).slideUp().delay(400);
      });

//    $('.dropdown').hover(function(){ 
//      $('.dropdown-toggle', this).trigger('click'); 
//    });
      


});


function notificar(texto, tipo){
	
	let icono = 'glyphicon glyphicon-ok';
	if (tipo=='success'){
		icono = 'glyphicon glyphicon-ok';	
	}
	else if (tipo=='danger'){
		icono = 'glyphicon glyphicon-remove';			
	}
	
	$.notify({
		icon: 'glyphicon glyphicon-warning-sign',
		message: texto
	},{
		// settings
		element: 'body',
		position: null,
		allow_dismiss: false,
		showProgressbar: false,
		placement: {
			from: "top",
			align: "center"
		},
		icon_type: 'class',
		offset: 20,
		spacing: 10,
		z_index: 1031,
		delay: 2000,
		timer: 1000,		
		type: tipo,
		animate: {
			enter: 'animated fadeInDown',
			exit: 'animated fadeOutUp'
		},
	});
	
}