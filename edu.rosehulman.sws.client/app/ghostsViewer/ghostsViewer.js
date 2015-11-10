angular.module("swsClient.ghostsViewer", [
    'ngRoute'
])
    .config(['$routeProvider',
        function ($routeProvider) {
            $routeProvider.when("/ghostsViewer", {
                templateUrl: "ghostsViewer/ghostsViewer.html",
                controller: "GhostsViewerCtrl"
            });
        }])
    .controller("GhostsViewerCtrl", ["$scope", "$http",
        function ($scope, $http) {
            $scope.activeGhost = null;
            
            
    	
    		$scope.getGhosts = function () {
                var result = $http.get("http://localhost:8080/v1/GhostbustersDB/ghosts");
                result.success(function (response) {
                    $scope.ghosts = response.elements;
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                });
            };
            
            $scope.getGhosts();

            $scope.getGhost = function (id) {
                var result = $http.get("http://localhost:8080/v1/GhostbustersDB/ghosts/" + id);
                result.success(function (response) {
                    $scope.activeGhost = response.elements[0];
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                });
            };

            $scope.addGhost = function (name, type) {
                var json = {
                    name: name,
                    type: type
                };

                var result = $http.post("http://localhost:8080/v1/GhostbustersDB/ghosts", json);
                result.success(function () {
                    $scope.getGhosts();
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                });
            };

            $scope.updateGhost = function (id, name, type) {
                var json = {
                    name: name,
                    type: type
                };

                var result = $http.put("http://localhost:8080/v1/GhostbustersDB/ghosts/" + id, json);
                result.success(function () {
                    $scope.getGhosts();
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                });
            };

            $scope.deleteGhost = function (id) {
                var result = $http.delete("http://localhost:8080/v1/GhostbustersDB/ghosts/" + id);
                result.success(function () {
                    $scope.getGhosts();
                });

                result.error(function (data, status, headers, config) {
                    console.log(status);
                })
            }

        }]);














