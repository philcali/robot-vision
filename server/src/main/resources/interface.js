$(function() {
  var desktop = new Desktop('buffer', { pointer: true });
  var viewport = new Viewport('desktop', desktop);

  // Specifically use the image buffer, to offload the desktop render
  $(desktop).on('reload', function(e, image) {
    viewport.withContext(function(context) {
      console.log("interface loop");
      context.drawImage(image, 0, 0);
    });
  });

  // Desktop reload separate from animation cycle
  setTimeout(desktop.start, 1000);
});
