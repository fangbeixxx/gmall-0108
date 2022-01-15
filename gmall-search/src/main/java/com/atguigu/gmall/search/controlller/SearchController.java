package com.atguigu.gmall.search.controlller;


import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;

import com.atguigu.gmall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Controller
//@RestController
@RequestMapping("search")
public class SearchController {
    
//    @Resource
    @Autowired
    SearchService searchService;
    
    @GetMapping
    public String search(SearchParamVo searchParam, Model model){
        SearchResponseVo responseVo=searchService.search(searchParam);

        model.addAttribute("response",responseVo);
        model.addAttribute("searchParam",searchParam);
        return "search";
    }
//@GetMapping
//public ResponseVo<SearchResponseVo> search(SearchParamVo paramVo){
//    SearchResponseVo responseVo = this.searchService.search(paramVo);
//
//    return ResponseVo.ok(responseVo);
//}
}
