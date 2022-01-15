package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {
    @Autowired
    IndexService indexService;

    @GetMapping
    public String toIndex(Model model){
       List<CategoryEntity> categoryEntityList=indexService.queryLvllCategories();
        model.addAttribute("categories",categoryEntityList);
        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLvl2WithSubsByPid(@PathVariable("pid") Long pid){
        List<CategoryEntity> categoryEntities=indexService.queryLvl2WithSubsByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("index/test/lock")
    @ResponseBody
    public ResponseVo testLock(){
        indexService.testLock();
        return  ResponseVo.ok();
    }

//    读,写锁
    @GetMapping("index/test/read")
    @ResponseBody
    public ResponseVo testRead(){
        indexService.testRead();
        return  ResponseVo.ok();
    }
    @GetMapping("index/test/write")
    @ResponseBody
    public ResponseVo testWrite(){
        indexService.testWrite();
        return ResponseVo.ok();
    }

    @GetMapping("index/test/latch")
    @ResponseBody
    public ResponseVo testLatch(){
        indexService.testLatch();
        return ResponseVo.ok("班长出来了");
    }
    @GetMapping("index/test/countDown")
    @ResponseBody
    public ResponseVo testCountDown(){
        indexService.testCountDown();
        return ResponseVo.ok("出来一位同学！！");
    }
}
