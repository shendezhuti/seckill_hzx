## SSM+Maven+IDEA实现高并发秒杀商品系统

### 时间：2020.02.06

## 功能

- 秒杀接口暴露
- 执行秒杀
- 相关查询

> [原项目github链接](https://github.com/codingXiaxw/seckill)

本项目的完成基于原项目，是自主学习的记录。有些原项目中开发知识点由于版本升级的原因（如mysql）已经不再适用，本说明文档会记录自己踩过的坑。

### 1.使用maven在terminal中创建项目

```
mvn archetype:generate -DgroupId=org -DartifactId=seckill -DarchetypeArtifactId=maven-archetype-webapp
```

注意原教程创建mvn在命令中用的create，是过时的写法，会报错，要改为generate

### 2.数据库编码部分

> 作者zhang提到，最好能拥有手写sql语言的能力，这样对项目上线后的修改非常有帮助

在mysql5.17后，如果直接使用作者的sql代码，会产生如下错误

```sql
Invalid default value....
```

[原因和解决方法在此](https://stackoverflow.com/questions/36374335/error-in-mysql-when-setting-default-value-for-date-or-datetime)

```sql
--创建数据库脚本
CREATE  DATABASE seckill;
--解决mysql版本升级带来的bug
SET sql_mode = '';
--使用数据库
use seckill;
--创建秒杀库存表
CREATE TABLE seckill(
  `seckill_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品库存id',
  `name` varchar (120) NOT NULL COMMENT '商品名称',
  `number` int NOT NULL COMMENT '库存数量',
  `start_time` timestamp NOT NULL COMMENT '秒杀开启时间',
  `end_time` timestamp NOT NULL COMMENT '秒杀结束时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY(seckill_id),
  key idx_start_time(start_time),
  key idx_end_time(end_time),
  key idx_create_time(create_time)
)ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT='秒杀库存表';

--初始化数据
insert into
  seckill(name,number,start_time,end_time)
  values
    ('1元秒杀坚果tNT工作站',100,'2018-06-01 00:00:00','2018-06-02 00:00:00'),
    ('1元秒杀iphonex',100,'2018-06-01 00:00:00','2018-06-02 00:00:00'),
    ('1元秒杀坚果3',100,'2018-06-01 00:00:00','2018-06-02 00:00:00'),
    ('1元秒杀mac',100,'2018-06-01 00:00:00','2018-06-02 00:00:00');

--秒杀成功明细表
--用户登录认证的相关的信息
CREATE TABLE success_killed(
  `seckill_id` bigint NOT NULL COMMENT '秒杀商品id',
  `user_phone` bigint NOT NULL COMMENT '用户手机号',
  `state` bigint NOT NULL DEFAULT -1 COMMENT '状态表示：-1：无效，0：成功，1：已付款',
  `create_time` timestamp NOT NULL COMMENT '创建时间',
  PRIMARY KEY (seckill_id,user_phone),
  key idx_create_time(create_time)
)ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀成功明细表';

```

## 3.DAO层

首先给出实体Entity类，用于建立与数据库表的关系。Entity类中的属性对应表中的列。记得要类的属性的定义使用驼峰命名法，getter和setter与toString可以使用idea自动生成。

**Seckill类**

```java
package org.seckill.entity;

import java.util.Date;

public class Seckill {

    private long seckillId;

    private String name;

    private int number;

    private Date startTime;

    private Date endTime;

    private Date createTime;

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "Seckill{" +
                "seckillId=" + seckillId +
                ", name='" + name + '\'' +
                ", number=" + number +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", createTime=" + createTime +
                '}';
    }
}

```

**SuccessKilled类**

注意在SuccessKilled里面有因为业务需求的 Seckill对象，因为一个Seckill秒杀产品可能有多个SuccessKilled对象

```java
package org.seckill.entity;

import java.util.Date;

public class SuccessKilled {
    private long seckillId;

    private long userPhone;

    private short state;

    private Date createTime;

    //变通，多对一
    private Seckill seckill;

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }


    public long getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(long userPhone) {
        this.userPhone = userPhone;
    }

    public short getState() {
        return state;
    }

    public void setState(short state) {
        this.state = state;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Seckill getSeckill() {
        return seckill;
    }

    public void setSeckill(Seckill seckill) {
        this.seckill = seckill;
    }
    @Override
    public String toString() {
        return "SuccessKilled{" +
                "seckillId=" + seckillId +
                ", userPhone=" + userPhone +
                ", state=" + state +
                ", createTime=" + createTime +
                '}';
    }
}

