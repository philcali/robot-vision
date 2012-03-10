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

  var protocol = window.location.protocol == 'https:' ? 'wss://' : 'ws://';
  var host = protocol + window.location.host;

  var socket = new WebSocket(host);

  // http://www.html5canvastutorials.com/advanced/html5-canvas-mouse-coordinates/
  canvas.onmousemove = function(evt) {
    var obj = canvas;
    var top = 0;
    var left = 0;
    while (obj && obj.tagName != 'BODY') {
        top += obj.offsetTop;
        left += obj.offsetLeft;
        obj = obj.offsetParent;
    }
 
    // return relative mouse position
    var x = evt.clientX - left + window.pageXOffset;
    var y = evt.clientY - top + window.pageYOffset;
    
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

      // Redraw desktop only after the last draw
      setTimeout(reDesktop, 200);
    };

    image.src = "/image/desktop.jpg";
  }

  setTimeout(redraw, 1000);
  setTimeout(reDesktop, 1000);
};
