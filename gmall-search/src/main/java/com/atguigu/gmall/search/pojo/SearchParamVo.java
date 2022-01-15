package com.atguigu.gmall.search.pojo;


import lombok.Data;

import java.util.List;
@Data
public class SearchParamVo {
    //关键字
    private String keyword;
    //品牌
    private List<Long> brandId;
    //分类
    private List<Long> categoryId;
    //规格参数过滤
    private List<String> props;
    //价格区间过滤
    private Double priceFrom;
    private Double priceTo;
    //显示有货
    private Boolean store;
    //排序  0-默认排序 1-价格降序 2-价格升序 3-销量的降序 4-新品降序
    private Integer sort=0;
    //页码
    //每页显示条数
    private Integer pageNum=1;
    private Integer pageSize=10;
}
