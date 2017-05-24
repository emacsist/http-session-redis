# http-session-redis
http session redis

这是一个非常简小的使用 redis 来做中心 session 存储机制。这样子，在使用 Nginx + Tomcat 做负载均衡时，可以做到 session 的一致性。(当然也可以使用 hash 算法来固定到 tomcat)

该项目只是当作学习，而不能用到生产，毕竟这只是通过亲手接触这些 Servlet 容器处理 session 的逻辑来学习它的目的的。