```

下面是SeckillDAO和SuccessKilledDAO

```java
package org.seckill.dao;

import org.seckill.entity.Seckill;

import java.util.Date;
import java.util.List;

public interface SeckillDAO {

    /**
     * 减库存
     * @param sekillId
     * @param killTime
     * @return 如果影响行数>1,表示更新的记录行数
     */
    int reduceNumber(long sekillId, Date killTime);


    /**
     *根据商品查询秒杀对象
     * @param seckillId
     * @return
     */
    Seckill queryById(long seckillId);

    /**
     * 根据偏移量查询秒杀商品列表
     * @param offet
     * @param limit 在偏移量offset后取多少行
     * @return
     */
    List<Seckill> queryAll(int offet, int limit);
}

```

```java
package org.seckill.dao;

import org.seckill.entity.SuccessKilled;

public interface SuccessKilledDAO {

    /**
     *插入购买明细，可过滤重复
     * @param seckillId
     * @param userPhone
     * @return 插入的行数
     */
    int insertSuccessKilled(long seckillId,long userPhone);

    /**
     * 根据Id查询SuccessKilled并携带秒杀产品对象实体
     * @param seckillId
     * @return
     */
    SuccessKilled queryByIdWithSeckill(long seckillId);
}

```

### 基于mybatis实现DAO

mybatis与hibernate的作用是建立数据库与Entity实体对象的映射

**mybatis特点**

- 参数+SQL=Entity/List

我们只要提供参数以及编写sql语言。由于mybatis封装了jdbc，会把结果返回给我们。

**如何使用mybatis**

- SQL写在哪？XML提供SQL vs 注解提供SQL
- 如何实现DAO接口？Mapper自动实现DAO接口 vs API编程实现DAO接口

一般来说建议使用xml提供sql，使用Mapper自动实现DAO接口，减少代码量。

### 配置mybatis

在resources下创建mybatis-config.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>
    <!-- 配置全局属性-->
    <settings>
        <!-- 使用jdbc的getGeratedKeys 获取数据库自增主键-->
        <setting name="useGeneratedKeys" value="true"/>

        <!-- 使用列名替换列名 默认true-->
        <setting name="useColumnLabel" value="true"/>

        <!-- 开启驼峰命名转换：Table(create_time)->Entity(createTime)-->
        <setting name="mapUnderscoreCamelCase" value="true"></setting>
    </settings>


</configuration>
```

在resources/mapper下创建SeckillDAO.xml与SuccessKilledDAO.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="org.seckill.dao.SeckillDAO">
    <!--目的：为DAO接口方法提供sql语句设置-->

    <update id="reduceNumber" >
        <!-- 具体的sql语句-->
        update
          seckill
        set
        number = number -1
        where seckill_id = #{seckillId}
        and start_time <![CDATA[<=]]> #{killTime},
          and end_time>=#{killTime}
          and number >0;
    </update>

    <select id="queryById" resultType="Seckill" parameterType="long">

      select seckill_id,name,number,start_time,end_time,create_time
      from seckill
      where seckill_id=#{seckillId}
    </select >

    <select id="queryAll" resultType="Seckill" >

      select seckill_id,name,number,start_time,end_time,create_time
      from seckill
      order by create_time desc
      limit #{offset},#{limit}

    </select >

