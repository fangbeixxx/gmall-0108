package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    JwtProperties jwtProperties;

    public AuthGatewayFilterFactory(){
        super(PathConfig.class);
    }

    //    规定参数顺序
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    //接收集合参数
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }


    @Override
    public GatewayFilter apply(PathConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//                System.out.println("我是局部过滤器！！！,key="+config.key+"value="+config.value);
                //System.out.println("我是局部过滤器！！！,list="+config.list);
                //获取当前请求路径
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

                // 获取当前请求的路径
                String curPath = request.getURI().getPath();  //不包含域名和端口号
                // 获取拦截路径名单
                List<String> paths = config.paths;
//             1、 判断当前请求 路径在不在拦截名单中， 不在则放行
                if(paths.stream().allMatch(path-> !curPath.startsWith(path))){
                    return chain.filter(exchange);
                };


                // 2.获取token信息，异步：token头中  同步：cookie中
                String token = request.getHeaders().getFirst(jwtProperties.getToken());
                if (StringUtils.isBlank(token)){
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())){
                        token = cookies.getFirst(jwtProperties.getCookieName()).getValue();
                    }
                }

                // 3.判断token是否为空，为空则拦截并重定向到登录页面 login
                if (StringUtils.isBlank(token)){
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    // 被拦截
                    return response.setComplete();
                }
//             4、解析jwt类型的token，如果解析过程中出现异常，则拦截并重定向到登录页面
                try {
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
 //             5、获取载荷中的ip和当前请求的ip地址判断是否一致，如果不一致说明是盗用，则重定向到登录页面
                    String ip = map.get("ip").toString();
//                    当前请求ip地址
                    String curIp = IpUtil.getIpAddressAtGateway(request);
//                   如果不等于拦截
                    if(!StringUtils.equals(ip,curIp)){
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl="+request.getURI());
//                    被拦截
                        return  response.setComplete();
                    }

                    // 6.把解析后的用户信息传递给后续服务
                    request.mutate()
                            .header("userId", map.get("userId").toString())
                            .header("username", map.get("username").toString())
                            .build();
                    exchange.mutate().request(request).build();
//                   7、放行
                    return chain.filter(exchange);
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl="+request.getURI());
//                    被拦截
                    return  response.setComplete();
                }

            }
        };
    }

    @Data
    public static class PathConfig{
//        public String key;
//        public String value;
      private List<String> paths;
    }
}
