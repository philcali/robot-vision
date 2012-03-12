$(function() {
  var protocol = window.location.protocol == 'https:' ? 'wss://' : 'ws://';
  var host = protocol + window.location.host;

  // A viewport has been created
  $(document).on('viewport', function(evt, viewport) {
    var control = new Control(host).attach(viewport); 

    viewport.getDesktop().stop();

    // Attach control pointer to animation cycle
    $(viewport).on('reload', function() {
      var desktop = viewport.getDesktop();

      viewport.withCanvasAndContext(function(canvas, context) {
        console.log("control loop");
        context.clearRect(0, 0, canvas.width, canvas.height);
        context.drawImage(desktop.currentImage(), 0, 0);
        control.drawPointer();
      });
    });
  });
});
