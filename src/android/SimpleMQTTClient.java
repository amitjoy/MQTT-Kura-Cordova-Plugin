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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

public class SimpleMQTTClient {

	// Client
	protected CallbackConnection connection = null;
	// List of channels we're subscribed to
	protected Map<String, MessageListener> channels = null;

	/**
	 * Creates a simple MQTT client and connects it to the specified MQTT broker
	 *
	 * @param host
	 *            the hostname of the broker
	 * @param clientId
	 *            the UNIQUE id of this client
	 */
	public SimpleMQTTClient(String host, String clientId, String username, String password) {
		// Create fusesource MQTT client
		final MQTT mqtt = new MQTT();
		try {
			mqtt.setHost(hostToURI(host));
			mqtt.setClientId(clientId);
			mqtt.setUserName(username);
			mqtt.setPassword(password);
		} catch (final URISyntaxException e) {
			System.out
					.println("Are you sure you specified host correctly? Terminating...");
		}
		// Initialize channels
		channels = new HashMap();
		// Register callbacks
		connection = mqtt.callbackConnection();
		connection.listener(new Listener() {
			@Override
			public void onConnected() {
			}

			@Override
			public void onDisconnected() {
			}

			@Override
			public void onPublish(UTF8Buffer mqttChannel, Buffer mqttMessage,
					Runnable ack) {
				if (channels.containsKey(mqttChannel.toString())) {
					final KuraPayloadDecoder decoder = new KuraPayloadDecoder(
							mqttMessage.toByteArray());
					try {
						channels.get(mqttChannel.toString()).processMessage(
								decoder.buildFromByteArray());
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
				ack.run();
			}

			@Override
			public void onFailure(Throwable throwable) {
			}
		});
		// Connect to broker in a blocking fashion
		final CountDownLatch l = new CountDownLatch(1);
		connection.connect(new Callback<Void>() {
			@Override
			public void onSuccess(Void aVoid) {
				l.countDown();
			}

			@Override
			public void onFailure(Throwable throwable) {
				System.err
						.println("Impossible to CONNECT to the MQTT server, terminating");
				System.exit(1);
			}
		});
		try {
			if (!l.await(5, TimeUnit.SECONDS)) {
				// Waits 3 seconds and then timeouts
				System.err
						.println("Impossible to CONNECT to the MQTT server: TIMEOUT. Terminating");
				System.exit(1);
			}
		} catch (final InterruptedException e) {
			System.err
					.println("Impossible to CONNECT to the MQTT server, terminating");
			System.exit(1);
		}
	}

	/**
	 * Creates a simple MQTT client and connects it to the specified MQTT broker
	 *
	 * @param host
	 *            the hostname of the broker we are trying to connect to
	 */
	public SimpleMQTTClient(String host) {
		this(host, null, null, null);
	}

	/**
	 * Subscribes to a channel and registers a callback that is fired every time
	 * a new message is published on the channel.
	 *
	 * @param channel
	 *            the channel we are subscribing to
	 * @param callback
	 *            the callback to be fired whenever a message is received on
	 *            this channel
	 */
	public void subscribe(final String channel, final MessageListener callback) {
		if (connection != null) {
			if (channels.containsKey(channel))
				return;
			final CountDownLatch l = new CountDownLatch(1);
			final Topic[] topic = { new Topic(channel, QoS.AT_MOST_ONCE) };
			connection.subscribe(topic, new Callback<byte[]>() {
				@Override
				public void onSuccess(byte[] bytes) {
					channels.put(channel, callback);
					l.countDown();
				}

				@Override
				public void onFailure(Throwable throwable) {
					System.err.println("Impossible to SUBSCRIBE to channel \""
							+ channel + "\"");
					l.countDown();
				}
			});
			try {
				l.await();
			} catch (final InterruptedException e) {
				System.err.println("Impossible to SUBSCRIBE to channel \""
						+ channel + "\"");
			}
		}
	}

	/**
	 * Unsubscribes from a channel.
	 *
	 * @param channel
	 *            the channel we are unsubscribing to
	 */
	public void unsubscribe(String channel) {
		if (connection != null) {
			channels.remove(channel);
			final UTF8Buffer[] topic = { UTF8Buffer.utf8(channel) };
			connection.unsubscribe(topic, new Callback<Void>() {
				@Override
				public void onSuccess(Void aVoid) {
				}

				@Override
				public void onFailure(Throwable throwable) {
				}
			});
		}
	}

	/**
	 * Returns the channels the client is currently subscribed to.
	 *
	 * @return set of channels the client is currently subscribed to
	 */
	public Set<String> getSubscribedChannels() {
		return channels.keySet();
	}

	/**
	 * Publish a message to a channel
	 *
	 * @param channel
	 *            the channel we are publishing to
	 * @param message
	 *            the message we are publishing
	 */
	public void publish(final String channel, KuraPayload payload) {
		if (connection != null) {
			final KuraPayloadEncoder encoder = new KuraPayloadEncoder(payload);
			try {
				connection.publish(channel, encoder.getBytes(),
						QoS.AT_MOST_ONCE, false, new Callback<Void>() {
							@Override
							public void onSuccess(Void aVoid) {
							}

							@Override
							public void onFailure(Throwable throwable) {
								System.out
										.println("Impossible to publish message to channel "
												+ channel);
							}
						});
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Disconnects the client.
	 */
	public void disconnect() {
		if (connection != null) {
			connection.disconnect(new Callback<Void>() {
				@Override
				public void onSuccess(Void aVoid) {
				}

				@Override
				public void onFailure(Throwable throwable) {
				}
			});
		}
	}

	private String hostToURI(String host) {
		return "tcp://" + host + ":17193";
	}

}
