$(function() {
  var desktop = Desktop.get('desktop', { pointer: true, interval: 200 });

  $('#controls-send').on('click', function() {
    desktop.scaleY = $('.size').val();
    desktop.scaleX = desktop.scaleY;
    desktop.pointer = $('.pointer').is(':checked');
    desktop.quality = $('.quality').val();

    $('#controls-modal').modal('hide');
    return false;
  });

  // Desktop reload separate from animation cycle
  setTimeout(desktop.start, 1000);
});
