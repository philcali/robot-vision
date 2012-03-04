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

  var mousepos = {};

  var host = "ws://" + window.location.host;

  var socket = new WebSocket(host);

  canvas.onmousemove = function(evt) {
    var x = evt.clientX - 9;
    var y = evt.clientY - 8;
    
    mousepos = { x: x, y: y };
    socket.send("mousemove|" + x + "|" + y);
  };
 
  canvas.onmousedown = function(evt) {
    socket.send("mousedown|" + evt.button);
  };

  canvas.onmouseup = function(evt) {
    socket.send("mouseup|" + evt.button);
  };

  document.onkeydown = function(evt) {
    if (evt.preventDefault) {
      evt.preventDefault();
    }
    socket.send("keydown|" + evt.keyCode);
  };

  document.onkeyup = function(evt) {
    if (evt.preventDefault) {
      evt.preventDefault();
    }
    socket.send("keyup|" + evt.keyCode);
  };
 
  var drawPointer = function(pos) {
    context.beginPath();
    context.arc(pos.x, pos.y, 3, 2 * Math.PI, false);
    context.lineWidth = 2;
    context.strokeStyle = "black";
    context.stroke();
  };

  socket.onmessage = function(e) {
    var image = new Image();
    image.onload = function() {
      context.clearRect(0, 0, canvas.width, canvas.height);
      context.drawImage(image, 0, 0);
      drawPointer(mousepos);
    };
    image.src = "/image/desktop.jpg"
  };

  function fireNotification() {
    socket.send("render");
    requestAnimFrame(fireNotification);
  }

  setTimeout(fireNotification, 1000);
};
