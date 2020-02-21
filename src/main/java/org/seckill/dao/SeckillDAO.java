package org.seckill.dao;

import org.apache.ibatis.annotations.Param;
import org.seckill.entity.Seckill;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface SeckillDAO {

    /**
     *
     * @param seckillId
     * @param killTime
     * @return
     */
    int reduceNumber(@Param("seckillId") long seckillId,@Param("killTime") Date killTime);


    /**
     * 
     * @param seckillId
     * @return
     */
    Seckill queryById(long seckillId);


    /**
     *根据偏移量查询
     * @param offset
     * @param limit
     * @return
     */
    List<Seckill> queryAll(@Param("offset") int offset,@Param("limit") int limit);

    /**
     *
     * @param paraMap
     */
    void killByProcedure(Map<String ,Object> paraMap);
}
