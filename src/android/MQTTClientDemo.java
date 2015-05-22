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
package org.pluginporo.mqtt;

import java.io.UnsupportedEncodingException;

public class MQTTClientDemo {

	public static void main(String[] args) {

		final SimpleMQTTClient sc = new SimpleMQTTClient("iot.eclipse.org");
		final String CONF_REQUEST_TOPIC = "$EDC/administrator/AMIT/CONF-V1/GET/configurations/de.tum.in.hvac.HVAC";
		final String CONF_RESPONSE_TOPIC = "$EDC/administrator/AMIT_083027868/CONF-V1/REPLY/55361535117";
		final String HVAC_REQUEST_TOPIC = "$EDC/administrator/AMIT/HVAC-V1/GET/configurations";
		final String HVAC_RESPONSE_TOPIC = "$EDC/administrator/AMIT_083027868/HVAC-V1/REPLY/55361535117";
		final String BUNDLE_REQUEST_TOPIC = "$EDC/administrator/AMIT/DEPLOY-V1/GET/bundles";
		final String BUNDLE_RESPONSE_TOPIC = "$EDC/administrator/AMIT_083027868/DEPLOY-V1/REPLY/55361535117";

		sc.subscribe(CONF_RESPONSE_TOPIC, new MessageListener() {

			@Override
			public void processMessage(KuraPayload payload) {
				try {
					System.out.println(new String(payload.getBody(), "UTF-8"));
				} catch (final UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		});

		final KuraPayload payload = new KuraPayload();
		payload.addMetric("request.id", "55361535117");
		payload.addMetric("requester.client.id", "AMIT_083027868");

		sc.publish(CONF_REQUEST_TOPIC, payload);

		System.out.println("Subscribed to channels "
				+ sc.getSubscribedChannels());

		System.out.println("Waiting for new messages");

		while (!Thread.currentThread().isInterrupted()) {
		}

		sc.disconnect();
	}

}
