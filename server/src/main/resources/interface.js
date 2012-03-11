$(function() {
  var desktop = new Desktop('buffer', { pointer: true });
  var viewport = new Viewport('desktop', desktop);

  $(viewport).on('reload', function() {
    $(this).withCanvasAndContext(function(canvas, context) {
      context.clearRect(0, 0, canvas.width, canvas.height);
      context.drawImage(desktop.currentImage(), 0, 0);
    });
  });

  // Desktop reload separate from animation cycle
  setTimeout(desktop.start, 1000);
});
