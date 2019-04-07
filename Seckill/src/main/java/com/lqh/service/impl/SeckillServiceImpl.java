package com.lqh.service.impl;

import com.lqh.enums.SeckillStateEnum;
import com.lqh.dto.Exposer;
import com.lqh.dto.SeckillExecution;
import com.lqh.entity.Seckill;
import com.lqh.entity.SeckillOrder;
import com.lqh.exception.RepeatKillException;
import com.lqh.exception.SeckillCloseException;
import com.lqh.exception.SeckillException;
import com.lqh.mapper.SeckillMapper;
import com.lqh.mapper.SeckillOrderMapper;
import com.lqh.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class SeckillServiceImpl implements SeckillService {
    //日志输出当前类的信息
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    //设置盐值字符串
    private final String salt = "u2923hf824sef742";
    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    RedisTemplate redisTemplate;
    //设置redis缓存的key
    private final String key = "seckill";

    @Override
    public List<Seckill> findAll() {
        List<Seckill> seckillList = redisTemplate.boundHashOps("seckill").values();
        if(seckillList==null||seckillList.size()==0){
            //说明缓存中没有秒杀列表数据
            //查询数据库中秒杀列表数据，并将列表数据循环放入redis缓存
            seckillList = seckillMapper.findAll();
            for (Seckill seckill:seckillList){
                //将秒杀列表数据依次放入redis缓存中，key:秒杀表的ID值；value:秒杀商品数据
                redisTemplate.boundHashOps(key).put(seckill.getSeckillId(),seckill);
                logger.info("findAll -> 从数据库中读取放入缓存中");
            }
        }else{
            logger.info("findAll -> 从缓存中读取");
        }
        return seckillList;
    }

    @Override
    public Seckill findById(long seckillId) {
        return seckillMapper.findById(seckillId);
    }

    @Override  //对外暴露秒杀接口
    public Exposer exportSeckillUrl(long seckillId) {
        //在redis里查找商品对象
        Seckill seckill = (Seckill) redisTemplate.boundHashOps(key).get(seckillId);
        if(seckill==null){
            //说明redis缓存中没有此key对应的value
            //查询数据库，并将数据放入缓存中
            seckill = seckillMapper.findById(seckillId);
            if(seckill==null){
                //数据库没有查询到数据
                return new Exposer(false,seckillId);
            }else {
                //查询到了，存入redis缓存中。 key:秒杀表的ID值； value:秒杀表数据
                redisTemplate.boundHashOps(key).put(seckill.getSeckillId(), seckill);
                logger.info("RedisTemplate -> 从数据库中读取并放入缓存中");
                }
            }else{  //redis中的value不为空
            logger.info("RedisTemplate -> 从缓存中读取");
            }
            Date startTime = seckill.getStartTime();
            Date endTime = seckill.getEndTime();
            //获取当前时间
            Date nowTime = new Date();
            if(nowTime.getTime()<startTime.getTime()||nowTime.getTime()>endTime.getTime()){
                return new Exposer(false,seckillId,nowTime.getTime(),startTime.getTime(),endTime.getTime());
            }//转换特定字符串的过程，不可逆的算法
                String md5 = getMD5(seckillId);
                return new Exposer(true,md5,seckillId);
    }

    @Override
    @Transactional  //加入事务以进行回滚
    public SeckillExecution executeSeckill(long seckillId, BigDecimal money, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5==null||!md5.equals(getMD5(seckillId))){
            throw new SeckillException("seckill data rewrite");
        }
        //执行秒杀操作,减少商品库存，新增订单
        Date curTime = new Date();
        try{
            int insertResult = seckillOrderMapper.insertOrder(seckillId, money, userPhone); //保存订单信息
            //seckillId,userPhone联合主键保证唯一性
            if(insertResult<=0){
                //订单重复
                throw new RepeatKillException("seckill repeated");
            }else{
                //从库存减少商品
                int updateResult = seckillMapper.reduceStock(seckillId, curTime);
                if(updateResult<=0){
                    //商品售罄
                    throw new SeckillCloseException("stock is empty");
                }else{
                    //秒杀成功
                    SeckillOrder seckillOrder = seckillOrderMapper.findById(seckillId,userPhone);
                    //更新缓存
                    Seckill seckill = (Seckill) redisTemplate.boundHashOps(key).get(seckillId);
                    seckill.setStockCount(seckill.getStockCount()-1);  //更新库存
                    redisTemplate.boundHashOps(key).put(seckillId,seckill);

                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS,seckillOrder);
                }
            }
        }catch (SeckillCloseException e){
            throw e;
        }catch (RepeatKillException e){
            throw e;
        }catch (Exception e){
            logger.error(e.getMessage(),e);
            //所有编译期异常，转换为运行期异常
            throw new SeckillException("seckill inner error:"+e.getMessage());
        }
    }

    private String getMD5(Long seckillId){
        String num = seckillId+"/"+salt;
        String md5 = DigestUtils.md5DigestAsHex(num.getBytes());
        return md5;
    }
}
