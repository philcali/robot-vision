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

window.onload = function() {
  var canvas = document.getElementById("desktop");
  var context = canvas.getContext("2d");

  var host = "ws://" + window.location.host;

  var socket = new WebSocket(host);

  socket.onmessage = function(e) {
    var image = new Image();
    image.onload = function() {
      context.drawImage(image, 0, 0);
    };
    image.src = "/image/desktop.jpg";
  };

  function fireNotification() {
    socket.send("render");
    requestAnimFrame(fireNotification);
  }

  setTimeout(fireNotification, 1000);
};
