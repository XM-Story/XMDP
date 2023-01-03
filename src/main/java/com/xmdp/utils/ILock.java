package com.xmdp.utils;

public interface ILock {

    /**
     * 分布式锁尝试获取锁  set nx ex
     * @param timeout
     * @return
     */
    boolean tryLock(long timeout);

    /**
     * 锁删除
     */
    void unlock();
}
