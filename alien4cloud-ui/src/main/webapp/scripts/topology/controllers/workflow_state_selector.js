define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  require('angular-ui-ace');

  require('angular-tree-control');

  modules.get('a4c-topology-editor').controller('WfStateSelectorController', [
              '$scope',
              '$modalInstance',
              'topologyDTO',
              function($scope, $modalInstance, topologyDTO) {

    $scope.topologyDTO = topologyDTO;
    
    $scope.selectedNodeTemplate = undefined;
    $scope.selectedState = undefined;
    
    $scope.nodeTemplateItems = [];
    $scope.stateItems = [];
    
    for (var nodeId in topologyDTO.topology.nodeTemplates) {
      $scope.nodeTemplateItems.push(nodeId);
    }
    
    $scope.selectNodeTemplate = function(nodeId) {
      $scope.selectedNodeTemplate = nodeId;
      $scope.stateItems = [];
      $scope.selectedState = undefined;
      $scope.stateItems = ['initial', 'creating', 'created', 'configuring', 'configured', 'starting', 'started', 'stopping', 'stopped', 'deleting', 'deleted'];
    }
    
    $scope.selectState = function(stateName) {
      $scope.selectedState = stateName
    }
    
    $scope.submit = function() {
      $modalInstance.close({node: $scope.selectedNodeTemplate, state: $scope.selectedState});
    }
    
    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  }]);
});
