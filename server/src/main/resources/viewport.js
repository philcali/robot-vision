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

function Viewport(canvasId, desktop) {
  var canvas = document.getElementById(canvasId);
  var context = canvas.getContext('2d');

  var running = false;

  return {
    getCanvas: function() { return canvas; },
    getContext: function() { return context; },
    getDesktop: function() { return desktop; },

    withCanvas: function(callback) { callback(canvas); },
    withContext: function(callback) { callback(context); },
    withDesktop: function(callback) { callback(desktop); },

    withCanvasAndContext: function(callback) {
      callback(canvas, context);
    },

    isRunning: function() { return running; },

    start: function() {
      running = true;
      $(this).trigger('start');
    },

    stop: function() {
      running = false;
      $(this).trigger('stop');
    },

    animate: function() {
      if (running) {
        $(this).trigger('animate');
        window.requestAnimFrame(this.animate);
      }
    }
  };
};
