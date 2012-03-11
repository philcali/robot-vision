function Viewport(canvasId, desktop) {
  var canvas = document.getElementById(canvasId);
  var context = canvas.getContext('2d');

  var viewport = this;

  this.getCanvas = function() { return canvas; }
  this.getContext = function() { return context; }
  this.getDesktop = function() { return desktop; }

  this.withCanvas = function(callback) { callback(canvas); }
  this.withContext = function(callback) { callback(context); }
  this.withDesktop = function(callback) { callback(desktop); }

  this.withCanvasAndContext = function(callback) {
    callback(canvas, context);
  }

  // Globally declare the viewport initiation.
  $(document).trigger('viewport', [viewport]);

  // Begin animation cycle... Clients should hook into the reload event
  function animate() {
    $(viewport).trigger('reload');

    requestAnimationFrame(animate);
  }

  setTimeout(animate, 1000);
}
