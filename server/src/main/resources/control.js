// Light wrapper over socket api, and convenience for attaching to a Viewport
function Control(url) {
  this.socket = new WebSocket(url);
  this.mousepos = { x: 0, y: 0 };

  return this;
}

Control.prototype.attach = function(viewport) {
  var control = this;

  viewport.withCanvas(function(canvas) {
    // http://www.html5canvastutorials.com/advanced/html5-canvas-mouse-coordinates/
    $(canvas).on('mousemove', function(evt) {
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
      
      control.mousepos = { x: x, y: y };
      control.socket.send("mousemove|" + x + "|" + y);
    });

    $(canvas).on('mousedown', function(evt) {
      control.socket.send("mousedown|" + evt.button);
    });

    $(canvas).on('mouseup', function(evt) {
      control.socket.send("mouseup|" + evt.button);
    });

    $(document).on('keydown', function(evt) {
      if (evt.preventDefault) {
        evt.preventDefault();
      }
      control.socket.send("keydown|" + evt.keyCode);
    });

    $(document).on('keyup', function(evt) {
      if (evt.preventDefault) {
        evt.preventDefault();
      }
      control.socket.send("keyup|" + evt.keyCode);
    });
  });

  control.drawPointer = function() {
    viewport.withContext(function(context) {
      context.beginPath();
      context.arc(control.mousepos..x, control.mousepos.y, 3, 2 * Math.PI, false);
      context.lineWidth = 2;
      context.strokeStyle = "black";
      context.stroke();
    });
  };

  $(control).trigger('attach', [viewport]);

  $(document).on('animate', function(e, v) {
    if (v != viewport) return;

    var desktop = viewport.getDesktop();

    viewport.withCanvasAndContext(function(canvas, context) {
      context.clearRect(0, 0, canvas.width, canvas.height);
      context.drawImage(desktop.currentImage(), 0, 0);
      control.drawPointer();
    });
  });

  return control;
}
