package com.whh.redisredisson;

import com.whh.redisredisson.config.RedissonConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.map.event.EntryUpdatedListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
class RedisRedissonApplicationTests {

	@Autowired
	private RedissonClient redissonClient;

	@Test
	void bucketTest() {

		RBucket<String> obj = redissonClient.getBucket("asdfasd");
		obj.set("asdasdf");

		String value  = obj.get();
		log.info("value: {}", value);
	}

	@Test
	void bucketTest2(){
		/**
		 * 事物性， hash不在同一节点会出现问题， {}可解决，只让{}内的进行hash
		 */
		RBucket<String> obj = redissonClient.getBucket("key4");
		obj.set("test1");

		Map<String, String> map = new HashMap<>();
		map.put("{key}3", "test0");
		map.put("{key}4", "test11");
		map.put("{key}5", "test2");

		RBuckets rBuckets = redissonClient.getBuckets();
		boolean ret = rBuckets.trySet(map);

		log.info("trySet: {}", ret);

		RBucket<String> obj2 = redissonClient.getBucket("key5");
		String key5 = obj2.get();
		log.info("key5: {}", key5);


	}

	@Test
    void geospatialBucket(){
        RGeo<String> geo =  redissonClient.getGeo("mygeo");
        geo.add(43, 45f, "深圳");

        String name = geo.getName();
        log.info("name: {}", name);

        Map<String, GeoPosition> map =  geo.pos("深圳");

        log.info("name: {}", map.size());
    }

    @Test
    void topic() throws InterruptedException {
        RTopic topic = redissonClient.getTopic("myTopic");

        topic.addListener(TestEvent.class, (charSequence, testEvent) -> log.info("onMessage"));

        long message = topic.publish(new TestEvent());
        log.info("message: {}", message);
        Thread.sleep(2000);


    }

    @Test
    void localCacheTest(){
        LocalCachedMapOptions options = LocalCachedMapOptions.defaults()
                // 用于淘汰清除本地缓存内的元素
                // 共有以下几种选择:
                // LFU - 统计元素的使用频率，淘汰用得最少（最不常用）的。
                // LRU - 按元素使用时间排序比较，淘汰最早（最久远）的。
                // SOFT - 元素用Java的WeakReference来保存，缓存元素通过GC过程清除。
                // WEAK - 元素用Java的SoftReference来保存, 缓存元素通过GC过程清除。
                // NONE - 永不淘汰清除缓存元素。
                .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.NONE)
                // 如果缓存容量值为0表示不限制本地缓存容量大小
                .cacheSize(1000)
                // 以下选项适用于断线原因造成了未收到本地缓存更新消息的情况。
                // 断线重连的策略有以下几种：
                // CLEAR - 如果断线一段时间以后则在重新建立连接以后清空本地缓存
                // LOAD - 在服务端保存一份10分钟的作废日志
                //        如果10分钟内重新建立连接，则按照作废日志内的记录清空本地缓存的元素
                //        如果断线时间超过了这个时间，则将清空本地缓存中所有的内容
                // NONE - 默认值。断线重连时不做处理。
                .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.NONE)
                // 以下选项适用于不同本地缓存之间相互保持同步的情况
                // 缓存同步策略有以下几种：
                // INVALIDATE - 默认值。当本地缓存映射的某条元素发生变动时，同时驱逐所有相同本地缓存映射内的该元素
                // UPDATE - 当本地缓存映射的某条元素发生变动时，同时更新所有相同本地缓存映射内的该元素
                // NONE - 不做任何同步处理
                .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
                // 每个Map本地缓存里元素的有效时间，默认毫秒为单位
                .timeToLive(10000)
                // 或者
                .timeToLive(10, TimeUnit.SECONDS)
                // 每个Map本地缓存里元素的最长闲置时间，默认毫秒为单位
                .maxIdle(10000)
                // 或者
                .maxIdle(10, TimeUnit.SECONDS);

        RLocalCachedMap<String, Integer> map = redissonClient.getLocalCachedMap("344334", options);

        map.put("321", 1);
        map.putIfAbsent("321", 2);

        Integer value = map.get("321");
        log.info("value: {}", value);

        map.putAsync("321", 56);
        //map.fastRemoveAsync("321");

        value = map.get("321");
        log.info("value: {}", value);
        map.destroy();
    }

    @Test
    void lockTest(){
	    RLock rLock = redissonClient.getLock("testLock");
	    rLock.lock(100, TimeUnit.SECONDS);

        try{
            //TODO 原子操作代码块
            log.info("this is source");
        }finally {
            rLock.unlock();
        }
    }
}
