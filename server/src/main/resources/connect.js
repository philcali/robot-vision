// shim layer with setTimeout fallback
// TODO: remove this, replace as chrome extension
window.requestAnimFrame = (function(){
  return  window.requestAnimationFrame       || 
          window.webkitRequestAnimationFrame || 
          window.mozRequestAnimationFrame    || 
          window.oRequestAnimationFrame      || 
          window.msRequestAnimationFrame     || 
          function( callback ){
            window.setTimeout(callback, 1000 / 60);
          };
})();

$(function() {
  var protocol = window.location.protocol == 'https:' ? 'wss://' : 'ws://';
  var host = protocol + window.location.host;

  // A viewport has been created
  $(document).on('viewport', function(viewport) {
    var control = new Control(host).attach(viewport); 

    // Attach control pointer to animation cycle
    $(viewport).on('reload', function() {
    });
  });
});
