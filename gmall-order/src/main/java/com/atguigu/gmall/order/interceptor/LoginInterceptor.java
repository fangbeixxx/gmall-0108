package com.atguigu.gmall.order.interceptor;




import com.atguigu.gmall.order.pojo.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final  static  ThreadLocal<UserInfo>  THREAD_LOCAL=new ThreadLocal<UserInfo>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //获userkey
        String userId = request.getHeader("userId");
        String username = request.getHeader("username");
        THREAD_LOCAL.set(new UserInfo(Long.valueOf(userId),null,username));
        return true;
    }
//    提供一个对外调用的方法
    public static UserInfo userInfo(){
        return  THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        THREAD_LOCAL.remove();
    }
}
