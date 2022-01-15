package com.atguigu.gmall.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;
import org.springframework.util.CollectionUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {
    @Autowired
    CategoryMapper categoryMapper;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategoriesByPid(Long parentId) {
        LambdaQueryWrapper<CategoryEntity> queryWrapper = new LambdaQueryWrapper<>();
        if(parentId!=-1){
            queryWrapper.eq(CategoryEntity::getParentId,parentId);
        }
        return this.list(queryWrapper);
    }

    @Override
    public List<CategoryEntity> queryLvl2WithSubsById(Long pid) {
        List<CategoryEntity> categoryEntities=categoryMapper.queryLvl2WithSubsById(pid);
        return categoryEntities;
    }

    @Override
    public List<CategoryEntity> querylv123WithSubsById(Long cid) {
//        获取三级分类
        CategoryEntity categoryEntity3 = this.categoryMapper.selectById(cid);

        if(categoryEntity3==null){
            return null;
        }
        //获取二级分类
        CategoryEntity categoryEntity2= this.getById(categoryEntity3.getParentId());
//        获取一级分类
        CategoryEntity categoryEntity1= this.getById(categoryEntity2.getParentId());

        return Arrays.asList(categoryEntity3,categoryEntity2,categoryEntity1);

    }

}