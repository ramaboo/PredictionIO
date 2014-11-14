//= require 'jquery'
//= require 'slidebars'

$(document).ready(function() {

  // Main Navigation
  $('.nav-main > ul > li > a').on('click', function(event) {
    event.preventDefault();
    $(this).next().toggle();
  });

  $('.nav-main .active').parent().parent().show();

  // Page Navigation
//  $('#nav-page-toc').on('click', function(event) {
  //  event.preventDefault();
    //$('#toc').toggle();
  //});

  // Slidebars
  $.slidebars();
});