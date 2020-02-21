## SSM+Maven+IDEA实现高并发秒杀商品系统

### 时间：2020.02.06

### 功能

- 秒杀接口暴露
- 执行秒杀
- 相关查询

> [原项目github链接](https://github.com/codingXiaxw/seckill)

本项目的完成基于原项目，是自主学习的记录。有些原项目中开发知识点由于版本升级的原因（如mysql）已经不再适用，本说明文档会记录自己踩过的坑。

​	![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/seckill.png)

![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/seckill function.jpg)

![seckill-core](https://github.com/shendezhuti/seckill_hzx/blob/master/image/seckill-core.png)

### 1.使用maven在terminal中创建项目

```
mvn archetype:generate -DgroupId=org -DartifactId=seckill -DarchetypeArtifactId=maven-archetype-webapp
```

注意原教程创建mvn在命令中用的create，是过时的写法，会报错，要改为generate

### 2.数据库编码部分

> 作者zhang提到，最好能拥有手写sql语言的能力，记录每次上线的DDL。这样对项目上线后的修改非常有帮助

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

#### 3.1基于mybatis实现DAO

mybatis与hibernate的作用是建立数据库与Entity实体对象的映射

**mybatis特点**

- 参数+SQL=Entity/List

我们只要提供参数以及编写sql语言。由于mybatis封装了jdbc，会把结果返回给我们。

**如何使用mybatis**

- SQL写在哪？XML提供SQL vs 注解提供SQL
- 如何实现DAO接口？Mapper自动实现DAO接口 vs API编程实现DAO接口

一般来说建议使用xml提供sql，使用Mapper自动实现DAO接口，减少代码量。

#### 3.2配置mybatis

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
        <!-- timestamp 类型的列插入如果为null，为自动生成系统当前时间-->
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

#### 3.3mybatis整合spring理论

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

#### 3.4mybatis整合spring编码

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

#### 3.5DAO层单元测试编码和问题排查

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

#### 4.3使用spring托管service依赖配置

![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/spring.png)

在spring下创建spring-service.xml 实现扫描service包下所有使用注解的类型

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context" xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <!-- 扫描service包下所有使用注解的类型-->
    <context:component-scan base-package="org.seckill.service"/>

</beans>
```

然后对SeckillServiceImpl类加上 `@Service`表明这是一个service类，注入spring容器。我们知道SeckillDAO和SuccessKilledDAO是mybatis和spring整合后，Dao都会以mapper的方式初始化好，放到spring容器中。在SeckillServiceImpl要获取这两个类的实例的话使用`@Autowired`，对象会自动注入

#### 4.4使用spring声明式事务理论

![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/srping-transactional.png)

在spring-service.xml文件中添加

```xml
<!-- 配置事务管理器-->

    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <!-- 注入数据库连接池-->
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <!-- 配置基于注解的声明式事务
           默认使用注解来管理事务行为
    -->

    <tx:annotation-driven transaction-manager="transactionManager"/>
```

同时在`executeSeckill()`方法上加上@Transcational注解，利用spring控制事务

 使用注解控制事务方法的优点：

* 1.开发团队达成一致目标，明确标注事务方法的编程风格。
* 2.保证事务方法的执行时间尽可能的短，不要穿插其他网络操作，RPC/Http请求/
* 3.不是所有的方法都需要事务，如只有一条修改操作，只读操作不需要事务控制

#### 4.5集成测试

```java
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
     * 注意这样写可以确保，集成测试的完整性
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
```

### 5.Web层

#### 5.1前端业务流程

![s](/Users/hzx/version-control/seckill_hzx/image/front_end_process.png)



#### 5.2 restful接口

![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/restful.jpg)

#### 5.3使用springmvc理论

![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/spring mvc.jpg)

​	![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/springmvc request function.png)

#### 5.4整合配置SpringMVC框架

在spring包下创建spring-web.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd
        http://www.springframework.org/schema/mvc
        http://www.springframework.org/schema/mvc/spring-mvc.xsd">

    <!--配置spring mvc-->
    <!--1,开启springmvc注解模式
    a.自动注册DefaultAnnotationHandlerMapping,AnnotationMethodHandlerAdapter
    b.默认提供一系列的功能:数据绑定，数字和日期的format@NumberFormat,@DateTimeFormat
    c:xml,json的默认读写支持-->
    <mvc:annotation-driven/>

    <!--2.静态资源默认servlet配置-->
    <!--
        1).加入对静态资源处理：js,gif,png
        2).允许使用 "/" 做整体映射
    -->
    <mvc:default-servlet-handler/>

    <!--3：配置JSP 显示ViewResolver-->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="viewClass" value="org.springframework.web.servlet.view.JstlView"/>
        <property name="prefix" value="/WEB-INF/jsp/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

    <!--4:扫描web相关的bean-->
    <context:component-scan base-package="org.seckill.web"/>
</beans>
```

#### 5.5使用SpringMVC实现Restful接口

首先在seckill下创建SeckillController

```java

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
    @RequestMapping(value="/{seckillId}/exposer",method = RequestMethod.POST)
    public void /*TODO */ exposer(Long seckillId){

    }

```

我们还需要一个dto来封装json结果，在dto包下创建SeckillResult<T>

```java
package org.seckill.dto;


//封装json结果
public class SeckillResult <T>{

    private boolean success;
    private T data;

    private String error;

    public SeckillResult(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public SeckillResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

```

接下来补全刚才的exposer方法和新建execute和now方法

```java
@RequestMapping(value="/{seckillId}/{md5}/execution",
        method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long seckillId,@PathVariable("md5") String md5,

                                                   @CookieValue(value="killPhone",required = false) Long phone){
        if(phone==null){
            return new SeckillResult<SeckillExecution>(false,"未注册");
        }
        SeckillResult<SeckillExecution> result;
        try {
            SeckillExecution seckillExecution = seckillService.executeSeckill(seckillId, phone, md5);
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
    public SeckillResult <Long> time(){

        Date now = new Date();
        return new SeckillResult<Long>(true,now.getTime());
    }
```

detail.jsp和list.jsp由于代码过多，这里暂不列举。

### 六.高并发秒杀优化

![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/concurrency.png)

之前开发的时候，我们将detail页、静态资源放在了CDN上，因此访问这些资源不需要返回系统，这个时候拿不到系统的时间，因此要单独做一个请求获取服务器的时间。

获取系统时间不用优化，因为Java访问一次内存(cacheline)大约10ns

秒杀地址接口分析无法使用CDN缓存，CDN适合请求资源不变化，一个URL对应一个结果。秒杀地址的返回结果可能是在变化的。秒杀地址接口可以放在服务器端缓存：redis等。一秒可以抗10WQPS，集群化之后可以抗百万的QPS(每秒查询率QPS,也即每秒的响应请求数，是对一个特定的查询服务器在规定时间内所处理流量多少的衡量标准)。后端缓存可以用业务系统来控制，访问数据库拿到秒杀数据后，放到redis缓存，下次访问可以直接从缓存中获取。

CDN的特点和相关信息：
使用CDN 获取公共js http://www.bootcdn.cn/
CDN特点：CDN是和请求对应的资源是不变化的，比如静态资源和JavaScript（URL对应的结果不变）
CDN是什么：
   1：CDN是(内容分发网络)加速用户获取数据的系统，例如视频资源
   2：部署在离用户最近的网络节点上 3：命中CDN不需要访问后端服务器
   4：互联网公司自己搭建或租用CDN
使用CDN的好处：
   01 不用去官网直接下载 02 当我们的服务上线一些稳定可靠的CDN比直接发布到我们的服务器更有效
   03 CDN也是web最重要的一个加速的功能点 怎样让系统抗住很高的并发的时候CDN也是一个点



秒杀地址接口本质是拿了一个秒杀对象：对当前时间和秒杀开启时间做一个判断来决定返回数据是什么样的，决定是否要返回秒杀接口。



#### 高并发出现的点：

![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/concurrency-analysis.png)

- 获取系统时间（但是不用优化，访问一次内存(Cacheline)大约10ns）
- 秒杀地址接口分析：
    1.无法使用CDN缓存
    2.但是它适合放在服务器缓存：Redis等->内存的服务器端的缓存可以抗很高的QPS，10^5/sQPS,
     现在Redis可以做集群，做了之后QPS可以达到10^6/s。
    3.为什么要做后端缓存 -> 因为后端缓存可以用我们的业务系统来控制。
     比如先访问数据库拿到秒杀的数据放到Redis缓存里面去，当一次访问的时候直接去缓存里面查找，
     缓存有就直接返回，而不去访问我们数据库了。
    4.一致性维护成本比较低：当我们秒杀的东西或秒杀的对象改变了的时候，
     我们可以修改我们的数据库，同时在改一下我们的缓存，或者干脆不改，等超时之后在改

秒杀地址接口优化：请求地址 -> Redis[一致性维护(超时穿透到SQL语句/当SQL语句更新时主动更新)] -> SQL语句

- 秒杀操作优化分析(是最重要的一个秒杀按钮操作)：
    1.也是不能使用CDN缓存的，CDN不可能把你最核心的东西给缓存(大部分写操作或最核心的数据请求一般没办法使用CDN)
    2.后端缓存困难：库存的问题，不可能在缓存里面减库存，否则会产生数据不一致问题。所以要通过事务来保证数据的一致性
    3.一行数据竞争：热点商品，会对数据库表中的那一行数据产生大量的update减库存竞争

#### Java控制事务行为分析

![](https://github.com/shendezhuti/seckill_hzx/blob/master/image/concurrency-analysis-java.png)

#### 瓶颈分析：
update 减库存：客户端会执行update，根据拿到结果是否更新了，当我们的SQL通过网络发送给数据库时本身就有网络延迟，
       除了网络延迟还有java GC（garbage collection,垃圾回收）操作 -> 不需要手动去回收，
       GC自动就帮我们回收了，新生代GC会暂停所有的事务代码（Java代码）后，执行GC（一般在几十ms），
       并且，同一行事务是做串行化的。

----》insert 购买明细：也会存在网络延迟和GC
----》commit/rollback
也就是说如果是Java客户端去控制这些事务会有什么问题：update 减库存（网络延迟，可能的GC，GC不一定每次都出现，但一定会出现）

--> 执行insert 购买明细（在网络延迟等待insert语句的返回，然后也可能会GC） --> 最后commit/rollback。
当前面的这些操作都执行完之后，第二个等待行锁的线程菜能够有机会拿到这一行的锁在去执行update减库存

特点：

根据上面的拆分，所以QPS很好分析了 --->（我们所有的SQL执行时间 + 网络延迟时间 + 可能的GC）这一行数据就是当前可以执行的时间.比如时间是2ms,概念是1s之内只能有500次减库存的秒杀操作，但是对于秒杀系统，特别是热点系统来说其实是不能满足我们的要求的，特别是排队特别长的时候，性能会呈现指数级别下降

得到的点是：行级锁是在commit/rollback之后释放的；
优化方向：怎样减少行级锁持有的时间 ---> （当你update表中一行数据的时候，一定要快速的commit/rollback，
     因为其他还在等待，因为这是一个热点的数据）；

#### 如何判断Update更新库成功

两个条件：

- update自身没报错
- 客户端确认update影响记录数

#### 延迟分析：
延迟问题是很关键的；
优化思路：

- 把客户端逻辑放到MySQL（数据库）服务端，避免网络延迟和GC影响
- 如何放到MySQL服务端：

#### 如何放到MySQL服务端

--两种解决方案：

- 定制SQL方案： 早期的阿里巴巴的天猫做了一个MySQL的源码层的修改 --->update/*+[auto_commit]*/，
    但是执行完这句update之后，会自动进行回滚（条件是：当update影响的记录数是1，它就会commit，如果等于0就会rollback）也就是说它不给Java客户端和MySQL之间网络延迟，然后在由Java客户端去控制commit还是rollback，而是直接用这条语句直接发过去，告诉它是commit还是rollback。本质上也是降低了网络延迟和GC的干扰，但是成本很高 --> 需要修改MySQL源码，大公司可以这样的团队

- 使用存储过程： 整个事务在MySQL端完成；存储过程设计出来的本质就是想让我们的一组SQL组成一个事务，然后在服务器端完成，而避免客户端去完成事务造成的一个性能的干扰。一般情况下像是spring声明式事务或我们手动控制事务都是客户端控制事务，这个事务在行级锁没有那么高的竞争情况下是完全OK的，但秒杀是一个特殊的应用场景，它会在同一行中产生热点，大家都竞争同一行，那么这个时候存储过程就发挥作用了，它把整个这条SQL执行过程完全放在MySQL中完成了， MySQL执行的效率非常高，因为我们都是通过主键去执行的，查询或更新

#### 优化总结

- 前端控制：暴露接口，按钮防重复
- 动静态数据分离：CDN缓存，后端缓存
- 事务竞争优化：减少事务锁时间 ---> 这是秒杀用MySQL解决秒杀问题的很重要的一个关键点；
    因为用事务有一个很大的优点是：保证原子性、隔离性、一致性、持久性。



#### redis 后端缓存优化编码

引入redis在哪优化呢？找到我们的`SeckillServiceImpl`，在暴露秒杀地址的方法`exportSeckillUrl`。在这里我们通过主键查询数据库seckill记录，可以通过redis缓存，降低数据库的访问量。注意在这里老师说，很多工程师会犯错，直接在这里写入业务的代码。我们由于有dao层，所以应该把访问redis的代码写在dao层。

我们在dao包下新建cache包，创建RedisDAO方法

```java
public class RedisDAO {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private JedisPool jedisPool;
    private RuntimeSchema<Seckill> schema=RuntimeSchema.createFrom(Seckill.class);

    public RedisDAO(String ip,int port){
         jedisPool = new JedisPool(ip,port);
    }

    public Seckill getSeckill(long seckillId){
        //redis操作

        try {
            Jedis jedis = jedisPool.getResource();
            try {
               String key = "seckill:"+seckillId;
               //并没有实现内部序列化操作
                //get->byte[]->反序列化->Object(Seckill)
                byte [] bytes=jedis.get(key.getBytes());
                if(bytes!=null){
                    Seckill seckill = schema.newMessage();
                    ProtostuffIOUtil.mergeFrom(bytes,seckill,schema);
                    //seckill 被反序列化
                    return seckill;
                }
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }

        return null;
    }

    public String putSeckill(Seckill seckill){
        // set Object (seckill) -> 序列化->byte[]
        try {
            Jedis jedis= jedisPool.getResource();
            try{
                String key = "seckill:"+seckill.getSeckillId();
                byte[]bytes=ProtostuffIOUtil.toByteArray(seckill,schema, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                int timeout=60*60;
              //超时缓存
                String result = jedis.setex(key.getBytes(),timeout,bytes);
                return result;
            }finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return null;
    }
}

```

#### 并发优化1

![image-20200221121315935](https://github.com/shendezhuti/seckill_hzx/blob/master/image/original-transaction.png)





![image-20200221132421337](https://github.com/shendezhuti/seckill_hzx/blob/master/image/simple-improvement.png)

#### 并发优化2-存储过程

我们将之前在客户端写的 购买明细逻辑+减库存逻辑 全部放在 MySQL端，在MySQL端编写存储过程，进行优化，这样可以避免了之前分析过的网络延迟这个性能杀手。

```sql
-- 秒杀执行存储过程

DElIMITER $$ -- console;转换为$$

-- 定义存储过程
-- 参数：in输入参数; out 输出参数
-- count count():返回上一条修改类型sql(delete,insert,update)的影响行数
-- row_count: 0 影响未修改数据 >0 修改的行数  <0 sql错误/未修改执行sql
CREATE PROCEDURE `seckill`.`execute_seckill`
    (in v_seckill_id bigint, in v_phone bigint,
     in v_kill_time timestamp, out r_result int)
    BEGIN
        DECLARE insert_count int DEFAULT 0;
        START TRANSACTION;
        insert ignore into success_killed
            (seckill_id, user_phone, create_time,state)
            values (v_seckill_id, v_phone, v_kill_time,0);
        select row_count() into insert_count;
        IF (insert_count = 0) THEN
            ROLLBACK;
            set r_result = -1;
        ELSEIF(insert_count < 0) THEN
            ROLLBACK;
            set r_result = -2;
        ELSE
            update seckill
            set number = number - 1
            where seckill_id = v_seckill_id
                and end_time > v_kill_time
                and start_time < v_kill_time
                and number > 0;
            select row_count() into insert_count;
            IF (insert_count = 0) THEN
                ROLLBACK;
                set r_result = 0;
            ELSEIF (insert_count < 0) THEN
                ROLLBACK;
                set r_result = -2;
            ELSE
                COMMIT;
                set r_result = 1;
            END IF;
        END IF;
    END;
$$
-- 存储过程定义结束

DELIMITER ;

set @r_result =-3;
call execute_seckill(1000,18684767681,now(),@r_result);
select @r_result;

-- 存储过程
-- 1：存储过程优化：事务行级锁持有的时间，
--2：不要过度依赖存储过程
--3.简单的逻辑，可以应用存储过程
--4:QPS：一个秒杀单6000/qps
```

在SeckillDAO下新建 void killByProcedure(Map<String,Obejct> paramMap)；接口 

```java
 List<Seckill> queryAll(@Param("offset") int offset,@Param("limit") int limit);

    /**
     *
     * @param paraMap
     */
    void killByProcedure(Map<String ,Object> paraMap);
```

在SeckillDao.xml下新增mybatis调用存储过程

```xml
 <!--mybatis调用存储过程-->

    <select id="killByProcedure" statementType="CALLABLE">
        call execute_seckill(
          #{seckillId,jdbcType=BIGINT,mode=IN},
          #{phone,jdbcType=BIGINT,mode=IN},
          #{killTime,jdbcType=TIMESTAMP,mode=IN},
          #{result,jdbcType=INTEGER,mode=OUT},
        )
    </select>
```

在SeckillServiceImpl类中新增实现方法

```java
   @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5)  {
        if(md5==null||!md5.equals(md5)){
            return new SeckillExecution( seckillId,SeckillStateEnum.DATA_REWRITE);

        }
        Date killTime= new Date();
        Map<String,Object> map = new HashMap<>();
        map.put("seckillId",seckillId);
        map.put("phone",userPhone);
        map.put("killTime",killTime);
        map.put("result",null);

        try {
            seckillDAO.killByProcedure(map);
            int result = MapUtils.getInteger(map,"result",-2);
            if(result == 1){
                SuccessKilled sk = successKilledDAO.queryByIdWithSeckill(seckillId,userPhone);
                return new SeckillExecution(seckillId,SeckillStateEnum.Success,sk);
            }else{
                return new SeckillExecution(seckillId,SeckillStateEnum.Success.stateof(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new SeckillExecution(seckillId,SeckillStateEnum.Success.INNER_ERROR);

        }
    }
```

在SeckillController类中实现存储过程的调用

```java
            SeckillExecution seckillExecution = seckillService.executeSeckillProcedure(seckillId, userPhone, md5);

```

