package com.atguigu.gmall.index.service;

import com.atguigu.gmall.pms.entity.CategoryEntity;

import java.util.List;

public interface IndexService {
    List<CategoryEntity> queryLvllCategories();

    List<CategoryEntity> queryLvl2WithSubsByPid(Long pid);

    void testLock();

    void testRead();

    void testWrite();

    void testLatch();

    void testCountDown();
}
