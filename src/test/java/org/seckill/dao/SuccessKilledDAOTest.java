package org.seckill.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.entity.SuccessKilled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring/spring-dao.xml")
public class SuccessKilledDAOTest {

    @Autowired
    private SuccessKilledDAO successKilledDAO;
    @Test
    public void insertSuccessKilled() {
            long id=1000;
            long userPhone=18684767682L;
           int insertcount= successKilledDAO.insertSuccessKilled(id,userPhone);
           System.out.println(insertcount);
    }

    @Test
    public void queryByIdWithSeckill() {
        long id=1000;
        long userPhone=18684767682L;
        SuccessKilled successKilled=successKilledDAO.queryByIdWithSeckill(id,userPhone);
        System.out.println(successKilled);
        System.out.println(successKilled.getSeckill());
    }
}