package com.jfecm.openmanagement.security.filters;

import com.jfecm.openmanagement.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private HandlerExceptionResolver exceptionResolver;

    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    /**
     * Will execute for every http call
     *
     * @param request     request
     * @param response    response
     * @param filterChain filterChain
     * @throws ServletException ServletException
     * @throws IOException      IOException
     */
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        log.info("Check Jwt token.");

        try {
            final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            log.info("Check Jwt token. AUTHORIZATION && Bearer OK");

            final String jwt = authHeader.substring(7);

            final String subject = jwtService.getSubject(jwt);

            log.info("Check Jwt token. JWT OK " + jwt);

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                log.info("Check Jwt token. SUBJECT OK " + subject + " The user is not authenticated yet. OK");

                UserDetails userDetails = userDetailsService.loadUserByUsername(subject);

                log.info("Check Jwt token. UserDetails data OK " + userDetails);

                if (jwtService.isTokenValid(jwt, userDetails)) {

                    log.info("Check Jwt token. Token valid OK - Updating the SecurityContextHolder");

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

                log.info("SecurityContextHolder status. " + SecurityContextHolder.getContext().getAuthentication());
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            exceptionResolver.resolveException(request, response, null, e);
        }
    }
}
