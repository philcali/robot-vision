// Light wrapper over socket api, and convenience for attaching to a Viewport
function Control(url) {
  this.socket = new WebSocket(url);
  this.mousepos = { x: 0, y: 0 };

  var self = this;

  this.attach = function(viewport) {
    // Only state change
    viewport.withContext(function(context) {
      context.strokeStyle = "black";
      context.lineWidth = 2;
    });

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
        
        self.mousepos = { x: x, y: y };
        self.socket.send("mousemove|" + x + "|" + y);
      });

      $(canvas).on('mousedown', function(evt) {
        self.socket.send("mousedown|" + evt.button);
      });

      $(canvas).on('mouseup', function(evt) {
        self.socket.send("mouseup|" + evt.button);
      });
    });

    $(document).on('keydown', function(evt) {
      if (evt.preventDefault) {
        evt.preventDefault();
      }
      self.socket.send("keydown|" + evt.keyCode);
    });

    $(document).on('keyup', function(evt) {
      if (evt.preventDefault) {
        evt.preventDefault();
      }
      self.socket.send("keyup|" + evt.keyCode);
    });

    return self;
  }

  this.drawPointer = function(viewport) {
    viewport.withCanvasAndContext(function(canvas, context) {
      context.clearRect(0, 0, canvas.width, canvas.height);

      context.beginPath();
      context.arc(self.mousepos.x, self.mousepos.y, 3, 2 * Math.PI, false);
      context.stroke();
    });
  };

  return self;
}
