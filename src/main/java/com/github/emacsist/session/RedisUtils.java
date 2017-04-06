package com.github.emacsist.session;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtils {

	private static final String FILE_NAME = "emacsist.session.properties";

	private static final Properties jedisProperties = new Properties();
	static {
		try {
			InputStream is = RedisUtils.class.getClassLoader().getResourceAsStream("/" + FILE_NAME);
			if (is == null) {
				System.err.println("can not find file =>" + FILE_NAME + ", so init for default config");
			} else {
				jedisProperties.load(is);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final JedisPool JEDIS_POOL;

	static {
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

		boolean isBlockWhenExhausted = Boolean.parseBoolean(jedisProperties.getProperty("blockWhenExhausted", "true"));
		int maxIdle = Integer.parseInt(jedisProperties.getProperty("maxIdle", "10"));
		int maxTotal = Integer.parseInt(jedisProperties.getProperty("maxTotal", "20"));
		int minIdle = Integer.parseInt(jedisProperties.getProperty("minIdle", "5"));

		String host = jedisProperties.getProperty("redis.host", "127.0.0.1");
		int port = Integer.parseInt(jedisProperties.getProperty("redis.port", "6379"));

		jedisPoolConfig.setBlockWhenExhausted(isBlockWhenExhausted);
		jedisPoolConfig.setMaxIdle(maxIdle);
		jedisPoolConfig.setMaxTotal(maxTotal);
		jedisPoolConfig.setMinIdle(minIdle);
		JEDIS_POOL = new JedisPool(jedisPoolConfig, host, port);
		System.out.println("init session redis pool OK...");
	}

	public static JedisPool getJedisPool() {
		return JEDIS_POOL;
	}

}
