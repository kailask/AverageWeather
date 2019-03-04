<?php

	include_once("GoogleGeoLookupAPI.php");
	//default template for weather sources
	abstract class WeatherSource{

		private $url_start;
		private $url_end;
		public $weather_data;


		//called by all classes
		public function makeHTTPRequest($url){
			$http_result = file_get_contents($url);

			if(isset($http_result)){return json_decode($http_result);}

			return null;
		}

		public abstract function getData($lat,$lon,$get_details);

		//all classes also have an extract data function but all will have diff #s of params

	}

	class OpenWeatherSource extends WeatherSource{

		private $url_forecast_start ='http://api.openweathermap.org/data/2.5/forecast?lat=';
		private $url_current_start = 'http://api.openweathermap.org/data/2.5/weather?lat=';
		private $url_end = '&units=imperial&APPID=[ID]';

		public function getData($lat,$lon,$get_details){

			$raw_current_data= parent::makeHTTPRequest($this->url_current_start.$lat.'&lon='.$lon.$this->url_end);

			if($get_details){

				//details only retrieved if necessary
				$raw_detail_data = parent::makeHTTPRequest($this->url_forecast_start.$lat.'&lon='.$lon.$this->url_end);

				if(isset($raw_detail_data) && isset($raw_current_data)){$this->weather_data = $this->extractDetailsData($raw_current_data,$raw_detail_data);}

			} else {
				if(isset($raw_current_data)){
				$this->weather_data = $this->extractCurrentData($raw_current_data);
				}
			}

		}

		//always called
		public function extractCurrentData($data_obj){
			$main_data = $data_obj->main;
			$wind = $data_obj->wind;
			$main_weather = $data_obj->weather[0];
			$clouds_data = $data_obj->clouds;

			//calculate precip
			$precip = $this->calculateprecip($data_obj->rain,$data_obj->snow);

			//create meta obj
			$meta = new MetaData(null,$data_obj->name,null);

			//normalize icon value
			$icon = $this->convertIcon($main_weather->icon);

			//Convert to percentage
			$humidity = ($main_data->humidity)/100;

			//some values are always null
			return new CurrentWeatherData($main_weather->description,$icon,$precip,null,$main_data->temp,$clouds_data->all/100,
			$wind->speed,$wind->deg,null,null,null,$meta,$humidity,$main_data->sea_level,null);

		}

		//calls adds extracted details data to call result from extractCurrentData
		public function extractDetailsData($raw_current_data,$raw_detail_data){

			$main_data = $this->extractCurrentData($raw_current_data);

			$raw_data_array = $raw_detail_data->list;

			$hourly_data_array = array();

			//daily forcasts not provided by this source so it is aproximated from hourly data
			$daily_data_array = $this->createDailyDataArray($raw_data_array);


			//fill hourly array with data
			foreach($raw_data_array as $current_data_block){
				$current_main = $current_data_block->main;
				$current_weather = $current_data_block->weather[0];
				$current_wind = $current_data_block->wind;

				$precip = $this->calculateprecip($current_data_block->rain,$current_data_block->snow);

				//normalize icon value
				$icon = $this->convertIcon($current_weather->icon);

				//three of each data block is added because source frequency is every 3 hours and needs to be normalized
				for($num_to_add = 2; $num_to_add >= 0; $num_to_add--){
					array_push($hourly_data_array,new HourlyDataBlock($current_data_block->dt - (3600*$num_to_add),$icon,
					$current_weather->description,$precip,null,$current_main->temp,$current_wind->speed));
				}
			}

			//no descriptions for this source
			$hourly_data = new DataArray($hourly_data_array,null);
			$daily_data = new DataArray($daily_data_array,null);

			//substitute null values with data arrays
			$main_data->hourly = $hourly_data;
			$main_data->daily = $daily_data;

			return $main_data;
		}

		//constructs daily data from hourly data
		private function createDailyDataArray($hourly_array){

			return null;
		}

		//function called to calculate precip based on snow,rain
		private function calculateprecip($rain_obj,$snow_obj){

			if(isset($rain_obj)){
				$precip = ($rain_obj->{'3h'})/3.0;
			} elseif(isset($snow_obj)){
				$precip = ($snow_obj->{'3h'})/3.0;
			} else {
				$precip = 0;
			}

			//conver mm to in
			$precip *= 0.0393701;

			return $precip;
		}

		//converts icon name to unified format
		//no differentiation is made between day/night
		private function convertIcon($icon){

			$icon = substr($icon,0,2);

			switch($icon){
				case '01':
					$icon = '01';
					break;
				case '02':
					$icon = '06';
					break;
				case '03':
				case '04':
					$icon = '05';
					break;
				case '09':
					$icon = '12';
					break;
				case '10':
					$icon = '02';
					break;
				case '11':
					$icon = '13';
					break;
				case '13':
					$icon = '03';
					break;
				case '50':
					$icon = '04';
					break;
				default:
					$icon = '00';
					break;
			}

			return $icon;
		}

	}

	class DarkSkySource extends WeatherSource{

		private $url_start = 'https://api.forecast.io/forecast/[key]/';


		public function getData($lat,$lon,$details){
			//details boolean is unused by this source

			$raw_data = parent::makeHTTPRequest($this->url_start.$lat.','.$lon);

			if(isset($raw_data)){$this->weather_data = $this->extractData($raw_data);}

		}

		public function extractData($data_obj){

			//divide raw data
			$current_data_raw = $data_obj->currently;
			$hourly_data_raw = $data_obj->hourly;
			$daily_data_raw = $data_obj->daily;

			//fill new arrays with extracted data
			$hourly_array = array();
			foreach($hourly_data_raw->data as $current_hour){

				//normalize icon
				$icon = $this->convertIcon($current_hour->icon);

				array_push($hourly_array,new HourlyDataBlock($current_hour->time,$icon,$current_hour->summary,
				$current_hour->precipIntensity,$current_hour->precipProbability,$current_hour->temperature,$current_hour->windSpeed));

			}
			$hourly_data = new DataArray($hourly_array,$hourly_data_raw->summary);

			$daily_array = array();
			foreach($daily_data_raw->data as $current_day){

				//normalize icon
				$icon = $this->convertIcon($current_day->icon);

				array_push($daily_array,new DailyDataBlock($current_day->time,$icon,$current_day->summary,
				$current_day->precipIntensity,$current_day->precipProbability,$current_day->temperatureMin,$current_day->temperatureMax,$current_day->windSpeed,$current_day->cloudCover,$current_day->humidity,
				$current_day->sunriseTime,$current_day->sunsetTime,$current_day->temperatureMinTime,$current_day->temperatureMaxTime,$current_day->windBearing,$current_day->visibility,$current_day->pressure));

			}
			$daily_data = new DataArray($daily_array,$daily_data_raw->summary);

			//create and populate metadata obj
			$meta_data = new MetaData(null,null,$data_obj->timezone);

			//normalize icon
			$icon = $this->convertIcon($current_data_raw->icon);

			//create main weather data obj
			return new CurrentWeatherData($current_data_raw->summary,$icon,$current_data_raw->precipIntensity,$current_data_raw->precipProbability,
			$current_data_raw->temperature,$current_data_raw->cloudCover,$current_data_raw->windSpeed,$current_data_raw->windBearing,$current_data_raw->nearestStormDistance,$hourly_data,
			$daily_data,$meta_data,$current_data_raw->humidity,$current_data_raw->pressure,$current_data_raw->visibility);
		}

		//converts icon name to unified format
		//no differentiation is made between day/night
		private function convertIcon($icon){

			switch($icon){
				case 'clear-night':
				case 'clear-day':
					$icon = '01';
					break;
				case 'rain':
					$icon = '02';
					break;
				case 'snow':
					$icon = '03';
					break;
				case 'sleet':
					$icon = '10';
					break;
				case 'wind':
					$icon = '11';
					break;
				case 'fog':
					$icon = '04';
					break;
				case 'cloudy':
					$icon = '05';
					break;
				case 'partly-cloudy-day':
				case 'partly-cloudy-night':
					$icon = '06';
					break;
				default:
					$icon = '00';
					break;
			}

			return $icon;
		}

	}

	//weather data class for all sources
	class CurrentWeatherData{

		/*POSSIBLE ICON VALUES (.png is not included in value)

		No differentiation is made between day and night at this stage

		Numbers greater than 10 may not be available
		from all sources and are more specific

			Icon	Description
			00		 sun+clouds (default)
			01		 clear sky
			02		 rain
			03		 snow
			04		 fog/mist
			05		 cloudy
			06		 partly cloudy

		(Only available from some sources)
			10		 sleet
			11		 wind
			12		 shower rain
			13		 storm

		*/

		//meta data opj
		public $metaData;
		//top-level data

		//Units for all values:       in/hr     0-1       deg F   mph     0-1         0-360         miles           mbar    0-1        miles
		public $description, $icon, $precip, $precipProb, $temp, $wind, $cloudCover, $windBaring, $nearestStorm,$pressure,$humidity,$visibility;
		//hourly array
		public $hourly;
		//daily array
		public $daily;


		public function __construct($description, $icon, $precip, $precipProb, $temp, $cloudCover, $wind, $windBaring, $nearestStorm,$hourly,$daily,$meta,$humidity,$pressure,$visibility){

			$this->description = $description;$this->icon = $icon;$this->precip = $precip;$this->precipProb = $precipProb;
			$this->temp = $temp;$this->wind = $wind;$this->cloudCover = $cloudCover;$this->windBaring = $windBaring;$this->nearestStorm = $nearestStorm;$this->pressure=$pressure;$this->humidity=$humidity;
			$this->visibility = $visibility;

			$this->hourly = $hourly;
			$this->daily = $daily;
			$this->metaData = $meta;

		}

		//function returns a new currentWeatherData block created from averaging an array of other data blocks
		public static function calculateAverages($data_array,$use_details){

			//array of average values mapped [varName[count,default value,num of decimals to round], . . .]
			//any values may be null so all are checked and have independent source counts
			$average_values = array(
			"temp"=> array('count' => 0, 'value' => null, 'decimals' => 0),
		 	"precip"=> array('count' => 0, 'value' => 0, 'decimals' => 4),
			"precipProb"=> array('count' => 0, 'value' => 0, 'decimals' => 2),
			"cloudCover"=> array('count' => 0, 'value' => 0, 'decimals' => 2),
			"wind"=> array('count' => 0, 'value' => 0, 'decimals' => 0),
			"nearestStorm"=> array('count' => 0, 'value' => null, 'decimals' => 1),
			"humidity"=>array('count'=>0,'value'=>null,'decimals'=>2),
			"pressure"=>array('count'=>0,'value'=>null,'decimals'=>0),
			"visibility"=>array('count'=>0,'value'=>null,'decimals'=>0)
			);


			$average_values = CurrentWeatherData::getAverages($data_array,$average_values);

			//TODO provide alteranative source if location meta data is null
			//meta data object
			$meta = null;
			$meta_location = null;
			$timezone = null;

			//the first non-null description is chosen
			$description = null;
			//first non null windBaring value is chosen
			//TODO fix windBaring average
			$windBaring = null;
			//first non null value is chosen for houly and daily description
			$hourly_description = null;
			$daily_description = null;

			//arrays of arrays from sources
			$daily_data = array();
			$hourly_data = array();




			foreach($data_array as $current_obj){

				//first non-null meta data is chosen
				if(isset($current_obj->metaData)){
					$current_meta = $current_obj->metaData;
					if(isset($current_meta->location)){
						$meta_location = $current_meta->location;
					}

					if(isset($current_meta->timezone)){
						$timezone = $current_meta->timezone;
					}
				}

				if($description == null && isset($current_obj->description)){
					$description = $current_obj->description;
				}

				if($windBaring == null && isset($current_obj->windBaring)){
					$windBaring = $current_obj->windBaring;
				}

				if(isset($current_obj->hourly)){
					$current_hourly = $current_obj->hourly;

					if(isset($current_hourly->description) && $hourly_description == null){
						$hourly_description = $current_hourly->description;
					}

					if(isset($current_hourly->data)){
						array_push($hourly_data, $current_hourly->data);
					}
				}

				if(isset($current_obj->daily)){
					$current_daily = $current_obj->daily;

					if(isset($current_daily->description) && $daily_description == null){
						$daily_description = $current_daily->description;
					}

					if(isset($current_daily->data)){
						array_push($daily_data, $current_daily->data);
					}
				}
			}

			$averaged_hourly_data = null;
			$averaged_daily_data = null;

			//average array arrays is not null
			if(!empty($hourly_data)){
				$averaged_hourly_data = DataArray::averageArrayData($hourly_data,true);
			}

			if(!empty($daily_data)){
				$averaged_daily_data = DataArray::averageArrayData($daily_data,false);
			}

			$meta = new MetaData(null,$meta_location,$timezone);

			$daily = new DataArray($averaged_daily_data,$daily_description);
			$hourly = new DataArray($averaged_hourly_data,$hourly_description);



			$icon = CurrentWeatherData::getIcon($data_array);


			//create new obj from averaged data
			return new CurrentWeatherData($description,$icon,$average_values['precip']['value'],$average_values['precipProb']['value'],
			$average_values['temp']['value'],$average_values['cloudCover']['value'],$average_values['wind']['value'],$windBaring,$average_values['nearestStorm']['value'],$hourly,$daily,$meta,$average_values['humidity']
			['value'],$average_values['pressure']['value'],$average_values['visibility']['value']);

		}

		//returns averages for vars in array as pulled from objects in object_array rounded to 'decimals'
		//var array must be an array in format [name[count = 0,value = default value, decimals = # decimals to round], . . .]
		//name of key in array must be same as property in target obj
		//any prop in object may be null so all are checked and have independent source counts
		//count is num of sources used for average, value is value, decimals is # of places after decimal to round
		public static function getAverages($object_array,$var_array)
		{
			//go through every obj
			foreach ($object_array as $current_source) {
				//add data from obj property to appropriate value in array
				foreach ($var_array as $name => &$current_var) {
					if(isset($current_source->$name)){
						//some values in array can have null as defalut value and are reset to 0 to be incremented
						if($current_var['value'] == null){$current_var['value'] = 0;}

						$current_var['value']+=$current_source->$name;
						$current_var['count']++;
					}
				}
			}

			//average values from counts
			foreach ($var_array as &$var) {
				if($var['count']>0){

					$var['value']/=$var['count'];

					//round value
					$var['value'] = round($var['value'],$var['decimals']);
				}

			}

			return $var_array;
		}

		//chooses icon from data_array
		//looks for mode icon; if none the most specific is chosed (icon code > 10); otherwise first mode
		public static function getIcon($data_array){
			$icon = null;
			//array in form [icon=># of times it occures, . . .]
			$icon_choices = array();

			foreach($data_array as $current_obj){
				if(isset($current_obj->icon)){
					//if icon is already in array increment otherwise create it
					if(isset($icon_choices[$current_obj->icon])){
						$icon_choices[$current_obj->icon]++;
					} else {
						$icon_choices[$current_obj->icon] = 1;
					}
				}
			}

			$most_specific = null;

			$mode = 0;
			//whether or not there are more than 1 modes
			$isMode = true;
			//$key is name of icon; $value is num of times it occured in
			foreach ($icon_choices as $key => $value) {
				//specific icons are stored in case there are more than one modes
				if(intval($key) >= 10){$most_specific = $key;}

				if($value == $mode){
					$isMode = false;
				} else if($value > $mode){
					$icon = $key;
					$mode = $value;
					$isMode = true;
				}
			}

			//if there are more than one modes choose the most specific
			if(!$isMode){
				if(isset($most_specific)){$icon = $most_specific;}
			}

			return $icon;
		}

	}



	//one block in hourly weather data array
	class HourlyDataBlock{
		public $time,$icon,$description,$precip,$precipProb,$temp,$wind;

		public function __construct($time,$icon,$description,$precip,$precipProb,$temp,$wind){

			$this->time=$time;$this->icon=$icon;$this->description=$description;$this->precip=$precip;$this->precipProb=$precipProb;
			$this->temp=$temp;$this->wind=$wind;
		}

		//returns a new HourlyDataBlock from an array of other HourlyDataBlocks that all have the same time
		public static function newAverageHourlyBlock($data_blocks){

			$values = array(
				"temp"=> array('count' => 0, 'value' => null, 'decimals' => 0),
			 	"precip"=> array('count' => 0, 'value' => 0, 'decimals' => 4),
				"precipProb"=> array('count' => 0, 'value' => 0, 'decimals' => 2),
				"wind"=> array('count' => 0, 'value' => 0, 'decimals' => 0),
			);

			$values = CurrentWeatherData::getAverages($data_blocks,$values);

			$icon = CurrentWeatherData::getIcon($data_blocks);

			//the first non-null description is chosen
			$description = null;
			//the first time value is chosen
			$time = null;

			foreach($data_blocks as $current_obj){

				if($description == null && isset($current_obj->description)){
					$description = $current_obj->description;
				}

				if($time == null){$time = $current_obj->time;}
			}

			return new HourlyDataBlock($time,$icon,$description,$values['precip']['value'],$values['precipProb']['value'],
			$values['temp']['value'],$values['wind']['value']);

		}
	}

	//one block in daily weather data array
	class DailyDataBlock{
		public $time,$icon,$description,$precip,$precipProb,$tempMin,$tempMax,$wind,$cloudCover,$humidity,$sunrise,$sunset,$tempMinTime,$tempMaxTime,$windBaring,$visibility,$pressure;

		public function __construct($time,$icon,$description,$precip,$precipProb,$tempMin,$tempMax,$wind,$cloudCover,$humidity,$sunrise,$sunset,$tempMinTime,$tempMaxTime,$windBaring,$visibility,$pressure){

			$this->time=$time;$this->icon=$icon;$this->description=$description;$this->precip=$precip;$this->precipProb=$precipProb;
			$this->tempMin=$tempMin;$this->tempMax = $tempMax;$this->wind=$wind;$this->cloudCover = $cloudCover;$this->humidity=$humidity;
			$this->sunrise=$sunrise;$this->sunset=$sunset;$this->tempMinTime=$tempMinTime;$this->tempMaxTime=$tempMaxTime;$this->windBaring=$windBaring;$this->visibility=$visibility;$this->pressure=$pressure;
		}

		//returns a new DailyDataBlock from an array of other DailyDataBlocks that all have the same time
		public static function newAverageDailyBlock($data_blocks){

			$values = array(
				"tempMin"=> array('count' => 0, 'value' => null, 'decimals' => 0),
				"tempMax"=> array('count' => 0, 'value' => null, 'decimals' => 0),
			 	"precip"=> array('count' => 0, 'value' => 0, 'decimals' => 4),
				"precipProb"=> array('count' => 0, 'value' => 0, 'decimals' => 2),
				"cloudCover"=> array('count' => 0, 'value' => 0, 'decimals' => 2),
				"wind"=> array('count' => 0, 'value' => 0, 'decimals' => 0),
				"humidity"=> array('count' => 0, 'value' => 0, 'decimals' => 2),
				"sunset"=> array('count' => 0, 'value' => null, 'decimals' => 0),
				"sunrise"=> array('count' => 0, 'value' => null, 'decimals' => 0),
				"tempMinTime"=> array('count' => 0, 'value' => null, 'decimals' => 0),
				"tempMaxTime"=> array('count' => 0, 'value' => null, 'decimals' => 0),
				"windBaring"=> array('count' => 0, 'value' => null, 'decimals' => 0),
				"visibility"=> array('count' => 0, 'value' => null, 'decimals' => 0),
				"pressure"=> array('count' => 0, 'value' => null, 'decimals' => 0)
			);

			$values = CurrentWeatherData::getAverages($data_blocks,$values);

			$icon = CurrentWeatherData::getIcon($data_blocks);

			//the first non-null description is chosen
			$description = null;
			//the first time value is chosen
			$time = null;

			foreach($data_blocks as $current_obj){

				if($description == null && isset($current_obj->description)){
					$description = $current_obj->description;
				}

				if($time == null){$time = $current_obj->time;}
			}

			return new DailyDataBlock($time,$icon,$description,$values['precip']['value'],$values['precipProb']['value'],
			$values['tempMin']['value'],$values['tempMax']['value'],$values['wind']['value'],$values['cloudCover']['value'],$values['humidity']['value'],$values['sunrise']['value'],$values['sunset']['value'],
			$values['tempMinTime']['value'],$values['tempMaxTime']['value'],$values['windBaring']['value'],$values['visibility']['value'],$values['pressure']['value']);

		}
	}

	class MetaData{
		public $time,$location,$timezone;

		public function __construct($time,$location,$timezone){
			$this->time = $time;$this->location = $location;$this->timezone = $timezone;
		}
	}

	//obj used for daily and hourly data
	class DataArray{
		public $description,$data;

		public function __construct($data,$description){
			$this->data = $data;
			$this->description = $description;
		}

		//returns array of average data from $data using either hourly or daily funcs based on $isHourly data
		//$data must be array of arrays to be averaged all objs must be same type
		//$data has at least one array with at least one obj
		public static function averageArrayData($data,$isHourly){
			$average_array = array();

			while(!empty($data)){
				//find the min time in the future to get next chronological elements from array
				$times = array();
				foreach($data as $array){
					array_push($times,$array[0]->time);
				}
				$min_time = min($times);

				//array of data blocks that represent same time (min time)
				$data_blocks_for_min_time = array();

				foreach($data as $key => $array){
					//add all elements that represnt min time to array to be averaged
					if($array[0]->time - $min_time <= 1800){
						array_push($data_blocks_for_min_time,$array[0]);
						//shift array moving first value
						array_shift($data[$key]);
						//if array is now empty remove it from $data
						if(empty($data[$key])){unset($data[$key]);}
					}
				}

				//create new average block from array of elements for current time
				if($isHourly){
					array_push($average_array,HourlyDataBlock::newAverageHourlyBlock($data_blocks_for_min_time));
				} else {
					array_push($average_array,DailyDataBlock::newAverageDailyBlock($data_blocks_for_min_time));
				}

			}
			return $average_array;
		}
	}
