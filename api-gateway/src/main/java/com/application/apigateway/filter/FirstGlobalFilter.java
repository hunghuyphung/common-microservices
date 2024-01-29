package com.application.apigateway.filter;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;



@Log4j2
@Order(0)
@Component
public class FirstGlobalFilter implements GlobalFilter {
    private static final String START_TIME = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ThreadContext.put("reqId", "REQ_ID");

        log.info("First Global Filter");
        var req = exchange.getRequest();
        req = req.mutate().header(START_TIME, String.valueOf(System.currentTimeMillis())).build();
        return chain.filter(exchange.mutate().request(req).build()).then(Mono.fromRunnable(() -> {
            var request = exchange.getRequest();
            var response = exchange.getResponse();
            long processTime = calcProcessTime(request);
            String message = String.format("channel=NA,uri=%s,code=%s,server=%s,requestId=%s,ip=%s,processTime=%s",
                    request.getPath(), response.getStatusCode(), request.getLocalAddress(), request.getId(), request.getRemoteAddress(), processTime);
            log.info(message);
        }));
    }

    public long calcProcessTime(ServerHttpRequest request) {
        long processTime = 0;
        var headers = request.getHeaders();
        var optional = headers.getOrDefault(START_TIME, new ArrayList<>()).stream().findFirst();
        if (optional.isPresent()) {
            long startTime = Long.parseLong(optional.get());
            processTime = System.currentTimeMillis() - startTime;
        }
        return processTime;
    }
}
