$(function() {
  var desktop = new Desktop('desktop', { pointer: true, interval: 200 });

  // Desktop reload separate from animation cycle
  setTimeout(desktop.start, 1000);
});
