package com.github.emacsist.session;

public class RedisHelper {
	public static String getRedisIDKey(String id) {
		return RedisConstant.SESSION_PREFIX + id;
	}
	
	public static String getRedisAttrHashKey(String name) {
		return RedisConstant.SESSION_ATTR_PREFIX + name;
	}
}
