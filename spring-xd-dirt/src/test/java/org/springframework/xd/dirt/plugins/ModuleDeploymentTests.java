/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.plugins;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.BusTestUtils;
import org.springframework.messaging.Message;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class ModuleDeploymentTests {

	// run redis-server and RedisContainerLauncher (or StreamServer) before this test

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	private static String deployerQueue = "queue.deployer." + System.currentTimeMillis();

	@Test
	public void testProcessor() throws Exception {
		LettuceConnectionFactory connectionFactory = redisAvailableRule.getResource();
		RedisQueueOutboundChannelAdapter adapter = new RedisQueueOutboundChannelAdapter(deployerQueue,
				connectionFactory);
		adapter.setBeanFactory(BusTestUtils.MOCK_BF);
		adapter.setExtractPayload(false);
		adapter.afterPropertiesSet();
		ModuleDeploymentRequest request = new ModuleDeploymentRequest.Builder()
				.setGroup("test")
				.setType(ModuleType.processor)
				.setModuleName("filter")
				.setIndex(0)
				.build();
		Message<?> message = MessageBuilder.withPayload(request.toString()).build();
		adapter.handleMessage(message);
	}

	@Test
	public void testSimpleStream() throws Exception {
		LettuceConnectionFactory connectionFactory = redisAvailableRule.getResource();
		RedisQueueOutboundChannelAdapter adapter = new RedisQueueOutboundChannelAdapter(deployerQueue,
				connectionFactory);
		adapter.setBeanFactory(BusTestUtils.MOCK_BF);
		adapter.setExtractPayload(false);
		adapter.afterPropertiesSet();
		ModuleDeploymentRequest sinkRequest = new ModuleDeploymentRequest.Builder()
				.setGroup("teststream")
				.setType(ModuleType.sink)
				.setModuleName("log")
				.setIndex(1)
				.build();
		Message<?> sinkMessage = MessageBuilder.withPayload(sinkRequest.toString()).build();
		adapter.handleMessage(sinkMessage);
		ModuleDeploymentRequest sourceRequest = new ModuleDeploymentRequest.Builder()
				.setGroup("teststream")
				.setType(ModuleType.source)
				.setModuleName("time")
				.setIndex(0)
				.build();
		Message<?> sourceMessage = MessageBuilder.withPayload(sourceRequest.toString()).build();
		adapter.handleMessage(sourceMessage);
	}

	@After
	public void cleanupQueue() {
		StringRedisTemplate template = new StringRedisTemplate(redisAvailableRule.getResource());
		template.delete(deployerQueue);
	}

}
