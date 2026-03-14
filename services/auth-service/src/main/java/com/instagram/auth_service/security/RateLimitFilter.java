package com.instagram.auth_service.security;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.signin.max-attempts}")
    private int signinMaxAttempts;

    @Value("${rate-limit.signin.window-minutes}")
    private int signinWindowMinutes;

    @Value("${rate-limit.signup.max-attempts}")
    private int signupMaxAttempts;

    @Value("${rate-limit.signup.window-minutes}")
    private int signupWindowMinutes;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).maximumSize(1000).build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    )throws ServletException, IOException{

        if(!"POST".equalsIgnoreCase(request.getMethod()))
        {
            chain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        String ip = getClientIp(request);
        if (ip == null || ip.isBlank()) 
        {
            ip = "unknown";
        }
        
       /* locahost i docker mreza
         if("127.0.0.1".equals(ip) || ip.startsWith("172.")) 
        {
            chain.doFilter(request, response);
            return;
        }
        */

        if("/api/v1/auth/signin".equals(uri)) 
        {
            if(!consume(response, "signin:" + ip, signinMaxAttempts, signinWindowMinutes))
                return;
        }
        else if("/api/v1/auth/signup".equals(uri))
        {
            if(!consume(response, "signup:" + ip, signupMaxAttempts, signupWindowMinutes))
                return;
        }

        chain.doFilter(request, response);
    }

    private boolean consume(
            HttpServletResponse response,
            String key,
            int attempts,
            int minutes
    )throws IOException{

        Bucket bucket = buckets.get(key, k -> createBucket(attempts, minutes));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if(probe.isConsumed()) 
        {
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(attempts));
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long retry = Math.max(1, probe.getNanosToWaitForRefill() / 1000000000);

        log.warn("Rate limit exceeded: {}", key);

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retry));
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write("{\"error\":\"Превише покушаја. Покушајте поново за " + retry + " секунди.\"}");

        return false;
    }

    private Bucket createBucket(int attempts, int minutes) 
    {
        return Bucket.builder().addLimit(Bandwidth.classic(attempts,Refill.greedy(attempts, Duration.ofMinutes(minutes)))).build();
    }

    private String getClientIp(HttpServletRequest request) 
    {
        String forwarded = request.getHeader("X-Forwarded-For");

        if(forwarded != null && !forwarded.isBlank()) 
        {
            return forwarded.split(",")[0].trim();
        }

        String remote = request.getRemoteAddr();

        if(remote != null && !remote.isBlank())
        {
            return remote;
        }
        else
        {
            return "unknown";
        }
    }
}