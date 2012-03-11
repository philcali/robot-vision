function Desktop(elem, obj) {
  if (typeof obj === 'undefined') {
    obj = {};
  }
  
  this.interval = typeof obj.interval === 'undefined' ? 200 : obj.interval;
  this.scaleX = typeof obj.scaleX === 'undefined' ? 1.0 : obj.scaleX;
  this.scaleY = typeof obj.scaleY === 'undefined' ? 1.0 : obj.scaleY;
  this.quality = typeof obj.quality === 'undefined' ? 0.2 : obj.quality;
  this.pointer = typeof obj.pointer === 'undefined' ? false : obj.pointer;

  var desktop = this;

  var running = false;

  var image = document.getElementById(elem);

  this.execute = function() {
    var newImage = new Image();

    newImage.onload = function() {
      image = newImage;

      // Redraw desktop only after the last draw
      if (running)
        setTimeout(desktop.execute, desktop.interval);
    };

    newImage.src = desktop.buildUrl();
  }

  this.isRunning = function() { return running; }

  this.stop = function() { 
    running = false;
  }

  this.start = function() {
    running = true;
    desktop.execute();
  }

  this.currentImage = function() { return image; };
}

Desktop.prototype.buildUrl = function() {
  var p = this.pointer ? 'p' : 'n';
  return "/image/desktop_" +
    this.scaleX + "x" +
    this.scaleY + "_" +
    this.quality + "_" + p + ".jpg";
}