</mapper>
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="org.seckill.dao.SuccessKilledDAO">

    <insert id="insertSuccessKilled">
        <!--当出现主键冲突时(即重复秒杀时)，会报错;不想让程序报错，加入ignore-->
        INSERT ignore INTO success_killed(seckill_id,user_phone,state)
        VALUES (#{seckillId},#{userPhone},0)
    </insert>

    <select id="queryByIdWithSeckill" resultType="SuccessKilled">
    <!-- 根据id 查询SuccessKilled并携带Seckill实体-->
    <!-- 如何告诉Mybatis把结果映射到SuccessKilled同时映射seckill属性-->
    <!-- 可以自由控制sql-->
      select
        sk.seckill_id,
        sk.user_phone,
        sk.create_time,
        sk.state,
        s.seckill_id "seckill.seckill_id",
        s.name "seckill.name",
        s.number "seckill.number",
        s.start_time  "seckill.start_time",
        s.end_time  "seckill.end_time",
        s.create_time "seckill.create_time"
      from success_killed sk
        inner join seckill s on sk.seckill_id=s.seckill_id
        where sk.seckill_id=#{seckillId}

    </select>
</mapper>
```

### mybatis整合spring理论

**整合目标**

- 更少的编码--只写接口，不写实现

接口已经能说明很多事儿，比如 Seckill queryById (long id) ;  Seckill指代结果集，queryById指代行为，long id指代参数。有了参数，通过行为就可以返回结果集

- 更少的配置-别名

org.seckill.entity.Seckill->Seckill         （基于package scan实现）

​		更少的配置-配置扫描

<mapper resource="mapper/SeckillDAO.xml"    -> 自动扫描配置文件

​		更少的配置-dao实现

<bean id="ClubDdao"class="...ClubDao"/>   ->  自动实现DAO接口，自动注入spring容器

- 足够的灵活性

自己定制SQL+自由传参 -> 结果集自动赋值

### mybatis整合spring编码

在resources/spring下创建srping-dao.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context" xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <!-- 配置整合mybatis过程 -->
    <!-- 1：配置数据库相关参数 properties的属性:${url}-->
    <context:property-placeholder location="classpath:jdbc.properties"/>

    <!-- 2：数据库连接池-->
    <bean id ="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
        <!-- 配置连接池属性-->
            <property name="driverClass" value="${driver}"/>
            <property name="jdbcUrl" value="${url}"/>
            <property name="user" value="${username}"/>
            <property name="password" value="${password}"/>

        <!-- c3p0连接池的私有属性 -->
            <property name="maxPoolSize" value="30"/>
            <property name="minPoolSize" value="10"/>
        <!--关闭连接不自动commit -->
            <property name="autoCommitOnClose" value="false"/>
        <!-- 获取连接超时时间-->
            <property name="checkoutTimeout" value="1000"/>

        <!-- 当获取连接失败重试次数-->
            <property name="acquireRetryAttempts" value="2"/>

    </bean>

    <!-- 约定大于配置-->
    <!-- 3：配置sqlSessionFactory对象-->
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <!-- 注入数据库连接池-->
        <property name="dataSource" ref="dataSource"/>
        <!-- 配置Mybatis全局配置文件:mybatis-config.xml-->
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <!-- 扫描entity包 使用别名 org.seckill.entity.Seckill->seckill-->
        <property name="typeAliasesPackage" value="org.seckill.entity"/>
        <!-- 扫描sql配置文件：mapper需要的xml文件-->
        <property name="mapperLocations" value="classpath:mapper/*.xml"/>
    </bean>

    <!-- 4.配置扫描Dao接口包，动态实现Dao接口，注入到spring容器中-->
    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <!--注入sqlsession的过程 -->
            <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
        <!-- 给出需要扫描Dao接口包 -->
            <property name="basePackage" value="org.seckill.dao"/>

    </bean>

</beans>
```

### DAO层单元测试编码和问题排查

博主用的IDEA是2018.3的版本，使用junit4单元测试不出现版本，这应该是个bug？解决方法：将DAO从interface改为class再改回去就好了。

### 4.秒杀业务service层

#### 4.1秒杀Service接口设计

开始Service层的编码之前，我们首先需要进行Dao层编码之后的思考:在Dao层我们只完成了针对表的相关操作包括写了接口方法和映射文件中的sql语句，并没有编写逻辑的代码，例如对多个Dao层方法的拼接，当我们用户成功秒杀商品时我们需要进行商品的减库存操作(调用SeckillDao接口)和增加用户明细(调用SuccessKilledDao接口)，这些逻辑我们都需要在Service层完成。这也是一些初学者容易出现的错误，他们喜欢在Dao层进行逻辑的编写，其实Dao就是数据访问的缩写，它只进行数据的访问操作，接下来我们便进行Service层代码的编写。

创建一个service包用于存放我们的Service接口和其实现类，创建一个exception包用于存放service层出现的异常例如重复秒杀商品异常、秒杀已关闭等异常，一个dto包作为传输层,dto和entity的区别在于:entity用于业务数据的封装，而dto用于完成web和service层的数据传递。

首先创建我们Service接口，里面的方法应该是按”使用者”(程序员)的角度去设计，SeckillService.java，代码如下:

```java
package org.seckill.service;

import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;

import java.util.List;

/**
 * 业务接口:站在"使用者"角度设计接口
 * 三个方面：方法定义粒度，参数，返回类型(return )
 */
public interface SeckillService {

    /**
     * 查询所有秒杀记录
     * @return
     */
    List<Seckill> getSeckillList();


    /**
     * 查询单个秒杀记录
     * @param seckillId
     * @return
     */
    Seckill getById(long seckillId);

    /**
     * 秒杀开启时输出秒杀接口地址
     * 否则输出系统时间和秒杀时间
     * @param seckillId
     */
    Exposer exportSeckillUrl(long seckillId);


    /**
     * 执行秒杀操作，有可能失败，有可能成功，所以要抛出我们的异常
     * @param seckillId
     * @param userPhone
     * @param md5
     */
    SeckillExecution executeSeckill  (long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException;

}

```

该接口中前面两个方法返回的都是跟我们业务相关的对象，而后两个方法返回的对象与业务不相关，这两个对象我们用于封装service和web层传递的数据，方法的作用我们已在注释中给出。相应在的dto包中创建Exposer.java，用于封装秒杀的地址信息，各个属性的作用在代码中已给出注释，代码如下:

```java
package org.seckill.dto;

/**
 * 暴露秒杀地址DTO
 */
public class Exposer {

    //是否开启秒杀
    private boolean exposed;

    //对秒杀地址加密措施
    private  String md5;

    //id
    private long seckillId;
    //系统当前时间(毫秒)
    private long now;

    //开启时间
    private long start;

    //结束时间
    private long end;


    public Exposer(boolean exposed, String md5, long seckillId) {
        this.exposed = exposed;
        this.md5 = md5;
        this.seckillId = seckillId;
    }

    public Exposer(boolean exposed,long seckillId, long now, long start, long end) {
        this.exposed = exposed;
        this.seckillId=seckillId;
        this.now = now;
        this.start = start;
        this.end = end;
    }

    public Exposer(boolean exposed, long seckillId) {
        this.exposed = exposed;
        this.seckillId = seckillId;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public long getNow() {
        return now;
    }

    public void setNow(long now) {
        this.now = now;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}

```

和SeckillExecution.java，用于判断秒杀是否成功，成功就返回秒杀成功的所有信息(包括秒杀的商品id、秒杀成功状态、成功信息、用户明细)，失败就抛出一个我们允许的异常(重复秒杀异常、秒杀结束异常),代码如下:

```java
package org.seckill.dto;

import org.seckill.entity.SuccessKilled;
import org.seckill.enums.SeckillStatEnum;

/**
 * 封装秒杀执行后的结果
 */
public class SeckillExecution {

    private long seckillId;

    //秒杀执行状态
    private  int state;

    //状态标识
    private String stateInfo;

    //当秒杀成功后，传递秒杀成功对象回去
    private SuccessKilled successKilled;

    //秒杀成功返回所有信息
    public SeckillExecution(long seckillId, SeckillStatEnum statEnum, SuccessKilled successKilled) {
        this.seckillId = seckillId;
        this.state = statEnum.getState();
        this.stateInfo = statEnum.getStateInfo();
        this.successKilled = successKilled;
    }

    //秒杀失败
    public SeckillExecution(long seckillId, SeckillStatEnum statEnum) {
        this.seckillId = seckillId;
        this.state = statEnum.getState();
        this.stateInfo = statEnum.getStateInfo();
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public void setStateInfo(String stateInfo) {
        this.stateInfo = stateInfo;
    }

    public SuccessKilled getSuccessKilled() {
        return successKilled;
    }

    public void setSuccessKilled(SuccessKilled successKilled) {
        this.successKilled = successKilled;
    }
}

```

然后需要创建我们在秒杀业务过程中允许的异常，重复秒杀异常RepeatKillException.java:

```java
package org.seckill.exception;

/**
 * 重复秒杀异常(运行期异常)，不需要我们手动try catch
 * mysql只支持运行期异常的回滚操作
 */
public class RepeatKillException extends  SeckillException{

    public RepeatKillException(String message) {
        super(message);
    }

    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}

```

秒杀关闭异常SeckillCloseException.java:

```java
package org.seckill.exception;

/**
 * 秒杀关闭异常，当秒杀结束时用户还要进行秒杀就会出现这个异常
 */
public class SeckillCloseException extends  SeckillException{
    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}

```

和一个异常包含与秒杀业务所有出现的异常SeckillException.java:

```java
package org.seckill.exception;

/**
 * 秒杀相关业务异常
 */
public class SeckillException extends RuntimeException{
    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}

```

#### 4.2秒杀Service接口的实现

在service包下创建impl包存放它的实现类，SeckillServiceImpl.java，内容如下:

```java
/**
 *
 */
public class SeckillServiceImpl  implements SeckillService {


    private Logger logger = LoggerFactory.getLogger(this.getClass());

     private SeckillDAO seckillDAO;

     private SuccessKilledDAO successKilledDAO;
     //md5盐值字符串
     private final String salt="213hj12398duui`$#!#*$&$%#%#";
    @Override
    public List<Seckill> getSeckillList() {
        return seckillDAO.queryAll(0,4);
    }

    @Override
    public Seckill getById(long seckillId) {
        return seckillDAO.queryById(seckillId);
    }

    @Override
    public Exposer exportSeckillUrl(long seckillId) {
        Seckill seckill = seckillDAO.queryById(seckillId);
        if(seckill==null){
            return new Exposer(false,seckillId);
        }
        Date startTime= seckill.getStartTime();
        Date endTime=seckill.getEndTime();
        Date nowTime= new Date();
        if(nowTime.getTime()<startTime.getTime()||nowTime.getTime()>endTime.getTime()){
            return new Exposer(false,seckillId,nowTime.getTime(),startTime.getTime(),endTime.getTime());
        }
        //转化特定字符串的过程,不可逆
        String md5=getMD5(seckillId);
        return new Exposer(true,md5,seckillId);

    }


    private String getMD5(long seckillId){
        String base=seckillId+"/"+salt;
        String md5= DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    /**
     * 秒杀是否成功，成功：减库存；失败：抛出异常，事务回滚
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    @Override
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5==null||!md5.equals(getMD5(seckillId))){
            throw new SeckillException("seckill data rewrite");
        }
        //执行秒杀逻辑
        Date nowTime = new Date();

        try {
            int updateCount = seckillDAO.reduceNumber(seckillId, nowTime);
            if (updateCount <= 0) {
                //没有更新到记录，秒杀结束
                throw new SeckillCloseException("seckill is closed");
            } else {
                //减库存成功，记录购买行为
                int insertCount = successKilledDAO.insertSuccessKilled(seckillId, userPhone);
                //唯一:seckillId,userPhone
                if (insertCount <= 0) {
                    //重复秒杀
                    throw new RepeatKillException("seckill repeated ");

                } else {
                    //秒杀成功
                    SuccessKilled successKilled = successKilledDAO.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
                }
            }
        }catch (SeckillCloseException e1){
            throw e1;
        }catch (RepeatKillException e2){
            throw e2;
        }
        catch (Exception e){
            logger.error(e.getMessage(),e);
            //所以编译器异常转化为运行期异常
            throw  new SeckillException("seckill inner error:"+e.getMessage());
        }

    }
}

```

对上述代码进行分析一下，在`return new SeckillExecution(seckillId,1,"秒杀成功",successKilled);`代码中，我们返回的state和stateInfo参数信息应该是输出给前端的，但是我们不想在我们的return代码中硬编码这两个参数，所以我们应该考虑用枚举的方式将这些常量封装起来，在cn.codingxiaxw包下新建一个枚举包enums，创建一个枚举类型SeckillStatEnum.java，内容如下:

```java
package org.seckill.enums;

/**
 * 使用枚举表述常量数据字段
 *
 */
public enum SeckillStatEnum {
    SUCCESS(1,"秒杀成功"),
    END(0,"秒杀结果"),
    REPEAT_KILL(-1,"重复秒杀"),
    INNER_ERROR(-2,"系统异常"),
    DATA_REWRITE(-3,"数据篡改"),
    ;

    private int state;

    private String stateInfo;

    SeckillStatEnum(int state, String stateInfo) {
        this.state = state;
        this.stateInfo = stateInfo;
    }

    public int getState() {
        return state;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public static SeckillStatEnum stateof(int index){
        for(SeckillStatEnum state:values()){
            if(state.getState()==index){
                return state;
            }
        }
        return null;
    }

}

```

然后修改执行秒杀操作的非业务类SeckillExecution.java里面涉及到state和stateInfo参数的构造方法:

```java
//秒杀成功返回所有信息
 public SeckillExecution(long seckillId, SeckillStatEnum statEnum, SuccessKilled successKilled) {
     this.seckillId = seckillId;
     this.state = statEnum.getState();
     this.stateInfo = statEnum.getInfo();
     this.successKilled = successKilled;
 }

 //秒杀失败
 public SeckillExecution(long seckillId, SeckillStatEnum statEnum) {
     this.seckillId = seckillId;
     this.state = statEnum.getState();
     this.stateInfo = statEnum.getInfo();
 }
```

然后便可修改实现类方法中的返回语句为:`return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS,successKilled);`，保证了一些常用常量数据被封装在枚举类型里。

目前为止我们Service的实现全部完成，接下来要将Service交给Spring的容器托管，进行一些配置。

### 4.3使用spring托管service依赖配置

