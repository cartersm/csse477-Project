'use strict';

// Declare app level module which depends on views, and components
angular.module('swsClient', [
  'ngRoute',
  'swsClient.ajaxTest'
]).
config(['$routeProvider', '$httpProvider', function($routeProvider, $httpProvider) {
  $routeProvider.otherwise({redirectTo: '/ajaxTest'});
  $httpProvider.defaults.useXDomain = true;
  delete $httpProvider.defaults.headers.common['X-Requested-With'];
}]);
