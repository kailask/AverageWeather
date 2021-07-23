<?php

  if(isset($_GET["lat"]) && isset($_GET["lon"])){

    //Unused: $streetViewBase = 'https://maps.googleapis.com/maps/api/streetview?key=[key]&size=640x640&fov=120&location=';
    $mapsBase = 'https://maps.googleapis.com/maps/api/staticmap?key=[key]&size=640x640&maptype=satellite&zoom=14&scale=2&center=';
    
    $placesPixBase = 'https://maps.googleapis.com/maps/api/place/photo?key=[key]';
    $placesSearceBase='https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=[key]';

    $width = 1080;
    $height = 720;

    $lat= $_GET['lat'];
    $lon=$_GET['lon'];

    //Set max width and height if available
    if(isset($_GET["h"])){
      $height = $_GET["h"];
    }

    if(isset($_GET["w"])){
      $width = $_GET["w"];
    }

    echo htmlentities($mapsBase.$lat.','.$lon);
  }
