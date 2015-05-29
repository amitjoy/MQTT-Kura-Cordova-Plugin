cordova.define("org.pluginporo.mqttplugin", function(require, exports, module) {
var exec = require('cordova/exec');
function MqttPlugin() { alert("MqttPlugin.js: is created");
}



MqttPlugin.prototype.publish = function(aString, param){ console.log("MqttPlugin.js: publish");
alert(aString + " " + param.data + " " + param.topic);
var conConf = [param.data, param.topic];

exec(
function(result){ /*alert("OK" + reply);*/ },
function(result){ /*alert("Error" + reply);*/ },
"MqttPlugin",
"publish",
conConf);
};

MqttPlugin.prototype.subscribe = function(aString, param){ console.log("MqttPlugin.js: Subscribe");
alert(aString + " " + param.data + " " + param.topic);
var str = aString.toUpperCase();
var conConf = [param.data, param.topic];

exec(
function(result){ /*alert("OK" + reply);*/ },
function(result){ /*alert("Error" + reply);*/ },
"MqttPlugin",
"subscribe",
conConf);
};

var MqttPlugin = new MqttPlugin(); 
module.exports = MqttPlugin;
});