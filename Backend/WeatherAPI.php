<?php
	require_once("WeatherSources.php");

	function getWeatherData($sources,$lat,$lon,$get_details){

		$data_array = array();

		//have each source retrieve its data from url and put data in array
		foreach($sources as $current_source){
			$current_source->getData($lat,$lon,$get_details);
			if(isset($current_source->weather_data)){array_push($data_array,$current_source->weather_data);}
		}

		//get new data block from average
		$averaged_result = CurrentWeatherData::calculateAverages($data_array,$get_details);

		return $averaged_result;


	}
