/*******************************************************************************
 * Copyright (C) 2015 - Amit Kumar Mondal <admin@amitinside.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

import com.google.common.base.Charsets;

public final class MQTTClientDemo {

	public static void main(String... args) {

		// Create the connection object
		// In the mobile application, the client should not know the username
		// and password until it gets verified with the identity server and the
		// server will provide the mobile client with the username and password
		// to connect to the broker
		// The client id would always be one randomly generated id for one
		// mobile client
		// but there will be a specific user for identity server and IoT Gateway
		// to connect to the message broker
		// First the client will ask (using REST call) Identity Server for
		// credential to connect
		// to message broker and the server checks its database if the user is
		// validated, then it will return the credential for that user to
		// connect to the message broker and the client will use it for further
		// usages. Once it is done, the client will scan QR code to add newly
		// purchased IoT Home Automation Gateway Solution
		final IKuraMQTTClient client = new KuraMQTTClient.Builder()
				.setHost("m20.cloudmqtt.com").setPort("11143")
				.setClientId("CLIENT_1294378").setUsername("user@email.com")
				.setPassword("iotiwbiot").build();

		// Connect to the Message Broker
		final boolean status = client.connect();
		System.out.println(status);

		// Declare the topics
		final String CONF_REQUEST_TOPIC = "$EDC/TUM/B8:27:EB:A6:A9:8A/BLUETOOTH-V1/GET/configurations";
		final String CONF_RESPONSE_TOPIC = "$EDC/TUM/CLIENT_1294378/BLUETOOTH-V1/REPLY/55361535117";

		// Subscribe to the topic first
		if (status)
			client.subscribe(CONF_RESPONSE_TOPIC, new MessageListener() {

				@Override
				public void processMessage(KuraPayload payload) {
					System.out.println(new String(payload.getBody(),
							Charsets.UTF_8));
				}
			});

		// Then publish the message
		final KuraPayload payload = new KuraPayload();
		// Request Id will always be randomly generated for each and every MQTT
		// request
		payload.addMetric("request.id", "55361535117");
		payload.addMetric("requester.client.id", "CLIENT_1294378");

		if (status)
			client.publish(CONF_REQUEST_TOPIC, payload);

		System.out.println("Subscribed to channels "
				+ client.getSubscribedChannels());

		System.out.println("Waiting for new messages");

		// The threadding is done for the sake of this example but in real-life
		// scenario, you don't need this
		while (!Thread.currentThread().isInterrupted()) {
		}

		// Finally disconnect
		client.disconnect();
	}
}
