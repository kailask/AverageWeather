<?php

function makeGeoLookupRequest($lat,$lon){
	
	$url_start = 'https://maps.googleapis.com/maps/api/geocode/json?latlng=';
	$url_end = '&key=[key]&result_type=colloquial_area|locality|neighborhood';
		
	
	$http_result = file_get_contents($url_start.$lat.','.$lon.$url_end);
		
	//confirm not null and status if OK
	if(isset($http_result) and strcmp(json_decode($http_result)->status,"OK")){return $http_result;}
	
	return null;
	
}