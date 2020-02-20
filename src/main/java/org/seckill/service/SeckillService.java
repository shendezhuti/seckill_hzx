package org.seckill.service;

import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;

import java.util.List;

public interface SeckillService {

    /**
     *
     * @return
     */
    List<Seckill> getSeckillList();

    /**
     *
     * @param seckillId
     * @return
     */
    Seckill getById(long seckillId);


    /**
     *秒杀开启时输出秒杀接口地址，否则输出系统时间和秒杀时间
      * @param seckillId
     * @return
     */
    Exposer exportSeckillUrl(long seckillId);

    /**
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     */
    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException;





}
