package com.github.emacsist.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

public class RedisSessionRequestWrapper extends HttpServletRequestWrapper {
	private HttpServletRequest request;

	public RedisSessionRequestWrapper(HttpServletRequest request) {
		super(request);
		this.request = request;
	}

	@Override
	public HttpSession getSession(boolean create) {
		HttpSession originalSession = request.getSession(create);
		HttpSession session = new RedisSession(originalSession.getId(), create, request.getServletContext());
		return session;
	}

	@Override
	public HttpSession getSession() {
		HttpSession originalSession = request.getSession();
		HttpSession session = new RedisSession(originalSession.getId(), request.getServletContext());
		return session;
	}

}
