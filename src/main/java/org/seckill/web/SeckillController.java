package org.seckill.web;


import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.dto.SeckillResult;
import org.seckill.entity.Seckill;
import org.seckill.enums.SeckillStateEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SeckillService seckillService;
    @RequestMapping(value = "/list",method = RequestMethod.GET)
    public String list(Model model){
        //list.jsp+model = ModelAndView
        List<Seckill> list= seckillService.getSeckillList();

        model.addAttribute("list",list);
        return "list";
    }

    @RequestMapping(value = "/{seckillId}/detail",method = RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Long seckillId, Model model){
        if(seckillId==null){
            return  "redirect:/seckill/list";
        }

        Seckill seckill= seckillService.getById(seckillId);
        if(seckill==null){
            return "forward:/seckill/list";
        }

        model.addAttribute("seckill",seckill);
        return "detail";
    }

    //ajax json接口
    @RequestMapping(value="/{seckillId}/exposer",method = RequestMethod.GET,
    produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer>  exposer(@PathVariable("seckillId") Long seckillId){
        SeckillResult<Exposer> result;
        try {
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            result = new SeckillResult<Exposer>(true, exposer);
        }catch (Exception e){
            logger.error(e.getMessage(),e);
            result=new SeckillResult<Exposer>(false,e.getMessage());
        }
        return result;
    }

    @RequestMapping(value="/{seckillId}/{md5}/execution",
        method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long seckillId,@PathVariable("md5") String md5,

                                                   @CookieValue(value="userPhone",required = false) Long userPhone){
        if(userPhone==null){
            return new SeckillResult<SeckillExecution>(false,"未注册");
        }
        SeckillResult<SeckillExecution> result;
        try {
            SeckillExecution seckillExecution = seckillService.executeSeckill(seckillId, userPhone, md5);
            return new SeckillResult<SeckillExecution>(true,seckillExecution);
        }catch (RepeatKillException e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExecution>(false,execution);
        }catch (SeckillCloseException e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.END);
            return new SeckillResult<SeckillExecution>(false,execution);
        }
        catch (Exception e){
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            return new SeckillResult<SeckillExecution>(false,execution);
        }
    }


    @RequestMapping(value="/time/now",method = RequestMethod.GET)
    @ResponseBody
    public SeckillResult <Long> time(){

        Date now = new Date();
        return new SeckillResult<Long>(true,now.getTime());
    }


}
