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

  // Buffer to be draw on animation
  var buffer = new Image();

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

  // Redrawing the canvas should be fluid
  function redraw() {
    context.clearRect(0, 0, canvas.width, canvas.height);
    context.drawImage(buffer, 0, 0);
    drawPointer(mousepos);

    requestAnimFrame(redraw);
  }

  // Redrawing the desktop image can be much slower
  function reDesktop() {
    var image = new Image();

    image.onload = function() {
      buffer = image;
    };

    image.src = "/image/desktop.jpg";

    setTimeout(reDesktop, 150);
  }

  setTimeout(redraw, 1000);
  setTimeout(reDesktop, 1000);
};
