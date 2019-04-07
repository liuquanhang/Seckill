package com.lqh.mapper;

import com.lqh.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface SeckillOrderMapper {
    //插入购买订单明细
    int insertOrder(@Param("seckillId")long seckillId, @Param("money")BigDecimal money,@Param("userPhone")long userPhone);
    //根据秒杀商品ID查询订单明细数据并得到对应秒杀商品的数据，因为我们在SeckillOrder中已经定义了一个Seckill的属性
    SeckillOrder findById(@Param("seckillId") long seckillId, @Param("userPhone") long userPhone);

}
