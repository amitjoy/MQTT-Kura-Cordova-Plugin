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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * Implementation of {@link IKuraMQTTClient}
 * 
 * @author AMIT KUMAR MONDAL
 * @see IKuraMQTTClient
 *
 */
public class KuraMQTTClient implements IKuraMQTTClient {

	/**
	 * Connection Params
	 */
	private final String host;
	private final String port;
	private final String clientId;
	private final String username;
	private final String password;

	private String errorMsg;
	private boolean isConnected;
	private final Lock connectionLock;

	protected CallbackConnection connection = null;

	protected Map<String, MessageListener> channels = null;

	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(KuraMQTTClient.class);

	/**
	 * Creates a simple MQTT client and connects it to the specified MQTT broker
	 *
	 * @param host
	 *            the hostname of the broker
	 * @param clientId
	 *            the UNIQUE id of this client
	 */
	private KuraMQTTClient(String host, String port, String clientId,
			String username, String password) {
		this.host = host;
		this.port = port;
		this.clientId = clientId;
		this.username = username;
		this.password = password;
		connectionLock = new ReentrantLock();
	}

	/** {@inheritDoc} */
	@Override
	public boolean connect() {

		Preconditions.checkNotNull(host);
		Preconditions.checkNotNull(port);
		Preconditions.checkNotNull(clientId);

		final MQTT mqtt = new MQTT();

		try {

			mqtt.setHost(hostToURI(host, port));
			mqtt.setClientId(clientId);
			mqtt.setPassword(password);
			mqtt.setUserName(username);

		} catch (final URISyntaxException e) {
			LOGGER.error(Throwables.getStackTraceAsString(e));
		}
		try {
			if (connectionLock.tryLock(5, TimeUnit.SECONDS)) {
				safelyConnect(mqtt);
			}
			isConnected = true;
		} catch (final InterruptedException e) {
			isConnected = false;
		} catch (final ConnectionException e) {
			isConnected = false;
		} finally {
			connectionLock.unlock();
		}
		return isConnected;
	}

	/**
	 * Connect in a thread safe manner
	 */
	private void safelyConnect(final MQTT mqtt) throws ConnectionException {
		if (isConnected)
			disconnect();
		// Initialize channels
		channels = new HashMap<>();
		// Register callbacks
		connection = mqtt.callbackConnection();
		connection.listener(new Listener() {
			@Override
			public void onConnected() {
				LOGGER.debug("Host connected");
			}

			@Override
			public void onDisconnected() {
				LOGGER.debug("Host disconnected");
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
						LOGGER.debug("I/O Exception Occurred: "
								+ e.getMessage());
					}
				}
				ack.run();
			}

			@Override
			public void onFailure(Throwable throwable) {
				LOGGER.debug("Exception Occurred: " + throwable.getMessage());
			}
		});
		// Connect to broker in a blocking fashion
		final CountDownLatch l = new CountDownLatch(1);
		connection.connect(new Callback<Void>() {
			@Override
			public void onSuccess(Void aVoid) {
				l.countDown();
				LOGGER.debug("Successfully Connected to Host");
			}

			@Override
			public void onFailure(Throwable throwable) {
				errorMsg = "Impossible to CONNECT to the MQTT server, terminating";
				LOGGER.debug(errorMsg);
			}

		});
		try {
			if (!l.await(5, TimeUnit.SECONDS)) {
				errorMsg = "Impossible to CONNECT to the MQTT server: TIMEOUT. Terminating";
				LOGGER.debug(errorMsg);
				exceptionOccurred(errorMsg);
			}
		} catch (final InterruptedException e) {
			errorMsg = "\"Impossible to CONNECT to the MQTT server, terminating\"";
			LOGGER.debug(errorMsg);
			exceptionOccurred(errorMsg);

		}
	}

	/**
	 * Connection Exception triggerer
	 */
	private void exceptionOccurred(String message) throws ConnectionException {
		throw new ConnectionException(message);
	}

	/** {@inheritDoc} */
	@Override
	public String getHost() {
		return host;
	}

	/** {@inheritDoc} */
	@Override
	public String getClientId() {
		return clientId;
	}

	/** {@inheritDoc} */
	@Override
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
					LOGGER.debug("Successfully subscribed to " + channel);
				}

				@Override
				public void onFailure(Throwable throwable) {
					LOGGER.debug("Impossible to SUBSCRIBE to channel \""
							+ channel + "\"");
					l.countDown();
				}
			});
			try {
				l.await();
			} catch (final InterruptedException e) {
				LOGGER.debug("Impossible to SUBSCRIBE to channel \"" + channel
						+ "\"");
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void unsubscribe(String channel) {
		if (connection != null) {
			channels.remove(channel);
			final UTF8Buffer[] topic = { UTF8Buffer.utf8(channel) };
			connection.unsubscribe(topic, new Callback<Void>() {
				@Override
				public void onSuccess(Void aVoid) {
					LOGGER.debug("Successfully unsubscribed");
				}

				@Override
				public void onFailure(Throwable throwable) {
					LOGGER.debug("Exception occurred while unsubscribing: "
							+ throwable.getMessage());
				}
			});
		}
	}

	/** {@inheritDoc} */
	@Override
	public Set<String> getSubscribedChannels() {
		return channels.keySet();
	}

	/** {@inheritDoc} */
	@Override
	public void publish(final String channel, KuraPayload payload) {
		if (connection != null) {
			final KuraPayloadEncoder encoder = new KuraPayloadEncoder(payload);
			try {
				connection.publish(channel, encoder.getBytes(),
						QoS.AT_MOST_ONCE, false, new Callback<Void>() {
							@Override
							public void onSuccess(Void aVoid) {
								LOGGER.debug("Successfully published");
							}

							@Override
							public void onFailure(Throwable throwable) {
								LOGGER.debug("Impossible to publish message to channel "
										+ channel);
							}
						});
			} catch (final IOException e) {
				LOGGER.debug("I/O Exception Occurred: " + e.getMessage());
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void disconnect() {
		try {
			connectionLock.tryLock(5, TimeUnit.SECONDS);
			safelyDisconnect();
		} catch (final Exception e) {
			LOGGER.debug("Exception while disconnecting");
		}
	}

	/**
	 * Disconnects the client in a thread safe way
	 */
	private void safelyDisconnect() {
		if (connection != null) {
			connection.disconnect(new Callback<Void>() {
				@Override
				public void onSuccess(Void aVoid) {
					LOGGER.debug("Successfully disconnected");
				}

				@Override
				public void onFailure(Throwable throwable) {
					LOGGER.debug("Error while disconnecting");
				}
			});
		}
	}

	public static class Builder {

		private String host;
		private String port;
		private String clientId;
		private String username;
		private String password;

		public Builder setHost(String host) {
			this.host = host;
			return this;
		}

		public Builder setPort(String port) {
			this.port = port;
			return this;
		}

		public Builder setClientId(String clientId) {
			this.clientId = clientId;
			return this;
		}

		public Builder setUsername(String username) {
			this.username = username;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public KuraMQTTClient build() {
			return new KuraMQTTClient(host, port, clientId, username, password);
		}
	}

	/**
	 * Returns the MQTT URI Scheme
	 */
	private String hostToURI(String host, String port) {
		return PROTOCOL + "://" + host + ":" + port;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isConnected() {
		return isConnected;
	}

}
