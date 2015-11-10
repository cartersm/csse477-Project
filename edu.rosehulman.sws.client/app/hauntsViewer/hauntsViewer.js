angular.module("swsClient.hauntsViewer", [
    'ngRoute'
])
    .config(['$routeProvider',
        function ($routeProvider) {
            $routeProvider.when("/hauntsViewer", {
                templateUrl: "hauntsViewer/hauntsViewer.html",
                controller: "HauntsViewerCtrl"
            });
        }])
    .controller("HauntsViewerCtrl", ["$scope", "$http",
        function ($scope, $http) {
            $scope.activeHaunt = null;
            
    		$scope.getHaunts = function () {
                var result = $http.get("http://localhost:8080/v1/GhostbustersDB/haunts");
                result.success(function (response) {
                    $scope.haunts = response.elements;
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                });
            };
            
            $scope.getHaunts();

            $scope.getHaunt = function (id) {
                var result = $http.get("http://localhost:8080/v1/GhostbustersDB/haunts/" + id);
                result.success(function (response) {
                    $scope.activeHaunt = response.elements[0];
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                });
            };

            $scope.addHaunt = function (name) {
                var json = {
                    name: name
                };

                var result = $http.post("http://localhost:8080/v1/GhostbustersDB/haunts", json);
                result.success(function () {
                    $scope.getHaunts();
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                });
            };

            $scope.updateHaunt = function (id, name) {
                var json = {
                    name: name
                };

                var result = $http.put("http://localhost:8080/v1/GhostbustersDB/haunts/" + id, json);
                result.success(function () {
                    $scope.getHaunts();
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                });
            };

            $scope.deleteHaunt = function (id) {
                var result = $http.delete("http://localhost:8080/v1/GhostbustersDB/haunts/" + id);
                result.success(function () {
                    $scope.getHaunts();
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                })
            }

        }]);














