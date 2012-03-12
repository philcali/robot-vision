$(function() {
  var viewport = new Viewport('desktop');
  var desktop = new Desktop('buffer', { pointer: true });

  // Specifically use the image buffer, to offload the desktop render
  $(desktop).on('reload', function(e, image) {
    viewport.withContext(function(context) {
      context.drawImage(image, 0, 0);
    });
  });

  // Desktop reload separate from animation cycle
  setTimeout(desktop.start, 1000);
});
