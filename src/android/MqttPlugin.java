package org.pluginporo.mqtt;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.StrictMode;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;

public class MqttPlugin extends CordovaPlugin {

    private static final String LOG_TAG = "MqttPlugin";

    CallbackContext pluginCallbackContext = null;

    String clientID = null;
    String brokerUrl = null;
    String userName = null;
    String password = null;
    String m_publishData = null;
    String m_topic = null;

    static SimpleMQTTClient s_simpleClient = null;

    static {
        s_simpleClient = new SimpleMQTTClient("m11.cloudmqtt.org", "ankur", "rvigfsyx", "ifCTVLo5YlP9");
    }

    // args = [url, username, password, clientID, topic]
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        if (action.equals("subscribe")) {
            this.setOpts(args);
            this.pluginCallbackContext = callbackContext;
            subscribe();
            return true;
        } else if (action.equals("stop")) {
            callbackContext.success("stopped");
            return true;
        } else if (action.equals("publish")) {
            this.setOpts(args);
            publish();
            this.pluginCallbackContext = callbackContext;
            return true;
        }
        return false;
    }


    private void publish() {
        final KuraPayload payload = new KuraPayload();
        payload.addMetric("request.id", "55361535117");
        payload.addMetric("requester.client.id", "AMIT_083027868");
        payload.setBody(m_publishData.getBytes());
        s_simpleClient.publish(m_topic, payload);
    }

    private void subscribe() {
        s_simpleClient.subscribe(m_topic, new MessageListener() {
            @Override
            public void processMessage(KuraPayload payload) {
                JSONObject object = new JSONObject();
                Map<String, Object> metrics = payload.metrics();
                for (Map.Entry entry : metrics.entrySet()) {
                    try {
                        object.put((String) entry.getKey(), entry.getValue());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                sendUpdate(object, true);
            }
        });
    }

    private void disconnect() {

    }

    private void sendUpdate(String info, boolean keepCallback) {
        if (this.pluginCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(keepCallback);
            this.pluginCallbackContext.sendPluginResult(result);
        }
    }

    private void sendUpdate(JSONObject info, boolean keepCallback) {
        if (this.pluginCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(keepCallback);
            this.pluginCallbackContext.sendPluginResult(result);
        }
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    // url, username, password, clientID, topic
    private void setOpts(JSONArray args) throws JSONException {
        /*this.brokerUrl = (String) args.get(0);
        this.userName = (String) args.get(1);
        this.password = (String) args.get(2);
        this.clientID = (String) args.get(3); */
        this.m_publishData = (String) args.get(0);
        this.m_topic = (String) args.get(1);


    }

}