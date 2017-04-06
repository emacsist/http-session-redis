package com.github.emacsist.session;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

@SuppressWarnings("deprecation")
public class RedisSession implements HttpSession {

	private static final int DEFAULT_INACTIVE_INTERVAL = 30 * 60;
	private String id;
	private ServletContext servletContext;
	private boolean isNew;

	public RedisSession(String id, ServletContext servletContext) {
		this(id, true, servletContext);
	}

	private Map<String, String> newDefaultSession(String id) {
		Map<String, String> defaultSession = new HashMap<>();
		long now = System.currentTimeMillis();
		defaultSession.put(RedisConstant.LAST_ACCESS_TIME, now + "");
		defaultSession.put(RedisConstant.CREATION_TIME, now + "");
		defaultSession.put(RedisConstant.MAX_INACTIVE_INTERVAL, DEFAULT_INACTIVE_INTERVAL + "");
		return defaultSession;
	}

	public RedisSession(String id, boolean isCreate, ServletContext servletContext) {
		if (id == null) {
			id = UUID.randomUUID().toString();
		}
		this.id = id;
		this.servletContext = servletContext;
		this.isNew = isCreate;

		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			Map<String, String> session = jedis.hgetAll(redisKey);
			if ((session == null || session.size() == 0) && isCreate) {
				session = newDefaultSession(id);
				jedis.hmset(redisKey, session);
			}
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public long getCreationTime() {
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			String creationTime = jedis.hget(redisKey, RedisConstant.CREATION_TIME);
			changeLastAccessTime(getMaxInactiveInterval());
			return Long.parseLong(creationTime);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public String getId() {
		return this.id;
	}

	public long getLastAccessedTime() {
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			String creationTime = jedis.hget(redisKey, RedisConstant.LAST_ACCESS_TIME);
			return Long.parseLong(creationTime);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public ServletContext getServletContext() {
		return this.servletContext;
	}

	public void setMaxInactiveInterval(int interval) {
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			Pipeline pipeline = jedis.pipelined();
			pipeline.hset(redisKey, RedisConstant.MAX_INACTIVE_INTERVAL, interval + "");
			pipeline.expire(redisKey, interval);
			pipeline.sync();
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public int getMaxInactiveInterval() {
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			String maxInactiveInterval = jedis.hget(redisKey, RedisConstant.MAX_INACTIVE_INTERVAL);
			return Integer.parseInt(maxInactiveInterval);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public HttpSessionContext getSessionContext() {
		return null;
	}

	public Object getAttribute(String name) {
		changeLastAccessTime(getMaxInactiveInterval());
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		Object object = null;
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			byte[] attrValue = jedis.hget(redisKey.getBytes(), (RedisConstant.SESSION_ATTR_PREFIX + name).getBytes());
			object = SerializeUtils.decode(attrValue);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return object;
	}

	public Object getValue(String name) {
		return getAttribute(name);
	}

	public Enumeration<String> getAttributeNames() {
		changeLastAccessTime(getMaxInactiveInterval());
		Set<String> attrNames = new HashSet<>();

		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			Set<String> redisHashKeys = jedis.hkeys(redisKey);
			for (String key : redisHashKeys) {
				if (key.startsWith(RedisConstant.SESSION_ATTR_PREFIX)) {
					attrNames.add(key);
				}
			}
			final Iterator<String> iterator = attrNames.iterator();
			return new Enumeration<String>() {
				@Override
				public boolean hasMoreElements() {
					return iterator.hasNext();
				}

				@Override
				public String nextElement() {
					return iterator.next();
				}
			};
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public String[] getValueNames() {
		return Collections.list(getAttributeNames()).toArray(new String[20]);
	}

	public void setAttribute(String name, Object value) {
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			Pipeline pipeline = jedis.pipelined();
			byte[] data = SerializeUtils.encode(value);
			pipeline.hset(redisKey.getBytes(), (RedisConstant.SESSION_ATTR_PREFIX + name).getBytes(), data);
			pipeline.sync();
			changeLastAccessTime(getMaxInactiveInterval());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	public void removeAttribute(String name) {
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			Pipeline pipeline = jedis.pipelined();
			pipeline.hdel(redisKey, RedisHelper.getRedisAttrHashKey(name));
			pipeline.sync();
			changeLastAccessTime(getMaxInactiveInterval());
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public void removeValue(String name) {
		removeAttribute(name);
	}

	public void invalidate() {
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			Pipeline pipeline = jedis.pipelined();
			pipeline.del(redisKey);
			pipeline.sync();
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	public boolean isNew() {
		return this.isNew;
	}

	private void changeLastAccessTime(int interval) {
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool == null) {
			throw new RuntimeException("Can not get jedis pool");
		}
		Jedis jedis = jedisPool.getResource();
		try {
			String redisKey = RedisHelper.getRedisIDKey(getId());
			Pipeline pipeline = jedis.pipelined();
			long now = System.currentTimeMillis();
			pipeline.hset(redisKey, RedisConstant.LAST_ACCESS_TIME, now + "");
			pipeline.expire(redisKey, interval);
			pipeline.sync();
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}
}
