package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
public class SearchAttrValueVo {
    @Field(type = FieldType.Long)
    private Long attrId;  //参数id
    @Field(type = FieldType.Keyword)
    private String attrName;  //参数名
    @Field(type = FieldType.Keyword)
    private String attrValue;   //参数值
}
