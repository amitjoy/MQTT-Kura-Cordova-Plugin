import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class MqttPlugin extends CordovaPlugin {

	private static final String LOG_TAG = "MqttPlugin";

	private CallbackContext pluginCallbackContext = null;

	private final String clientID = null;
	private final String brokerUrl = null;
	private final String userName = null;
	private final String password = null;
	private String m_publishData = null;
	private String m_topic = null;

	final IKuraMQTTClient client = new KuraMQTTClient.Builder()
					.setHost("m20.cloudmqtt.com").setPort("11143")
					.setClientId("CLIENT_1294378").setUsername("user@email.com")
					.setPassword("iotiwbiot").build();

	// Connect to the Message Broker
	final boolean status = client.connect();
	System.out.println(status);

	// args = [url, username, password, clientID, topic]
	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
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
				final JSONObject object = new JSONObject();
				final Map<String, Object> metrics = payload.metrics();
				for (final Map.Entry entry : metrics.entrySet()) {
					try {
						object.put((String) entry.getKey(), entry.getValue());
					} catch (final JSONException e) {
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
			final PluginResult result = new PluginResult(
					PluginResult.Status.OK, info);
			result.setKeepCallback(keepCallback);
			this.pluginCallbackContext.sendPluginResult(result);
		}
	}

	private void sendUpdate(JSONObject info, boolean keepCallback) {
		if (this.pluginCallbackContext != null) {
			final PluginResult result = new PluginResult(
					PluginResult.Status.OK, info);
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

	private void setOpts(JSONArray args) throws JSONException {
		/*
		 * this.brokerUrl = (String) args.get(0); this.userName = (String)
		 * args.get(1); this.password = (String) args.get(2); this.clientID =
		 * (String) args.get(3);
		 */
		this.m_publishData = (String) args.get(0);
		this.m_topic = (String) args.get(1);

	}

}