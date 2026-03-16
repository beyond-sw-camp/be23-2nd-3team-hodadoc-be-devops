package com.beyond.hodadoc.common.service;//package com.beyond.basic.b2_board.common.service;
//
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.*;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
////Aspect : aop 코드임을 명
//@Aspect
//@Component
//@Slf4j
//public class AopLogService {
//
////    AOP의 대상이 되는 controller, service 등을 어노테이션 기준으로 명시
//    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
//    public void controllerPointCut() {
//
//    }
//
//    //    AOP의 대상이 되는 controller, service 등을 패키지 구조 기준으로 명시
////    @Pointcut("within(com.beyond.basic.b2_board.author.controller.AuthorController)")
////    public void controllerPointCut() {}
////}
//
//////    aop활용방법 1 : arount를  통해 before, joinpoint, after 코드 한꺼번에 작성
////    @Around("controllerPointCut()")
//////    joinpoint는 사용자가 실행하고자 하는 콛를 의미하고, 위에서 정이한 pointcut을 의미
////    public Object controllerLogger(ProceedingJoinPoint joinPoint) {
////
//////        joinpoint 이전
////        log.info("aop start");
////        log.info("요청자 : " + SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
////        log.info("요청 메서드명 : " + joinPoint.getSignature().getName());
////
//////        servlet 객체에서 http 요청을 꺼내는 법
////        ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
////        HttpServletRequest request = attributes.getRequest();
////        log.info("http url : " + request.getRequestURI());
////        log.info("http 메서드 : " + request.getMethod());
////        log.info("http 헤더 - 토큰 : " + request.getHeader("Authorization"));
////        log.info("http 헤더 - contentType : " + request.getHeader("Content-Type"));
////
//////        joinpoint 실행
////        Object object = null;
////        try {
////            object = joinPoint.proceed();
////        } catch (Throwable e) {
////            throw new RuntimeException(e);
////        }
////
//////        joinpoint 이후
////        log.info("aop end");
////
////        return object;
////    }
//
//    //    aop활용방법 2 : Before, After 어노테이션 사용
//    @Before("controllerPointCut()")
//    public void beforeController(JoinPoint joinPoint) {
//        log.info("aop start");
//        log.info("요청자 : " + SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
//        log.info("요청 메서드명 : " + joinPoint.getSignature().getName());
//
//
//    }
//
//    @After("controllerPointCut()")
//    public void afterController() {
//        log.info("aop end");
//    }
//}
