package com.atguigu.gmall.pms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@AllArgsConstructor
class User{
    private String name;
    private Integer age;
    private Boolean sex;
}
@Data
@ToString
class Person{
    private String name;
    private Integer age;
}
public class StreamDemo {
    public static void main(String[] args) {
        //创建对象集合
        List<User> users = Arrays.asList(
                new User("liuyan", 20, false),
                new User("marong", 21, false),
                new User("xiaolu", 22, false),
                new User("laowang", 23, true),
                new User("xiaoliang", 24, true),
                new User("zhengshuang", 25, false),
                new User("pig", 26, true)
        );
//        集合转换
        Stream<User> stream = users.stream();
//        stream.map(user -> user.getName()).collect(Collectors.toList()).forEach(System.out::println);
//        stream.map(user -> {
//            Person person = new Person();
//            person.setAge(user.getAge());
//            person.setName(user.getName());
//            return person;
//        }).collect(Collectors.toList()).forEach(System.out::println);
//       stream.filter(User::getSex).collect(Collectors.toList()).forEach(System.out::println);
//        stream.filter(user -> user.getAge()>20).collect(Collectors.toList()).forEach(System.out::println);

//        删选并交换
//        stream.filter(user -> user.getAge()>22).map(user -> {
//            Person person = new Person();
//            person.setName(user.getName());
//            person.setAge(user.getAge());
//            return person;
//        }).collect(Collectors.toList()).forEach(System.out::println);

        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream1 = list.stream();
//        System.out.println(stream1.reduce((a, b) -> a + b).get());
        Integer integer = stream1.reduce((a, b) -> a + b).get();
        System.out.println(integer);
//        System.out.println(arrs.stream().reduce((a, b) -> a + b).get());

    }
}
