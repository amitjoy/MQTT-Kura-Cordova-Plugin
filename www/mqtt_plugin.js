var cordova = require('cordova');
var exec = require('cordova/exec');

var MqttPlugin = function() {

	// opts must have {url, userName, password, clientID, topics}
	this.subscribe = function(opts, success_cb, error_cb){
		var conConf = [opts.topic];
		exec(success_cb, error_cb, "MqttPlugin", "subscribe", conConf);
	};

	this.publish = function(opts, success_cb, error_cb){
		var conConf = [opts.data, opts.topic];
		exec(success_cb, error_cb, "MqttPlugin", "publish", conConf);
	};

};

module.exports = MqttPlugin;