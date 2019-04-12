# Seckill
--------------
电商秒杀小网站练习
购买者抢购商品一般限制为么人一件，为了实现这个需求，在数据库的商品表中采用了手机号和商品ID的联合主键，这样每个用户就只能购买该一次，重复购买时，数据库添加新数据时会查询到重复的一行数据，添加失败导致异常之后数据库回滚数据。
```java
@Transactional
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
```
