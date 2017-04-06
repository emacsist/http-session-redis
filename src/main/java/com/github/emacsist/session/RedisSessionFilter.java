package com.github.emacsist.session;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import redis.clients.jedis.JedisPool;

public class RedisSessionFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		RedisSessionRequestWrapper redisSessionRequestWrapper = new RedisSessionRequestWrapper((HttpServletRequest) request);
		chain.doFilter(redisSessionRequestWrapper, response);
	}

	@Override
	public void destroy() {
		JedisPool jedisPool = RedisUtils.getJedisPool();
		if (jedisPool != null && !jedisPool.isClosed()) {
			jedisPool.close();
		}
	}

}
