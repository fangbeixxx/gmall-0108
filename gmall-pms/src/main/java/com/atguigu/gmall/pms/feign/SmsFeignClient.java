package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("sms-service")
public interface SmsFeignClient extends GmallSmsApi {
//    @PostMapping("sms/skubounds/sales/save")
//    @ApiOperation("保存")
//    public ResponseVo<Object> saleSales(@RequestBody SkuSaleVo skuBounds);
}
