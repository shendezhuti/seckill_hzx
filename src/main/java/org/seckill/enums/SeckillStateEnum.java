package org.seckill.enums;

/**
 * 数据字典
 *
 */
public enum  SeckillStateEnum {
        Success(1,"秒杀成功") ,
     END (0,"秒杀结束"),
        REPEAT_KILL(-1,"重复秒杀"),
     INNER_ERROR(-2,"系统异常"),
     DATA_REWRITE(-3,"数据篡改");

    private int state;
    private String stateInfo;
    SeckillStateEnum( int state,String stateInfo) {
        this.stateInfo = stateInfo;
        this.state = state;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public int getState() {
        return state;
    }

    public static SeckillStateEnum stateof(int index){
        for(SeckillStateEnum stateEnum:values()){
            if(stateEnum.getState() == index){
                return stateEnum;
            }
        }
        return null;
    }
}
