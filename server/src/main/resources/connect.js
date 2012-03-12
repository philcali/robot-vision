$(function() {
  var protocol = window.location.protocol == 'https:' ? 'wss://' : 'ws://';
  var host = protocol + window.location.host;

  var viewport = new Viewport('control');
  var control = new Control(host).attach(viewport);

  $(viewport).on('reload', function() {
    control.drawPointer(viewport);
  });

  setTimeout(viewport.start, 1000);
});
