<?php

	header('Content-Type: application/json');

	//TURNS OFF ERROR REPORTING!!
	error_reporting(0);

	require_once("WeatherAPI.php");
	require_once("WeatherSources.php");
	include_once("GoogleGeoLookupAPI.php");

	//sources can be edited here
	$sources = array(new DarkSkySource(),new OpenWeatherSource());

	$result;

	if(isset($_GET["lat"]) and isset($_GET["lon"])){

		$use_details = false;

		//details value does not need to be set
		if(isset($_GET["details"])){
			$use_details = ($_GET["details"] == "true");
		}

		$lat = $_GET["lat"];
		$lon = $_GET["lon"];


		$result = getWeatherData($sources,$lat,$lon,$use_details);


	} else {
		echo "Invalid Syntax";
	}

	echo json_encode($result);
