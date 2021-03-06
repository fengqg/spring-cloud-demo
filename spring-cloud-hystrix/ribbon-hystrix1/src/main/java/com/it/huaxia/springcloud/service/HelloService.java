package com.it.huaxia.springcloud.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.ObservableExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import rx.Observable;
import rx.Subscriber;

/**
 * 请求熔断
 * @author fengqigui
 * @Description
 * @Date 2018/01/25 19:52
 */
@Service
public class HelloService {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 重点
     * fallbackMethod:降级(一级降级)
     *      helloFallback:就是方法名，参数一样，返回值一样
     *
     * @return
     */
    @HystrixCommand(fallbackMethod = "helloFallback")
    public String helloService(){

        /**
         * http://EUREKA-SERVER/hello:
         *  EUREKA-SERVER:生产者的名字
         *  hello：消费之的RequestMapping
         * String.class:对应方法返回值得类型
         */
        return  restTemplate.getForEntity("http://EUREKA-CONSUMER/hello", String.class).getBody();
    }


    /**
     * observableExecutionMode:设置冷热执行的注解
     *      LAZY：冷执行
     *      EAGER: 热执行
     * ignoreExceptions :忽略的异常
     * groupKey ：线程池的分组
     * threadPoolKey ：指定线程池
     *
     * @return
     */
    @HystrixCommand(fallbackMethod = "helloFallback", observableExecutionMode = ObservableExecutionMode.EAGER, ignoreExceptions = RestClientException.class)
    public Observable<String> helloService1(){

        return Observable.create(new Observable.OnSubscribe<String>() {


            @Override
            public void call(Subscriber<? super String> subscriber) {
                System.out.println(Thread.currentThread().getName());

                // 只要是订阅了这个就执行下面的逻辑
                try {
                    if(!subscriber.isUnsubscribed()){
                        System.out.println("方法执行----");
                        String result = restTemplate.getForEntity("http://EUREKA-CONSUMER/hello", String.class).getBody();
                        // 向下传递结果
                        subscriber.onNext(result);
                        String result1 = restTemplate.getForEntity("http://EUREKA-CONSUMER/hello", String.class).getBody();
                        // 向下传递结果
                        subscriber.onNext(result1);
                        subscriber.onCompleted();// 这个之后不会执行
                    }
                } catch (RestClientException e) {
                    // 如果有错误则返回，调用者
                    subscriber.onError(e);
                }


            }
        });
    }

    @HystrixCommand(fallbackMethod = "helloFallBack2")
    public String helloFallback(){
        return "error";
    }


    /**
     * 在最后一个降级的服务中，最好不要有网络请求服务。否则没完没了
     * @param  throwable 这里就是上面服务中出现的异常，
     * @return
     */
    public String helloFallBack2(Throwable throwable){

        return "二级降级服务";
    }

}
