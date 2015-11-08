'use strict';

angular.module('swsClient.ajaxTest', [ 'ngRoute' ])

.config([ '$routeProvider', function($routeProvider) {
	$routeProvider.when('/ajaxTest', {
		templateUrl : "ajaxTest/ajaxTest.html",
		controller : "AjaxTestCtrl"
	});
} ])

.controller('AjaxTestCtrl', [ "$scope", "$http", function($scope, $http) {
	$scope.getHello = function () {
		$http.get("http://localhost:8080/BasicPlugin/HelloServlet")
			.success(function(response) {
				$scope.hello = response.hello;
			});
	}
} ]);