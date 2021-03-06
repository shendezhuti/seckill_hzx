package org.seckill.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml",
        "classpath:spring/spring-service.xml"})
public class SeckillServiceImplTest {
    private final Logger logger= LoggerFactory.getLogger(this.getClass());

    @Autowired
    SeckillService seckillService;
    @Test
    public void getSeckillList() {
        List<Seckill> list= seckillService.getSeckillList();
        logger.info("list={}",list);

    }

    @Test
    public void getById() {
        long id=1000;
        Seckill seckill = seckillService.getById(id);
        logger.info("seckilll={}",seckill);
    }


    /**
     * 集成测试的完整性
     */
    @Test
    public void exportSeckillLogic() {
        long id = 1002;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        if (exposer.isExposed()) {
            long phone=18684767685L;
            String md5="e7723ed46a43abc2a11ed7400f83542a";
            try{
                SeckillExecution execution= seckillService.executeSeckill(id,phone,md5);
                logger.info("result={}",execution);
            }catch (RepeatKillException e1){
                logger.error(e1.getMessage());
            }catch (SeckillCloseException e2){
                logger.error(e2.getMessage());
            }catch(Exception e){
                logger.error(e.getMessage());
            }
        } else {
            logger.warn("exposer={}", exposer);
        }
    }


}