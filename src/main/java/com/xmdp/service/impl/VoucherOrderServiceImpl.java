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
import com.xmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

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
