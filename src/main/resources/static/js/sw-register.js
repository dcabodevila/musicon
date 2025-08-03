$(document).ready(function() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/js/service-worker.js')
      .then(registration => {
        console.log('Service Worker registrado correctamente');
      })
      .catch(error => {
        console.error('Error al registrar el Service Worker:', error);
      });
  }
});