function Desktop(elem, obj) {
  if (typeof obj === 'undefined') {
    obj = {};
  }
 
  // Public members 
  this.interval = typeof obj.interval === 'undefined' ? 200 : obj.interval;
  this.scaleX = typeof obj.scaleX === 'undefined' ? "1.0" : obj.scaleX + '';
  this.scaleY = typeof obj.scaleY === 'undefined' ? "1.0" : obj.scaleY + '';
  this.quality = typeof obj.quality === 'undefined' ? "0.2" : obj.quality + '';
  this.pointer = typeof obj.pointer === 'undefined' ? false : obj.pointer;

  var self = this;

  // private members
  var image = document.getElementById(elem);
  image.onload = function() {
    $(self).trigger('reload', [image]);

    // Redraw desktop only after the last draw
    if (self.isRunning()) {
      setTimeout(self.execute, self.interval);
    }
  };

  var running = false;

  this.buildUrl = function() {
    var p = self.pointer ? 'p' : 'n';
    return "/image/desktop_" +
      self.scaleX + "x" +
      self.scaleY + "_" +
      self.quality + "_" + p + ".jpg";
  };

  this.execute = function() {
    image.src = self.buildUrl();
  }

  this.isRunning = function() { return running; }
  this.start = function() {
    running = true;
    self.execute();
  }

  this.stop = function() {
    running = false;
  }

  this.currentImage = function() { return image; }
}
