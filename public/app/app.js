angular.module('logs', [])
.controller('LogCtrl', ['$scope', '$http', function ($scope, $http) {


    $scope.logs = new Array();

    $scope.start = function(){
        $http.get('/start').success(function(){
        });
    };
    $scope.stop = function(){
        $http.get('/stop').success(function(){
            //$scope.logs = new Array();
        });
    };

    var source = new EventSource("/feeds");
    source.addEventListener("message", function(event){
        $scope.$apply(function(){
            $scope.logs.push(JSON.parse(event.data));
        });
    });

}]);