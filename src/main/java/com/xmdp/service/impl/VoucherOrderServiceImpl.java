package com.xmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.xmdp.common.utils.RedisIdWorker;
import com.xmdp.dto.Result;
import com.xmdp.entity.SeckillVoucher;
import com.xmdp.entity.VoucherOrder;
import com.xmdp.mapper.VoucherOrderMapper;
import com.xmdp.service.ISeckillVoucherService;
import com.xmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmdp.utils.SimpleRedisLock;
import com.xmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    ISeckillVoucherService seckillVoucherService;

    @Resource
    RedisIdWorker redisIdWorker;

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher==null){
            return Result.fail("不存在该优惠券");
        }
        //注意LocalDateTime类型为java8优化时间使用推出的时间类型，分时区，国际时区是UTC
        //共有LocalDate（2021-9-1），LocalTime（08：07），LocalDateTime（2021-09-01 11：07）三种，分别对应数据库date，time，dateTime三种
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now()) || seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("该类型优惠券暂未开放");
        }
        if (seckillVoucher.getStock()<1){
            return Result.fail("优惠券库存不足！");
        }
        Long id = UserHolder.getUser().getId();
        //一人一单 保证锁的粒度最细toString锁不住，底层代码每次都是new的，所以使用intern在常量池中取
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order"+id,stringRedisTemplate);
        boolean b = simpleRedisLock.tryLock(5);
        if (!b){
            return Result.fail("获取优惠券失败请稍后再试！");
        }
        //获取到锁了事务开始处理，最后不管是否成功再释放锁，保证锁于事务的同步提交，这里代码需要注意Transactional的位置。
        //保证事务的生效使用aop代理对象调用接口方法。
        IVoucherOrderService voucherOrderService = (IVoucherOrderService)AopContext.currentProxy();
        return voucherOrderService.createVoucherOrder(voucherId);
    }
    @Transactional
    public Result createVoucherOrder(Long voucherId){
        Long id = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", id).eq("voucher_id", voucherId).count();
        //查询用户是否已经购买过
        if (count>0){
            return Result.fail("优惠券只可以购买一次！");
        }
        //为了防止商品超卖 少卖等问题使用悲观锁或者乐观锁处理
        //悲观锁就是比较悲观，直接用syn和lock锁处理，但是高并发性能很差
        //乐观锁就是cas机制，加一个版本号，每次提交数据查看版本号是否变动，变动则更新失败，但高并发下会发现
        //大部分版本号都会变动，导致大部分优惠券都抢不到，这里建议使用数据库行锁处理，如下。
        // 下列表示  update 某个表 set某个属性等于多少，where条件等于多少
        boolean update = seckillVoucherService.update().setSql("stock = stock-1").gt("stock", 0).eq("voucher_id", voucherId).update();
        if (!update){
            return Result.fail("优惠券库存不足！");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        //使用全局id生成器处理id
        long orderId = redisIdWorker.workId("order");
        voucherOrder.setId(orderId);
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
